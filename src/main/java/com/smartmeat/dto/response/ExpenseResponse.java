package com.smartmeat.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

//── Expense ───────────────────────────────────────────────────────────────────
@Data @Builder public class ExpenseResponse {
 Long id;
 String category;
 BigDecimal amount;
 String description;
 String expenseDate;
 String enteredByName;
}
