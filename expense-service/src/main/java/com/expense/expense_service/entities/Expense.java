package com.expense.expense_service.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private BigDecimal amount;

    private String merchant;

    @Builder.Default
    private String currency = "INR";

    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.externalId == null) {
            this.externalId = UUID.randomUUID().toString();
        }
        if (this.currency == null) {
            this.currency = "INR";
        }
        this.createdAt = Timestamp.from(Instant.now());
        this.updatedAt = Timestamp.from(Instant.now());
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Timestamp.from(Instant.now());
    }
}