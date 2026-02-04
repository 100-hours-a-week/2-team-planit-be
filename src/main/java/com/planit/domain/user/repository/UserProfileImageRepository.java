package com.planit.domain.user.repository; // 사용자 도메인 레포지토리 패키지

import com.planit.domain.user.entity.UserProfileImage; // 프로필 이미지 엔티티
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository; // Spring Data JPA 인터페이스

public interface UserProfileImageRepository extends JpaRepository<UserProfileImage, Long> {
    void deleteByUserId(Long userId); // 삭제 시 사용자 ID 기준으로 기존 이미지 관계 삭제

    boolean existsByUserId(Long userId); // 프론트가 "프로필 이미지 등록 여부" 판단할 수 있도록 존재 여부 반환

    Optional<UserProfileImage> findByUserId(Long userId); // 프로필 정보 응답에 연결된 이미지 ID를 채우기 위한 조회
}
