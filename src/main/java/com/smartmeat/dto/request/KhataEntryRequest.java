package com.smartmeat.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class KhataEntryRequest {
    @NotNull Long accountId;
    @NotBlank String entryType;   // DEBIT | CREDIT
    @NotNull @DecimalMin("0.01") BigDecimal amount;
    String paymentMode;           // CASH | UPI | CARD | SPLIT
    BigDecimal cashAmount;        // cash portion (for SPLIT or CASH)
    BigDecimal accountAmount;     // UPI/card portion (for SPLIT or UPI/CARD)
    String description;
    String referenceNote;
    Long orderId;
}
