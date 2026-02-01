package com.planit.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity // JPA 엔티티로 관리됨
@Table(name = "users") // users 테이블과 매핑
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id; // 자동 생성되는 사용자 PK

    @Column(name = "login_id", nullable = false, length = 20, unique = true)
    private String loginId; // 로그인 ID (고유)

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 10, unique = true)
    private String nickname; // 고유 닉네임

    @Column(columnDefinition = "json")
    private String preferences;

    @Column(name = "profile_image_url")
    private String profileImageUrl;
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // soft delete

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void markDeleted(LocalDateTime when) {
        this.deleted = true;
        this.deletedAt = when;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
        this.updatedAt = LocalDateTime.now();
    }

    public void changePassword(String hashedPassword) {
        this.password = hashedPassword;
        this.updatedAt = LocalDateTime.now();
    }

    public void attachProfileImage(String imageUrl) {
        this.profileImageUrl = imageUrl;
        this.updatedAt = LocalDateTime.now();
    }
}
