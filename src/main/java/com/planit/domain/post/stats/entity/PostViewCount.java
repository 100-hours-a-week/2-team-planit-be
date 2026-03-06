package com.planit.domain.post.stats.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_view_count")
public class PostViewCount {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected PostViewCount() {
    }

    public PostViewCount(Long postId, long viewCount) {
        this.postId = postId;
        this.viewCount = viewCount;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getPostId() {
        return postId;
    }

    public long getViewCount() {
        return viewCount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void replace(long viewCount) {
        this.viewCount = viewCount;
        this.updatedAt = LocalDateTime.now();
    }

    public void increase() {
        this.viewCount += 1;
        this.updatedAt = LocalDateTime.now();
    }
}
