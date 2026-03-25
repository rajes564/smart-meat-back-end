package com.smartmeat.dto.response;

import lombok.Builder;
import lombok.Data;

//── Shop Settings ─────────────────────────────────────────────────────────────
@Data
@Builder 
public class ShopSettingsResponse {
    String shopName;
    String tagline;
    String phone;
    String email;
    String address;
    Double latitude;
    Double longitude;
    String status;
    String openTime;
    String closeTime;
    String sundayClose;
    String logoUrl;
    String bannerUrl;
    java.math.BigDecimal cashBalance;
    java.math.BigDecimal accountBalance;
}