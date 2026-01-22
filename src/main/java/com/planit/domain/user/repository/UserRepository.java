package com.planit.domain.user.repository;

import com.planit.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginIdAndDeletedFalse(String loginId);

    Optional<User> findByNicknameAndDeletedFalse(String nickname);

    boolean existsByLoginIdAndDeletedFalse(String loginId);

    boolean existsByNicknameAndDeletedFalse(String nickname);

    @Query(value = "select count(1) > 0 from user_image where user_id = :userId", nativeQuery = true)
    boolean existsProfileImageByUserId(@Param("userId") Long userId);
}
