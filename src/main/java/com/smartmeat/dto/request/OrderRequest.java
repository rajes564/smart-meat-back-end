package com.smartmeat.dto.request;

import java.math.BigDecimal;
import java.util.List;

import com.smartmeat.dto.request.OrderRequest.ItemRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

//── Order ─────────────────────────────────────────────────────────────────────
@Data public class OrderRequest {
 @NotBlank String customerName;
 @NotBlank @Size(min=10, max=15) String customerMobile;
 String paymentMethod;
 String notes;
 @NotEmpty @Valid List<ItemRequest> items;

 @Data public static class ItemRequest {
     @NotNull Long productId;
     @NotNull @DecimalMin("0.1") BigDecimal qty;
 }
}