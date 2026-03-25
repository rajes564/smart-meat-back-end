package com.smartmeat.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

//── Stock Purchase ────────────────────────────────────────────────────────────
@Data @Builder public class StockPurchaseResponse {
 Long id;
 Long productId;
 String productName;
 String supplierName;
 BigDecimal qty;
 BigDecimal costPerKg;
 BigDecimal totalCost;
 BigDecimal amountPaid;
 BigDecimal amountDue;
 String invoiceNo;
 String purchaseDate;
 String enteredByName;
}
