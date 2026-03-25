package com.smartmeat.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

//── Product ───────────────────────────────────────────────────────────────────
@Data public class ProductRequest {
 @NotBlank @Size(max=100) String name;
 String description;
 @NotNull Long categoryId;
 @NotNull @DecimalMin("0.01") BigDecimal pricePerKg;
 @DecimalMin("0.01") BigDecimal costPerKg;
 BigDecimal stockQty;
 BigDecimal minStockLevel;
 BigDecimal minOrderQty;
 BigDecimal orderStep;
 Integer sortOrder;
}