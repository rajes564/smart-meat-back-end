package com.smartmeat.dto.request;



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
    
    private java.util.List<CreateOrderRequest.OrderItemDto> items;
}