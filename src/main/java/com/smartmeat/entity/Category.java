package com.smartmeat.entity;

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

 // ── Category ──────────────────────────────────────────────────────────────────
@Entity @Table(name = "categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 50) private String name;
    @Column(unique = true, length = 50) private String slug;
    @Column(length = 10) private String icon;
    @Column(name = "sort_order") private Integer sortOrder = 0;
    @Column(name = "is_active") private boolean active = true;
}


