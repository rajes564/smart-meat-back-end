package com.smartmeat.dto.request;

import lombok.Data;

//── Shop Settings ─────────────────────────────────────────────────────────────
@Data public class ShopSettingsRequest {
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
}