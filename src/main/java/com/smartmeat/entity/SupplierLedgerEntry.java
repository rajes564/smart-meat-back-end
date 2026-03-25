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

@Entity @Table(name = "supplier_ledger_entries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierLedgerEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "supplier_id") private Supplier supplier;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "purchase_id") private StockPurchase purchase;
    @Column(name = "entry_type", nullable = false, length = 10) private String entryType;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal amount;
    @Column(name = "cash_amount", precision = 10, scale = 2) private BigDecimal cashAmount = BigDecimal.ZERO;
    @Column(name = "account_amount", precision = 10, scale = 2) private BigDecimal accountAmount = BigDecimal.ZERO;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "reference_note", length = 200) private String referenceNote;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "entered_by") private User enteredBy;
    @Column(name = "entry_date") private LocalDate entryDate = LocalDate.now();
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
}