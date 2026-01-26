package com.planit.domain.post.entity;

import com.planit.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity // posts 테이블과 매핑되는 JPA 엔티티
@Table(name = "posts") // DB 테이블 지정
@Getter
@Setter
@NoArgsConstructor
public class Post {
    @Id // PK
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id; // 게시글 식별자

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User author; // 작성자(User 엔티티)

    @Column(name = "title", nullable = false, length = 24)
    private String title; // 게시글 제목 (제목 최대 24자)

    @Column(name = "content", columnDefinition = "TEXT", length = 2000)
    private String content; // 본문 (textarea, 줄바꿈/공백 유지)

    @Enumerated(EnumType.STRING)
    @Column(name = "board_type", nullable = false)
    private BoardType boardType; // 게시판 타입(enum)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 생성 시점

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 수정 시각 (audit)

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted = false; // 논리 삭제 플래그

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // 삭제된 시점

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", referencedColumnName = "post_id", insertable = false, updatable = false)
    private List<PostedImage> postedImages = new ArrayList<>(); // 첨부된 이미지 목록

    public void addPostedImage(PostedImage image) {
        this.postedImages.add(image);
    }

    public void markDeleted(LocalDateTime deletedAt) {
        this.deleted = true;
        this.deletedAt = deletedAt;
    }

    public void touchUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Post create(User author, String title, String content, BoardType boardType, LocalDateTime now) {
        Post post = new Post();
        post.author = author;
        post.title = title;
        post.content = content;
        post.boardType = boardType;
        post.createdAt = now;
        post.updatedAt = now;
        return post;
    }
}
