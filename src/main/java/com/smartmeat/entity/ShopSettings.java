package com.smartmeat.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//── ShopSettings ──────────────────────────────────────────────────────────────
@Entity @Table(name = "shop_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShopSettings {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "shop_name", length = 100) private String shopName;
    @Column(length = 200) private String tagline;
    @Column(length = 15) private String phone;
    @Column(length = 150) private String email;
    @Column(columnDefinition = "TEXT") private String address;
    private Double latitude;
    private Double longitude;
    
    @Column(length = 20) 
    private String status = "OPEN";
    
    @Column(name = "open_time", length = 10) 
    private String openTime = "07:00";
    @Column(name = "close_time", length = 10) 
    private String closeTime = "20:00";
    @Column(name = "sunday_close", length = 10)
    private String sundayClose = "14:00";
    
    @Column(name = "logo_url") private String logoUrl;
    @Column(name = "banner_url") private String bannerUrl;
    // Cash drawer and bank/UPI account balances
    @Column(name = "cash_balance", precision = 12, scale = 2) private BigDecimal cashBalance = BigDecimal.ZERO;
    @Column(name = "account_balance", precision = 12, scale = 2) private BigDecimal accountBalance = BigDecimal.ZERO;
}