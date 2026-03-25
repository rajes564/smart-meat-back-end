package com.smartmeat.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data @Builder public class KhataLedgerResponse {
    KhataAccountResponse account;
    List<KhataEntryResponse> entries;
    BigDecimal runningBalance;
}
