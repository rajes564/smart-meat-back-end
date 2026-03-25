package com.smartmeat.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.smartmeat.dto.response.OrderResponse.ItemResponse;

import lombok.Builder;
import lombok.Data;

//── Order ─────────────────────────────────────────────────────────────────────
@Data @Builder public class OrderResponse {
 Long id;
 String orderNumber;
 String customerName;
 String customerMobile;
 String status;
 String paymentMethod;
 BigDecimal subtotal;
 BigDecimal total;
 String notes;
 boolean onlineOrder;
 Instant createdAt;
 List<ItemResponse> items;

 @Data @Builder public static class ItemResponse {
     Long id;
     Long productId;
     String productName;
     BigDecimal qty;
     BigDecimal unitPrice;
     BigDecimal total;
 }
}