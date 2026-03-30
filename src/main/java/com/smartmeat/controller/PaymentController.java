package com.smartmeat.controller;



import com.smartmeat.dto.request.CreateOrderRequest;
import com.smartmeat.dto.request.VerifyPaymentRequest;
import com.smartmeat.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    

    // Step 1: Frontend calls this to create Razorpay order
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
    	
        return ResponseEntity.ok(paymentService.createOrder(request));
    }

    // Step 2: Frontend calls this after payment to verify
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody VerifyPaymentRequest request) {
        return ResponseEntity.ok(paymentService.verifyAndConfirm(request));
    }
    
    
    @PostMapping("/verify-by-admin")
    public ResponseEntity<?> verifyPaymentByAdmin(@RequestBody VerifyPaymentRequest request) {
    	System.out.println("verify by admin is touched.....");
    	System.out.println(request);
    	
        return ResponseEntity.ok(paymentService.verifyAndConfirmByAdmin(request));
    }
}