package com.smartmeat.entity;


import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shop_settings")
@Getter
@Setter
@NoArgsConstructor
@Builder
public class ShopSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_name", length = 100)  private String shopName;
    @Column(length = 200)                        private String tagline;
    @Column(length = 15)                         private String phone;
    @Column(length = 150)                        private String email;
    @Column(columnDefinition = "TEXT")           private String address;
    private Double latitude;
    private Double longitude;

    @Column(length = 20)
    private String status = "OPEN";

    @Column(name = "open_time",    length = 10) private String openTime    = "07:00";
    @Column(name = "close_time",   length = 10) private String closeTime   = "20:00";
    @Column(name = "sunday_close", length = 10) private String sundayClose = "14:00";

    @Column(name = "logo_url")   private String logoUrl;
    @Column(name = "banner_url") private String bannerUrl;

    @Column(name = "cash_balance",    precision = 12, scale = 2)
    private BigDecimal cashBalance = BigDecimal.ZERO;

    @Column(name = "account_balance", precision = 12, scale = 2)
    private BigDecimal accountBalance = BigDecimal.ZERO;

    // ── Hero Carousel ────────────────────────────────────────────────────────
    @OneToMany(
        mappedBy      = "shopSettings",
        cascade       = CascadeType.ALL,
        orphanRemoval = true,
        fetch         = FetchType.LAZY
    )
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<HeroSlide> heroSlides = new ArrayList<>();

    // ── Gallery ──────────────────────────────────────────────────────────────
    @OneToMany(
        mappedBy      = "shopSettings",
        cascade       = CascadeType.ALL,
        orphanRemoval = true,
        fetch         = FetchType.LAZY
    )
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<GalleryItem> galleryItems = new ArrayList<>();

    // Explicit all-args constructor required when mixing
    // @NoArgsConstructor + @Builder + @Builder.Default
    @Builder
    public ShopSettings(
            Long id, String shopName, String tagline, String phone, String email,
            String address, Double latitude, Double longitude, String status,
            String openTime, String closeTime, String sundayClose,
            String logoUrl, String bannerUrl,
            BigDecimal cashBalance, BigDecimal accountBalance,
            List<HeroSlide> heroSlides, List<GalleryItem> galleryItems
    ) {
        this.id             = id;
        this.shopName       = shopName;
        this.tagline        = tagline;
        this.phone          = phone;
        this.email          = email;
        this.address        = address;
        this.latitude       = latitude;
        this.longitude      = longitude;
        this.status         = status      != null ? status      : "OPEN";
        this.openTime       = openTime    != null ? openTime    : "07:00";
        this.closeTime      = closeTime   != null ? closeTime   : "20:00";
        this.sundayClose    = sundayClose != null ? sundayClose : "14:00";
        this.logoUrl        = logoUrl;
        this.bannerUrl      = bannerUrl;
        this.cashBalance    = cashBalance    != null ? cashBalance    : BigDecimal.ZERO;
        this.accountBalance = accountBalance != null ? accountBalance : BigDecimal.ZERO;
        this.heroSlides     = heroSlides     != null ? heroSlides     : new ArrayList<>();
        this.galleryItems   = galleryItems   != null ? galleryItems   : new ArrayList<>();
    }
} 