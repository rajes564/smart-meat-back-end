package com.smartmeat.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data public class KhataEntryRequest {
    @NotNull Long accountId;
    @NotBlank String entryType; // DEBIT | CREDIT
    @NotNull @DecimalMin("0.01") BigDecimal amount;
    String description;
    String referenceNote;
    Long orderId;
}
