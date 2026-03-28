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

//── Expense ───────────────────────────────────────────────────────────────────
@Entity @Table(name = "expenses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Expense {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 30) private String category;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal amount;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "expense_date") private LocalDate expenseDate = LocalDate.now();
    @Column(name = "payment_mode", length = 20) private String paymentMode = "CASH";
    @Column(name = "cash_amount", precision = 10, scale = 2) private BigDecimal cashAmount = BigDecimal.ZERO;
    @Column(name = "account_amount", precision = 10, scale = 2) private BigDecimal accountAmount = BigDecimal.ZERO;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "entered_by") private User enteredBy;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
}