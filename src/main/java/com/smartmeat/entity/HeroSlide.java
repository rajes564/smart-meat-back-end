package com.smartmeat.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hero_slides")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HeroSlide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Client-side UUID, kept so the frontend can match files by id */
    @Column(name = "client_id", length = 64, unique = true)
    private String clientId;

    @Column(name = "src", columnDefinition = "TEXT", nullable = false)
    private String src;

    @Column(name = "caption", length = 500)
    private String caption;

    /** Display order — lower = earlier in carousel */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_settings_id", nullable = false)
    private ShopSettings shopSettings;
}