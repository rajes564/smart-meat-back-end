package com.smartmeat.service;

import com.smartmeat.dto.request.OrderRequest;
import com.smartmeat.dto.response.OrderResponse;
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
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository  orderRepository;
    private final ProductRepository productRepository;
    private final ProductService   productService;
    private final SseService       sseService;
    private final SecurityUtils    securityUtils;

    // Seeded from DB on startup — never resets to a value that already exists
    private final AtomicInteger orderSeq = new AtomicInteger(100);

    /**
     * Read the highest existing SEQ suffix from all order numbers at startup.
     * This prevents the duplicate-key collision that occurs when the app
     * restarts and the in-memory counter resets to 100.
     */
    @PostConstruct
    public void initOrderSeq() {
        try {
            int maxSeq = orderRepository.findMaxOrderSeq();
            orderSeq.set(maxSeq);
            log.info("Order sequence initialized to {} from DB", maxSeq);
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
        // Attach the seller who processed it
        try {
            User seller = securityUtils.currentUser();
            if (seller != null) order.setProcessedBy(seller);
        } catch (Exception ignored) {}
        Order saved = orderRepository.save(order);
        saved.getItems().forEach(item ->
            productService.reduceStock(item.getProduct().getId(), item.getQty()));
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
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customerName(req.getCustomerName())
                .customerMobile(req.getCustomerMobile())
                .paymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : "CASH")
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
}