package com.smartmeat.service;


import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.smartmeat.dto.request.CreateOrderRequest;
import com.smartmeat.dto.request.VerifyPaymentRequest;
import com.smartmeat.dto.response.OrderResponse;
import com.smartmeat.dto.response.ProductResponse;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RazorpayClient razorpayClient;
    
    private final OrderService orderService;

    private final ProductService productService;

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    // ── CREATE ORDER ──────────────────────────────────────────────
    public Map<String, Object> createOrder(CreateOrderRequest request) {

        // 1. Calculate total from DB — never trust frontend amount
        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderRequest.OrderItemDto item : request.getItems()) {
            ProductResponse product = productService.getById(item.getProductId());
            if (!product.isAvailable()) {
                throw new RuntimeException(product.getName() + " is not available");
            }
            total = total.add(product.getPricePerKg().multiply(item.getQty()));
        }

        System.out.println("Backend calculated total: " + total);
        System.out.println("UPI paid: "   + request.getUpiPaid());
        System.out.println("Cash paid: "  + request.getCashPaid());
        System.out.println("Payment mode: " + request.getPaymentMode());
        System.out.println("Role: "       + request.getRole());
        System.out.println("Is Khata: "   + request.isKhata());

        int amountInPaise;

        if (request.isKhata()) {
            // ── KHATA MODE (UPI or SPLIT) ────────────────────────────────────
            // Formula: cashPaid + upiPaid + khataRemainder = total
            // cashPaid + upiPaid can be less than total — remainder goes to Khata.
            // Only validate that what they're paying now does not exceed the bill.
            BigDecimal payingNow = request.getCashPaid().add(request.getUpiPaid());

            if (payingNow.compareTo(total) > 0) {
                throw new RuntimeException(
                    "Amount paying now (₹" + payingNow + ") exceeds bill total (₹" + total + ")");
            }

            // Razorpay charges only the UPI portion
            amountInPaise = request.getUpiPaid()
                .setScale(0, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .intValue();

            System.out.println("Khata mode — Razorpay amount (UPI portion): " + amountInPaise);

        } else if ((request.getRole().equalsIgnoreCase("ADMIN")
                 || request.getRole().equalsIgnoreCase("SELLER"))
                 && request.getPaymentMode().equalsIgnoreCase("SPLIT")) {
            // ── ADMIN/SELLER SPLIT (no Khata) ───────────────────────────────
            // cash + upi must equal total exactly — no remainder allowed
            BigDecimal splitSum = request.getCashPaid().add(request.getUpiPaid());

            if (total.compareTo(splitSum) != 0) {
                throw new RuntimeException(
                    "Payment mismatch — cash (₹" + request.getCashPaid() +
                    ") + UPI (₹" + request.getUpiPaid() +
                    ") = ₹" + splitSum +
                    " but bill total is ₹" + total);
            }

            // Razorpay charges only the UPI portion
            amountInPaise = request.getUpiPaid()
                .setScale(0, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .intValue();

            System.out.println("Admin split mode — Razorpay amount (UPI portion): " + amountInPaise);

        } else {
            // ── PURE UPI / CARD (Customer or Admin, no Khata, no Split) ─────
            // Razorpay charges the full bill total
            amountInPaise = total
                .setScale(0, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .intValue();

            System.out.println("UPI/Card mode — Razorpay amount (full total): " + amountInPaise);
        }

        // 2. Create Razorpay order
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount",          amountInPaise);
            orderRequest.put("currency",        "INR");
            orderRequest.put("receipt",         "rcpt_" + System.currentTimeMillis());
            orderRequest.put("payment_capture", 1);

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            System.out.println("Razorpay order created: " + razorpayOrder.get("id") +
                               " for amount (paise): " + amountInPaise);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId",  razorpayOrder.get("id"));
            response.put("amount",   amountInPaise);
            response.put("currency", "INR");
            response.put("keyId",    keyId);
            return response;

        } catch (RazorpayException e) {
            throw new RuntimeException("Failed to create Razorpay order: " + e.getMessage());
        }
    }

    // ── VERIFY PAYMENT ────────────────────────────────────────────
    public OrderResponse verifyAndConfirm(VerifyPaymentRequest request) {

        // 1. Verify signature — proves payment is genuine
        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        boolean isValid = verifySignature(payload, request.getRazorpaySignature());

        if (!isValid) {
            throw new RuntimeException("Payment verification failed — invalid signature");
        }

        // 2. Re-calculate amount from DB again for safety
        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderRequest.OrderItemDto item : request.getItems()) {
            ProductResponse product = productService.getById(item.getProductId());
            BigDecimal itemTotal = product.getPricePerKg()
                .multiply(item.getQty());
            total = total.add(itemTotal);
        }
        

        
        // 3. Save order to DB    
        return orderService.placeOrder(request);
    }

    // ── HMAC SHA256 Signature Verification ───────────────────────
    private boolean verifySignature(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(keySecret.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Object> verifyAndConfirmByAdmin(VerifyPaymentRequest request) {
        // 1. Verify signature only
        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        boolean isValid = verifySignature(payload, request.getRazorpaySignature());

        if (!isValid) {
            throw new RuntimeException("payment verification by admin...!");
        }

        // 2. Return verified status — DO NOT call orderService.placeOrder()
        //    pos-sale endpoint is the single place that saves the order.
        Map<String, Object> response = new HashMap<>();
        response.put("verified", true);
        response.put("razorpayPaymentId", request.getRazorpayPaymentId());
        return response;
    }
}