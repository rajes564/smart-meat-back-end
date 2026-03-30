package com.smartmeat.service;

import com.smartmeat.dto.request.CreateOrderRequest;
import com.smartmeat.dto.request.OrderRequest;
import com.smartmeat.dto.request.VerifyPaymentRequest;
import com.smartmeat.dto.response.OrderResponse;
import com.smartmeat.dto.response.ProductResponse;
import com.smartmeat.entity.Order;
import com.smartmeat.entity.OrderItem;
import com.smartmeat.entity.Product;
import com.smartmeat.entity.User;
import com.smartmeat.exception.BusinessException;
import com.smartmeat.exception.ResourceNotFoundException;
import com.smartmeat.repository.OrderRepository;
import com.smartmeat.repository.ProductRepository;
import com.smartmeat.security.SecurityUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository   orderRepository;
    private final ProductRepository productRepository;
    private final ProductService    productService;
    private final SseService        sseService;
    private final SecurityUtils     securityUtils;
    private final ShopService       shopService;   // for balance updates

    private final AtomicInteger orderSeq = new AtomicInteger(100);

    /**
     * Read the highest existing SEQ suffix from all order numbers at startup.
     * This prevents the duplicate-key collision that occurs when the app
     * restarts and the in-memory counter resets to 100.
     */
    @PostConstruct
    public void initOrderSeq() {
        try {
            Long maxSeq = orderRepository.findMaxOrderSeq();
            int seed = (maxSeq != null) ? maxSeq.intValue() : 100;
            orderSeq.set(seed);
            log.info("Order sequence initialized to {} from DB", seed);
        } catch (Exception e) {
            log.warn("Could not read max order seq from DB, defaulting to 100. Reason: {}", e.getMessage());
        }
    }

    // ── Place online order (public / guest) ───────────────────────────────────

    @Transactional
    public OrderResponse place(OrderRequest req) {
        Order order = buildOrder(req, true);
        Order saved = orderRepository.save(order);
        saved.getItems().forEach(item ->
            productService.reduceStock(item.getProduct().getId(), item.getQty()));
        OrderResponse response = toResponse(saved);
        sseService.newOrder(response);
        log.info("Online order placed: {}", saved.getOrderNumber());
        return response;
    }

    // ── POS sale (seller / admin) ─────────────────────────────────────────────

    @Transactional
    public OrderResponse posSale(OrderRequest req) {
        Order order = buildOrder(req, false);
        try {
            User seller = securityUtils.currentUser();
            if (seller != null) order.setProcessedBy(seller);
        } catch (Exception ignored) {}
        
        
        Order saved = orderRepository.save(order);
        
        saved.getItems().forEach(item ->
            productService.reduceStock(item.getProduct().getId(), item.getQty()));

        // ── Update shop cash/account balances ─────────────────────────────────
        BigDecimal orderTotal = saved.getTotal();
        String method = saved.getPaymentMethod();
        if (method == null) method = "CASH";

        BigDecimal cashDelta    = BigDecimal.ZERO;
        BigDecimal accountDelta = BigDecimal.ZERO;

        switch (method.toUpperCase()) {
            case "CASH"  -> cashDelta    = orderTotal;
            case "UPI",
                 "CARD"  -> accountDelta = orderTotal;
            case "SPLIT" -> {
                cashDelta    = req.getCashPaid()  != null ? req.getCashPaid()  : BigDecimal.ZERO;
                accountDelta = req.getUpiPaid()   != null ? req.getUpiPaid()   : BigDecimal.ZERO;
            }
            case "KHATA" -> {
                // Only the paid-now portion goes into balances
                BigDecimal cashPart = req.getCashPaid() != null ? req.getCashPaid() : BigDecimal.ZERO;
                BigDecimal upiPart  = req.getUpiPaid()  != null ? req.getUpiPaid()  : BigDecimal.ZERO;
                cashDelta    = cashPart;
                accountDelta = upiPart;
            }
            // default: no balance change (e.g. fully on Khata credit)
        }

        try {
            shopService.addToBalance(cashDelta, accountDelta);
        } catch (Exception e) {
            log.warn("Balance update failed for order {}: {}", saved.getOrderNumber(), e.getMessage());
        }

        return toResponse(saved);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public Page<OrderResponse> list(String status, String date, Pageable pageable) {
        if (status != null && date != null) {
            return orderRepository.findByStatusAndDate(status, LocalDate.parse(date), pageable)
                    .map(this::toResponse);
        } else if (status != null) {
            return orderRepository.findByStatus(status, pageable).map(this::toResponse);
        } else if (date != null) {
            return orderRepository.findByDate(LocalDate.parse(date), pageable).map(this::toResponse);
        }
        return orderRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    public Page<OrderResponse> myOrders(Pageable pageable) {
        User current = securityUtils.currentUser();
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(current.getId(), pageable)
                .map(this::toResponse);
    }

    public OrderResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public OrderResponse updateStatus(Long id, String status) {
        Order order = findById(id);
        validateStatusTransition(order.getStatus(), status);
        order.setStatus(status);
        Order saved = orderRepository.save(order);
        sseService.orderStatusChanged(toResponse(saved));
        return toResponse(saved);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Order buildOrder(OrderRequest req, boolean isOnline) {
        BigDecimal cashPaid = req.getCashPaid() != null ? req.getCashPaid() : BigDecimal.ZERO;
        BigDecimal upiPaid  = req.getUpiPaid()  != null ? req.getUpiPaid()  : BigDecimal.ZERO;

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customerName(req.getCustomerName())
                .customerMobile(req.getCustomerMobile())
                .paymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : "CASH")
                .cashPaid(cashPaid)
                .upiPaid(upiPaid)
                .status("PENDING")
                .onlineOrder(isOnline)
                .notes(req.getNotes())
                .build();

        // Link to logged-in CUSTOMER if present — safe to skip for guests
        try {
            User current = securityUtils.currentUser();
            if (current != null && "CUSTOMER".equals(current.getRole().name())) {
                order.setCustomer(current);
            }
        } catch (Exception ignored) {}

        BigDecimal total = BigDecimal.ZERO;
        for (OrderRequest.ItemRequest itemReq : req.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found: " + itemReq.getProductId()));

            if (!product.isAvailable()) {
                throw new BusinessException("Product not available: " + product.getName());
            }
            if (product.getStockQty().compareTo(itemReq.getQty()) < 0) {
                throw new BusinessException("Insufficient stock for: " + product.getName()
                        + " (available: " + product.getStockQty() + " kg)");
            }

            BigDecimal lineTotal = product.getPricePerKg().multiply(itemReq.getQty());
            order.getItems().add(OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .qty(itemReq.getQty())
                    .unitPrice(product.getPricePerKg())
                    .total(lineTotal)
                    .build());
            total = total.add(lineTotal);
        }
        order.setSubtotal(total);
        order.setTotal(total);
        return order;
    }

    /**
     * Generates ORD-YYMMDD-SEQ.
     * orderSeq is always > any existing SEQ because it was seeded from DB at startup.
     */
    private String generateOrderNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        return "ORD-" + date + "-" + orderSeq.incrementAndGet();
    }

    private void validateStatusTransition(String current, String next) {
        boolean valid = switch (current) {
            case "PENDING"   -> next.equals("ACCEPTED")  || next.equals("CANCELLED");
            case "ACCEPTED"  -> next.equals("PREPARING") || next.equals("CANCELLED");
            case "PREPARING" -> next.equals("READY");
            case "READY"     -> next.equals("COLLECTED");
            default          -> false;
        };
        if (!valid) throw new BusinessException(
                "Invalid status transition: " + current + " → " + next);
    }

    private Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    public OrderResponse toResponse(Order o) {
        return OrderResponse.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .customerName(o.getCustomerName())
                .customerMobile(o.getCustomerMobile())
                .status(o.getStatus())
                .paymentMethod(o.getPaymentMethod())
                .subtotal(o.getSubtotal())
                .total(o.getTotal())
                .notes(o.getNotes())
                .onlineOrder(o.isOnlineOrder())
                .createdAt(o.getCreatedAt())
                .items(o.getItems().stream().map(i -> OrderResponse.ItemResponse.builder()
                        .id(i.getId())
                        .productId(i.getProduct() != null ? i.getProduct().getId() : null)
                        .productName(i.getProductName())
                        .qty(i.getQty())
                        .unitPrice(i.getUnitPrice())
                        .total(i.getTotal())
                        .build()).toList())
                .build();
    }


	
    @Transactional
    public OrderResponse placeOrder(VerifyPaymentRequest req) {
        Order order = buildOrder1(req, true);
        Order saved = orderRepository.save(order);

        saved.getItems().forEach(item ->
            productService.reduceStock(item.getProduct().getId(), item.getQty()));
        
        
        try {
            shopService.addToBalance( order.getCashPaid(),order.getUpiPaid());
        } catch (Exception e) {
            log.warn("Balance update failed for order {}: {}", saved.getOrderNumber(), e.getMessage());
        }

        OrderResponse response = toResponse(saved);
        sseService.newOrder(response);
        log.info("Online order placed: {}", saved.getOrderNumber());
        return response;
    }
    

private Order buildOrder1(VerifyPaymentRequest req, boolean isOnline) {

    // ── Build items + calculate total in ONE loop ──────────────────
    List<OrderItem> orderItems = new ArrayList<>();
    BigDecimal total = BigDecimal.ZERO;

    for (CreateOrderRequest.OrderItemDto item : req.getItems()) {
        Product product = productRepository.findById(item.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + item.getProductId()));

        if (!product.isAvailable()) {
            throw new BusinessException("Product not available: " + product.getName());
        }
        if (product.getStockQty().compareTo(item.getQty()) < 0) {
            throw new BusinessException("Insufficient stock for: " + product.getName()
                    + " (available: " + product.getStockQty() + " kg)");
        }

        // ✅ Fix 1: was `total.add(total)` — should be lineTotal
        BigDecimal lineTotal = product.getPricePerKg().multiply(item.getQty());
        total = total.add(lineTotal);

        orderItems.add(OrderItem.builder()
                .product(product)
                .productName(product.getName())
                .qty(item.getQty())
                .unitPrice(product.getPricePerKg())
                // ✅ Fix 2: set lineTotal on item too
                .total(lineTotal)
                .build());
    }

    // ── Build order ────────────────────────────────────────────────
    Order order = Order.builder()
            .orderNumber(generateOrderNumber())
            .customerName(req.getCustomerName())
            .customerMobile(req.getCustomerMobile())
            .customerEmail(req.getCustomerEmail())
            .notes(req.getNotes())
            .onlineOrder(isOnline)
            .status("PAID")
            .razorpayOrderId(req.getRazorpayOrderId())
            .razorpayPaymentId(req.getRazorpayPaymentId())
            .paymentMethod("Razorpay")
            .upiPaid(total)
            .cashPaid(BigDecimal.ZERO)
            // ✅ Fix 3: was set to total1 (ZERO) — now correctly set to total
            .subtotal(total)
            .total(total)
            .build();

    // ✅ Fix 4: link items to order AFTER order is built
    orderItems.forEach(item -> item.setOrder(order));
    order.setItems(orderItems);

    // ── Link logged-in customer if present ─────────────────────────
    try {
        User current = securityUtils.currentUser();
        if (current != null && "CUSTOMER".equals(current.getRole().name())) {
            order.setCustomer(current);
        }
    } catch (Exception ignored) {}


    return order;
}
}