package com.smartmeat.controller;

import com.smartmeat.dto.request.OrderRequest;
import com.smartmeat.dto.response.OrderResponse;
import com.smartmeat.service.OrderService;
import com.smartmeat.service.SseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final SseService sseService;

    // SSE stream — admin/seller subscribe here for live order updates
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return sseService.register();
    }

    // Customer places an online order
    @PostMapping
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody OrderRequest req) {
    	System.out.println("Controller touched........");
        return ResponseEntity.ok(orderService.place(req));
    }

    // Admin/Seller — view all orders with filters
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    public ResponseEntity<Page<OrderResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date,
            Pageable pageable) {
        return ResponseEntity.ok(orderService.list(status, date, pageable));
    }

    // Customer — view their own orders
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Page<OrderResponse>> myOrders(Pageable pageable) {
        return ResponseEntity.ok(orderService.myOrders(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    // Admin/Seller — update status
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(orderService.updateStatus(id, status));
    }

    // Admin/Seller — POS sale entry (new sale screen)
    @PostMapping("/pos")
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    public ResponseEntity<OrderResponse> posSale(@Valid @RequestBody OrderRequest req) {
    	System.out.println("pos sale touched......" +req);
        return ResponseEntity.ok(orderService.posSale(req));
    }
}
