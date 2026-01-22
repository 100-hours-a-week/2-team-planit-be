package com.planit.domain.user.repository; // 사용자 도메인 레포지토리 패키지

import com.planit.domain.user.entity.UserProfileImage; // 프로필 이미지 엔티티
import org.springframework.data.jpa.repository.JpaRepository; // Spring Data JPA 인터페이스

public interface UserProfileImageRepository extends JpaRepository<UserProfileImage, Long> {
    void deleteByUserId(Long userId); // 사용자 ID로 프로필 이미지 제거
    boolean existsByUserId(Long userId); // 프로필 이미지 존재 여부 확인
}
