package com.planit.domain.user.repository; // 사용자 도메인 레포지토리 패키지

import com.planit.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByLoginIdAndDeletedFalse(String loginId);

    Optional<User> findByNicknameAndDeletedFalse(String nickname);

    boolean existsByLoginIdAndDeletedFalse(String loginId);

    boolean existsByNicknameAndDeletedFalse(String nickname);

    @Transactional
    @Modifying
    @Query("update User u set u.deleted = true, u.deletedAt = :when where u.id = :userId")
    int softDelete(@Param("userId") Long userId, @Param("when") LocalDateTime when);

    @Query(value = """
        select
            u.user_id as userId,
            u.login_id as loginId,
            u.nickname as nickname,
                u.profile_image_url as profileImageUrl,
            (select count(1) from posts p where p.user_id = u.user_id and p.is_deleted = 0) as postCount,
            (select count(1) from comments c where c.author_id = u.user_id and c.deleted_at is null) as commentCount,
            (select count(1) from likes l where l.author_id = u.user_id) as likeCount,
            (select count(1) from notifications n where n.user_id = u.user_id) as notificationCount
        from users u
        where u.user_id = :userId and u.is_deleted = 0
    """, nativeQuery = true)
    Optional<ProfileSummary> fetchProfileSummary(@Param("userId") Long userId);

    interface ProfileSummary {
        Long getUserId();
        String getLoginId();
        String getNickname();
        String getProfileImageUrl();
        Long getPostCount();
        Long getCommentCount();
        Long getLikeCount();
        Long getNotificationCount();
    }
}
