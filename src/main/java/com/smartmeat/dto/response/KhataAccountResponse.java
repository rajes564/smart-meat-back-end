package com.smartmeat.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

//── Khata ─────────────────────────────────────────────────────────────────────
@Data @Builder public class KhataAccountResponse {
 Long id;
 Long customerId;
 String customerName;
 String customerMobile;
 BigDecimal creditLimit;
 BigDecimal currentDue;
 BigDecimal totalCredit;
 boolean active;
 double usagePercent;
}
