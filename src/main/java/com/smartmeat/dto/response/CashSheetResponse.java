package com.smartmeat.dto.response;

import java.math.BigDecimal;
import java.util.List;


import lombok.Builder;
import lombok.Data;

@Data @Builder
public class CashSheetResponse {
    BigDecimal openingBalance;
    BigDecimal openingCash;      // cash drawer portion of opening
    BigDecimal openingAccount;   // bank/UPI portion of opening
    BigDecimal totalCredit;
    BigDecimal totalDebit;
    BigDecimal closingBalance;
    List<CashSheetEntry> entries;
 
    @Data @Builder public static class CashSheetEntry {
        String date;
        String type;         // CREDIT | DEBIT
        String category;     // SALES | STOCK_PURCHASE | EXPENSE | KHATA_PAYMENT
        String description;
        BigDecimal amount;
        BigDecimal cashAmount;     // cash portion of this entry
        BigDecimal accountAmount;  // UPI/card portion of this entry
        String staff;
        BigDecimal runningBalance;
        List<SubEntry> subEntries;
 
        @Data @Builder public static class SubEntry {
            String time;
            String label;
            String detail;
            BigDecimal amount;
            BigDecimal cashAmount;
            BigDecimal accountAmount;
            String paymentMethod;    // human-readable label e.g. "Cash ₹200 + UPI ₹300"
        }
    }
}