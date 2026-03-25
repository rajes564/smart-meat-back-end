package com.smartmeat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "price_per_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerKg;

    @Column(name = "cost_per_kg", precision = 10, scale = 2)
    private BigDecimal costPerKg;

    @Column(name = "stock_qty", precision = 10, scale = 3)
    private BigDecimal stockQty = BigDecimal.ZERO;

    @Column(name = "min_stock_level", precision = 10, scale = 3)
    private BigDecimal minStockLevel = BigDecimal.valueOf(5);

    @Column(name = "min_order_qty", precision = 5, scale = 3)
    private BigDecimal minOrderQty = BigDecimal.valueOf(0.5);

    @Column(name = "order_step", precision = 5, scale = 3)
    private BigDecimal orderStep = BigDecimal.valueOf(0.5);

    @Column(name = "stock_status", length = 20)
    private String stockStatus = "IN_STOCK";

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "is_available")
    private boolean available = true;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Computed helper
    @Transient
    public String computedStockStatus() {
        if (stockQty == null || stockQty.compareTo(BigDecimal.ZERO) <= 0) return "OUT_OF_STOCK";
        if (minStockLevel != null && stockQty.compareTo(minStockLevel) <= 0) return "LOW_STOCK";
        return "IN_STOCK";
    }
}
