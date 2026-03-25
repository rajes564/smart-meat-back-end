package com.smartmeat.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

//── Stock Purchase ────────────────────────────────────────────────────────────
@Data 
public class StockPurchaseRequest {
    @NotNull Long productId;
    Long   supplierId;      // optional — if set, links to Supplier entity
    String supplierName;    // fallback if no supplierId
    @NotNull @DecimalMin("0.1") BigDecimal qty;
    @NotNull @DecimalMin("0.01") BigDecimal costPerKg;
    BigDecimal amountPaid;
    BigDecimal cashPaid;       // portion paid in cash
    BigDecimal accountPaid;    // portion paid via UPI/bank
    String paymentMode;        // CASH | UPI | CARD | SPLIT
    String invoiceNo;
    String purchaseDate;
}