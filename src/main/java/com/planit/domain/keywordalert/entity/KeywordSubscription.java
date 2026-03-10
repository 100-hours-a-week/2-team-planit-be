package com.planit.domain.keywordalert.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "keyword_subscriptions",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_keyword_subscription_user_keyword",
            columnNames = {"user_id", "keyword"}
        )
    },
    indexes = {
        @Index(name = "idx_keyword_subscription_user", columnList = "user_id")
    }
)
public class KeywordSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscription_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "keyword", nullable = false, length = 10)
    private String keyword;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected KeywordSubscription() {
    }

    public KeywordSubscription(Long userId, String keyword, LocalDateTime createdAt) {
        this.userId = userId;
        this.keyword = keyword;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getSubscriptionId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getKeyword() {
        return keyword;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
