package com.smartmeat.dto.request;



import java.math.BigDecimal;

import lombok.Data;

@Data
public class VerifyPaymentRequest {
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    private String customerName;
    private String customerMobile;
    private String customerEmail;
    private String notes;
    
    
    private BigDecimal upiPaid;
    private BigDecimal cashPaid;
    
    private java.util.List<CreateOrderRequest.OrderItemDto> items;
}