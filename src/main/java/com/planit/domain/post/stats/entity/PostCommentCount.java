package com.planit.domain.post.stats.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_comment_count")
public class PostCommentCount {

    @Id
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "comment_count", nullable = false)
    private long commentCount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected PostCommentCount() {
    }

    public PostCommentCount(Long postId, long commentCount) {
        this.postId = postId;
        this.commentCount = commentCount;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getPostId() {
        return postId;
    }

    public long getCommentCount() {
        return commentCount;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void replace(long commentCount) {
        this.commentCount = commentCount;
        this.updatedAt = LocalDateTime.now();
    }

    public void increase() {
        this.commentCount += 1;
        this.updatedAt = LocalDateTime.now();
    }

    public void decrease() {
        this.commentCount = Math.max(0, this.commentCount - 1);
        this.updatedAt = LocalDateTime.now();
    }
}
