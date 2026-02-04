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

    @Column(name = "created_at", nullable = false) // 저장 시점
    private LocalDateTime createdAt;

    public Image() {
    }

    public Image(String fileName, Long fileSize, LocalDateTime createdAt) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
    }
}
