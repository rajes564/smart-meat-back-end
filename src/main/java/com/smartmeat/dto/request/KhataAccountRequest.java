package com.smartmeat.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

//── Khata ─────────────────────────────────────────────────────────────────────
@Data public class KhataAccountRequest {
 @NotNull Long customerId;
 BigDecimal creditLimit;
}