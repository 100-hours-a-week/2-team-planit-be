package com.planit.domain.post.stats.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_like_count")
public class PostLikeCount {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "like_count", nullable = false)
    private long likeCount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected PostLikeCount() {
    }

    public PostLikeCount(Long postId, long likeCount) {
        this.postId = postId;
        this.likeCount = likeCount;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getPostId() {
        return postId;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void replace(long likeCount) {
        this.likeCount = likeCount;
        this.updatedAt = LocalDateTime.now();
    }

    public void increase() {
        this.likeCount += 1;
        this.updatedAt = LocalDateTime.now();
    }

    public void decrease() {
        this.likeCount = Math.max(0, this.likeCount - 1);
        this.updatedAt = LocalDateTime.now();
    }
}
