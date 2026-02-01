package com.planit.domain.user.entity; // 유저 도메인 엔티티 모음 패키지

import jakarta.persistence.Column; // JPA 컬럼 설정
import jakarta.persistence.Entity; // 엔티티 선언
import jakarta.persistence.GeneratedValue; // 자동 증가 ID
import jakarta.persistence.GenerationType; // ID 전략
import jakarta.persistence.Id; // PK 선언
import jakarta.persistence.Table; // 테이블명 지정
import lombok.Getter; // getter 자동 생성
import lombok.NoArgsConstructor; // 기본 생성자 자동 생성
import lombok.Setter; // setter 자동 생성

@Entity // JPA 엔티티로 관리
@Table(name = "user_image") // user_image 테이블 매핑
@Getter
@Setter
@NoArgsConstructor
public class UserProfileImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // user_image 테이블 PK

    @Column(name = "user_id", nullable = false)
    private Long userId; // 참조하는 users 테이블의 PK

    @Column(name = "image_id", nullable = false)
    private Long imageId; // 프로필 이미지 식별자
}
