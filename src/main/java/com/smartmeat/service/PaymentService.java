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
    	
    	   // DEBUG — remove after fixing
        log.info("Razorpay Key ID: {}", keyId);
        log.info("Razorpay Secret length: {}", keySecret != null ? keySecret.length() : "NULL");
        

        // 1. Calculate amount from DB — never trust frontend amount
        BigDecimal total = BigDecimal.ZERO;
        for (CreateOrderRequest.OrderItemDto item : request.getItems()) {
            ProductResponse product = productService.getById(item.getProductId());

            if (!product.isAvailable()) {
                throw new RuntimeException(product.getName() + " is not available");
            }

            BigDecimal itemTotal = product.getPricePerKg()
                .multiply(item.getQty());
            total = total.add(itemTotal);
        }
        
        System.out.println("back end calculated total"+total);
        
        System.out.println("upi payment..."+request.getUpiPaid());
        System.out.println("cash payment..."+request.getCashPaid());
        
        System.out.println("cash payment..."+request.getCashPaid().add(request.getUpiPaid()));

        System.out.println(total.compareTo(request.getUpiPaid().add(request.getCashPaid())) != 0);
        
        int amountInPaise = 0;
        if((request.getRole().equalsIgnoreCase("ADMIN") || request.getRole().equalsIgnoreCase("SELLER")) && request.getPaymentMode().equalsIgnoreCase("SPLIT")) {
        	if(total.compareTo(request.getUpiPaid().add(request.getCashPaid())) != 0) {
        		throw new RuntimeException("payment mismatch...!");
        	}
        	amountInPaise = request.getUpiPaid().multiply(BigDecimal.valueOf(100)).intValue();
        }else {
        	amountInPaise = total.multiply(BigDecimal.valueOf(100)).intValue();
        }
        
       

        // 2. Convert to paise (Razorpay uses smallest currency unit)
      

        // 3. Create Razorpay order
        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "rcpt_" + System.currentTimeMillis());
            orderRequest.put("payment_capture", 1);

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", razorpayOrder.get("id"));
            response.put("amount", amountInPaise);
            response.put("currency", "INR");
            response.put("keyId", keyId);
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

	public Object verifyAndConfirmByAdmin(VerifyPaymentRequest request) {
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
        
        System.out.println(total.compareTo(request.getUpiPaid().add(request.getCashPaid())) !=0);
        
        if (total.compareTo(request.getUpiPaid().add(request.getCashPaid())) != 0) {
        	throw new RuntimeException("payment mismatch...!");
        }

        // 3. Save order to DB    
        return orderService.placeOrder(request);
	}
}