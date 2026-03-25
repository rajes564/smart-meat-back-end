package com.smartmeat.dto.response;

import java.math.BigDecimal;
import java.util.List;

import com.smartmeat.dto.response.CashSheetResponse.CashSheetEntry;
import com.smartmeat.dto.response.CashSheetResponse.CashSheetEntry.SubEntry;

import lombok.Builder;
import lombok.Data;

//── Cash Sheet ────────────────────────────────────────────────────────────────
@Data @Builder public class CashSheetResponse {
 BigDecimal openingBalance;
 BigDecimal totalCredit;
 BigDecimal totalDebit;
 BigDecimal closingBalance;
 List<CashSheetEntry> entries;

 @Data @Builder public static class CashSheetEntry {
     String date;
     String type; // CREDIT | DEBIT
     String category; // SALES | STOCK_PURCHASE | EXPENSE | KHATA_PAYMENT
     String description;
     BigDecimal amount;
     String staff;
     BigDecimal runningBalance;
     List<SubEntry> subEntries;

     @Data @Builder public static class SubEntry {
         String time;
         String label;
         String detail;
         BigDecimal amount;
         String paymentMethod;
     }
 }
}