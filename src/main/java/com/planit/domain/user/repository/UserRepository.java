package com.planit.domain.user.repository; // 사용자 도메인 레포지토리 패키지

import com.planit.domain.user.entity.User; // User 엔티티
import java.util.Optional; // Optional 반환 타입
import org.springframework.data.jpa.repository.JpaRepository; // JpaRepository 상속
import org.springframework.data.jpa.repository.Query; // 커스텀 쿼리 정의
import org.springframework.data.repository.query.Param; // 쿼리 파라미터 바인딩

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginIdAndDeletedFalse(String loginId); // ID+삭제 flag 조건

    Optional<User> findByNicknameAndDeletedFalse(String nickname); // 닉네임+삭제 flag

    boolean existsByLoginIdAndDeletedFalse(String loginId); // ID 중복 확인

    boolean existsByNicknameAndDeletedFalse(String nickname); // 닉네임 중복 확인

    @Query(value = "select count(1) > 0 from user_image where user_id = :userId", nativeQuery = true)
    boolean existsProfileImageByUserId(@Param("userId") Long userId); // 프로필 이미지 존재 확인
}
