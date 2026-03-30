package com.smartmeat.dto.request;



import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateOrderRequest {
    private String customerName;
    private String customerMobile;
    private String customerEmail;
    private String notes;
    private List<OrderItemDto> items;
    
    private String paymentMode;
    private String role;
    
    private BigDecimal cashPaid;
    private BigDecimal upiPaid;

    @Data
    public static class OrderItemDto {
        private Long productId;
        private BigDecimal qty;
    }
}
