package com.smartmeat.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;



// ── Notification ──────────────────────────────────────────────────────────────
@Entity @Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id") private User user;
    @Column(nullable = false, length = 200) private String title;
    @Column(columnDefinition = "TEXT") private String message;
    @Column(length = 30) private String type = "INFO";
    @Column(name = "entity_type", length = 30) private String entityType;
    @Column(name = "entity_id") private Long entityId;
    @Column(name = "is_read") private boolean read = false;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
}
