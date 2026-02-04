package com.planit.domain.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity // uploads/이미지 메타를 저장하는 테이블과 매핑
@Table(name = "images") // images 테이블 지정
@Getter
public class Image {
    @Id // PK
    @GeneratedValue(strategy = GenerationType.IDENTITY) // auto increment 전략
    private Long id; // 이미지 식별자

    @Column(name = "file_name", nullable = false) // 원본 파일명
    private String fileName;

    @Column(name = "file_size", nullable = false) // 바이트 단위 파일 크기
    private Long fileSize;

    @Column(name = "s3_key", length = 500) // S3 객체 key (Presigned URL 업로드용)
    private String s3Key;

    @Column(name = "created_at", nullable = false) // 저장 시점
    private LocalDateTime createdAt;

    public Image() {
    }

    /** Presigned URL 업로드 후 s3_key로 생성 (fileName은 key의 마지막 경로, fileSize는 0) */
    public Image(String s3Key, LocalDateTime createdAt) {
        this.fileName = s3Key != null ? s3Key.substring(s3Key.lastIndexOf('/') + 1) : "";
        this.fileSize = 0L;
        this.s3Key = s3Key;
        this.createdAt = createdAt;
    }
}