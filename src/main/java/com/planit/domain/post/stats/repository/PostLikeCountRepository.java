package com.planit.domain.post.stats.repository;

import com.planit.domain.post.stats.entity.PostLikeCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostLikeCountRepository extends JpaRepository<PostLikeCount, Long> {

    @Modifying
    @Query(
            value = """
                    insert into post_like_count (post_id, like_count, updated_at)
                    values (:postId, if(:delta < 0, 0, :delta), now(6))
                    on duplicate key update
                        like_count = greatest(0, like_count + :delta),
                        updated_at = now(6)
                    """,
            nativeQuery = true
    )
    void upsertAndAdjust(@Param("postId") Long postId, @Param("delta") long delta);

    @Modifying
    @Query(
            value = """
                    insert into post_like_count (post_id, like_count, updated_at)
                    values (:postId, :count, now(6))
                    on duplicate key update
                        like_count = :count,
                        updated_at = now(6)
                    """,
            nativeQuery = true
    )
    void upsertAndSet(@Param("postId") Long postId, @Param("count") long count);
}
