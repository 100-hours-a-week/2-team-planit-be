package com.planit.domain.user.entity; // 사용자 엔티티를 정의하는 패키지

import jakarta.persistence.Column; // 컬럼 속성 정의
import jakarta.persistence.Entity; // JPA 엔티티 선언
import jakarta.persistence.GeneratedValue; // 자동 생성 전략
import jakarta.persistence.GenerationType; // ID 생성 타입
import jakarta.persistence.Id; // PK 지정
import jakarta.persistence.Table; // 테이블 매핑
import java.time.LocalDateTime; // 생성/수정 시간 표현
import lombok.Getter; // getter 자동 생성
import lombok.NoArgsConstructor; // 기본 생성자 자동 생성
import lombok.Setter; // setter 자동 생성

@Entity // JPA 엔티티로 관리됨
@Table(name = "users") // users 테이블과 매핑
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id; // 자동 생성되는 사용자 PK

    @Column(name = "login_id", nullable = false, length = 20, unique = true)
    private String loginId; // 로그인 ID (고유)

    @Column(nullable = false, length = 255)
    private String password; // 암호화된 비밀번호

    @Column(nullable = false, length = 10, unique = true)
    private String nickname; // 고유 닉네임

    @Column(columnDefinition = "json")
    private String preferences; // JSON 형태의 사용자 설정

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted; // soft-delete flag

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // 삭제된 시간 (soft delete)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt; // 생성 시간

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 마지막 수정 시간
}
