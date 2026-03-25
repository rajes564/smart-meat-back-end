package com.smartmeat.entity;

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

//── Review ────────────────────────────────────────────────────────────────────
@Entity @Table(name = "reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {
 @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
 @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id") private Order order;
 @Column(nullable = false) private Integer rating;
 @Column(columnDefinition = "TEXT") private String comment;
 @Column(name = "submitter_name", length = 100) private String submitterName;
 @Column(columnDefinition = "text[]") private String[] tags;
 @Column(name = "is_approved") private boolean approved = true;
 @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
}