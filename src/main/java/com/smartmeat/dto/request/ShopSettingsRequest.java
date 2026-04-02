package com.smartmeat.dto.request;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ShopSettingsRequest {
 
    // Basic info
    private String shopName;
    private String tagline;
 
    // Contact
    private String phone;
    private String email;
    private String address;
 
    // Location
    private Double latitude;
    private Double longitude;
 
    // Hours
    private String status;
    private String openTime;
    private String closeTime;
    private String sundayClose;
 
    // Balances
    private BigDecimal cashBalance;
    private BigDecimal accountBalance;
 
    // Hero carousel metadata (order determines sortOrder)
    private List<HeroSlideMeta> heroSlidesMeta;
 
    // Gallery metadata
    private List<GalleryItemMeta> galleryMeta;
}
 