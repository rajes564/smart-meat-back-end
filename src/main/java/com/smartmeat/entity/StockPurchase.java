package com.smartmeat.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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

//── StockPurchase ─────────────────────────────────────────────────────────────
@Entity @Table(name = "stock_purchases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockPurchase {

@Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "product_id") private Product product;
@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "supplier_id") private Supplier supplier;
@Column(name = "supplier_name", length = 100) private String supplierName;
@Column(nullable = false, precision = 10, scale = 3) private BigDecimal qty;
@Column(name = "cost_per_kg", nullable = false, precision = 10, scale = 2) private BigDecimal costPerKg;
@Column(name = "total_cost", nullable = false, precision = 10, scale = 2) private BigDecimal totalCost;
@Column(name = "amount_paid", precision = 10, scale = 2) private BigDecimal amountPaid = BigDecimal.ZERO;
@Column(name = "cash_paid", precision = 10, scale = 2) private BigDecimal cashPaid = BigDecimal.ZERO;
@Column(name = "account_paid", precision = 10, scale = 2) private BigDecimal accountPaid = BigDecimal.ZERO;
@Column(name = "payment_mode", length = 20) private String paymentMode = "CASH";
@Column(name = "invoice_no", length = 50) private String invoiceNo;
@Column(name = "purchase_date") private LocalDate purchaseDate = LocalDate.now();
@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "entered_by") private User enteredBy;
@CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;

}