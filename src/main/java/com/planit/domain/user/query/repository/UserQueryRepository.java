package com.planit.domain.user.query.repository;

import com.planit.domain.user.entity.User;
import com.planit.domain.user.query.projection.MyPagePostPreviewProjection;
import com.planit.domain.user.query.projection.MyPageSummaryProjection;
import com.planit.domain.user.query.projection.UserProfileProjection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface UserQueryRepository extends Repository<User, Long> {

    @Query(value = """
            select
                u.user_id as userId,
                u.login_id as loginId,
                u.nickname as nickname,
                u.profile_image_key as profileImageKey
            from users u
            where u.login_id = :loginId
              and u.is_deleted = 0
            limit 1
            """, nativeQuery = true)
    Optional<UserProfileProjection> findProfileByLoginId(@Param("loginId") String loginId);

    @Query(value = """
            select
                u.user_id as userId,
                u.login_id as loginId,
                u.nickname as nickname,
                u.profile_image_key as profileImageKey,
                (select count(1) from posts p where p.user_id = u.user_id and p.is_deleted = 0) as postCount,
                (select count(1) from comments c where c.author_id = u.user_id and c.deleted_at is null) as commentCount,
                (select count(1) from likes l where l.author_id = u.user_id) as likeCount,
                (select count(1) from notifications n where n.user_id = u.user_id) as notificationCount
            from users u
            where u.login_id = :loginId
              and u.is_deleted = 0
            limit 1
            """, nativeQuery = true)
    Optional<MyPageSummaryProjection> findMyPageSummaryByLoginId(@Param("loginId") String loginId);

    @Query(value = """
            select
                p.post_id as postId,
                p.title as title,
                p.board_type as boardType
            from posts p
            join users u on u.user_id = p.user_id and u.is_deleted = 0
            where u.login_id = :loginId
              and p.is_deleted = 0
            order by p.created_at desc
            limit :limit
            """, nativeQuery = true)
    List<MyPagePostPreviewProjection> findMyPagePostPreviewsByLoginId(
            @Param("loginId") String loginId,
            @Param("limit") int limit
    );

    @Query(value = """
            select count(*) > 0
            from users u
            where u.login_id = :loginId
              and u.is_deleted = 0
            """, nativeQuery = true)
    boolean existsActiveByLoginId(@Param("loginId") String loginId);

    @Query(value = """
            select count(*) > 0
            from users u
            where u.nickname = :nickname
              and u.is_deleted = 0
            """, nativeQuery = true)
    boolean existsActiveByNickname(@Param("nickname") String nickname);
}
