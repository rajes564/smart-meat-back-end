package com.smartmeat.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//── Supplier ──────────────────────────────────────────────────────────────────
@Entity @Table(name = "suppliers")
@Getter @Setter 
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @Column(nullable = false, length = 100) private String name;
 @Column(length = 15) private String mobile;
 @Column(length = 150) private String email;
 @Column(columnDefinition = "TEXT") private String address;
 @Column(columnDefinition = "TEXT") private String products;
 @Column(name = "credit_limit", precision = 10, scale = 2) private BigDecimal creditLimit = BigDecimal.ZERO;
 @Column(name = "current_due", precision = 10, scale = 2) private BigDecimal currentDue = BigDecimal.ZERO;
 @Column(name = "is_active") private boolean active = true;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by") private User createdBy;
 @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
}