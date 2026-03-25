package com.smartmeat.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data @Builder public class KhataEntryResponse {
    Long id;
    String entryType;
    BigDecimal amount;
    String description;
    String referenceNote;
    String entryDate;
    String enteredByName;
    BigDecimal runningBalance;
}
