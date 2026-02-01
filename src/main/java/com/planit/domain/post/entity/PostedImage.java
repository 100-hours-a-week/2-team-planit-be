package com.planit.domain.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity // 게시글에 첨부된 이미지 매핑 엔티티
@Table(name = "posted_images")
@Getter
public class PostedImage {
    @Id // PK
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId; // 연결된 게시글 FK

    @Column(name = "image_id", nullable = false)
    private Long imageId; // 저장된 Image 엔티티와 FK

    @Column(name = "is_main_image", nullable = false)
    private Boolean isMainImage; // 대표 이미지 여부

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 등록 시점

    public PostedImage() {
    }

    public PostedImage(Long postId, Long imageId, Boolean isMainImage, LocalDateTime createdAt) {
        this.postId = postId;
        this.imageId = imageId;
        this.isMainImage = isMainImage;
        this.createdAt = createdAt;
    }
}
