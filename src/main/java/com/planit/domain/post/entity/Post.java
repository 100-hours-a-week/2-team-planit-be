package com.planit.domain.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity // posts 테이블과 매핑되는 JPA 엔티티
@Table(name = "posts") // DB 테이블 지정
@Getter
public class Post {
    @Id // PK
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id; // 게시글 식별자

    @Column(name = "user_id", nullable = false)
    private Long userId; // 작성자 FK

    @Column(name = "title", nullable = false, length = 100)
    private String title; // 게시글 제목

    @Column(name = "content", columnDefinition = "TEXT")
    private String content; // 본문 (줄바꿈/공백 유지)

    @Enumerated(EnumType.STRING)
    @Column(name = "board_type", nullable = false)
    private BoardType boardType; // 게시판 구분 (enum으로 관리)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 등록 시점
}
