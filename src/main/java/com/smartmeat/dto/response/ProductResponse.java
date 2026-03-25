package com.smartmeat.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

//── Product ───────────────────────────────────────────────────────────────────
@Data @Builder public class ProductResponse {
 Long id;
 String name;
 String description;
 Long categoryId;
 String categoryName;
 String categoryIcon;
 BigDecimal pricePerKg;
 BigDecimal costPerKg;
 BigDecimal stockQty;
 BigDecimal minStockLevel;
 BigDecimal minOrderQty;
 BigDecimal orderStep;
 String stockStatus; // IN_STOCK | LOW_STOCK | OUT_OF_STOCK
 String imageUrl;
 boolean available;
 Integer sortOrder;
}