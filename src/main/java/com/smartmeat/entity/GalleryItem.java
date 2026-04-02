package com.smartmeat.entity;



import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "gallery_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GalleryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Client-side UUID */
    @Column(name = "client_id", length = 64, unique = true)
    private String clientId;

    /** "image" or "video" */
    @Column(name = "type", length = 10, nullable = false)
    private String type;

    @Column(name = "src", columnDefinition = "TEXT", nullable = false)
    private String src;

    @Column(name = "caption", length = 500)
    private String caption;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_settings_id", nullable = false)
    private ShopSettings shopSettings;
}