package com.smartmeat.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

//── Expense ───────────────────────────────────────────────────────────────────
@Data 
public class ExpenseRequest {
    @NotBlank String category;
    @NotNull @DecimalMin("0.01") BigDecimal amount;
    BigDecimal cashAmount;      // cash portion
    BigDecimal accountAmount;   // UPI/card portion
    String paymentMode;         // CASH | UPI | CARD | SPLIT
    String description;
    String expenseDate;
}