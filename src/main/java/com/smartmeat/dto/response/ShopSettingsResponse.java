package com.smartmeat.dto.response;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//── Shop Settings ─────────────────────────────────────────────────────────────
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ShopSettingsResponse {
    private String     shopName;
    private String     tagline;
    private String     phone;
    private String     email;
    private String     address;
    private Double     latitude;
    private Double     longitude;
    private String     status;
    private String     openTime;
    private String     closeTime;
    private String     sundayClose;
    private String     logoUrl;
    private String     bannerUrl;
    private BigDecimal cashBalance;
    private BigDecimal accountBalance;
 
    private List<HeroSlideResponse>   heroImages;    // key matches frontend expectation
    private List<GalleryItemResponse> galleryItems;
}