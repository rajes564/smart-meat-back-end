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

//── KhataAccount ──────────────────────────────────────────────────────────────
@Entity @Table(name = "khata_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KhataAccount {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "customer_id") private User customer;
 @Column(name = "credit_limit", precision = 10, scale = 2) private BigDecimal creditLimit = BigDecimal.valueOf(10000);
 @Column(name = "current_due", precision = 10, scale = 2) private BigDecimal currentDue = BigDecimal.ZERO;
 @Column(name = "total_credit", precision = 10, scale = 2) private BigDecimal totalCredit = BigDecimal.ZERO;
 @Column(name = "is_active") private boolean active = true;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by") private User createdBy;
 @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
}
