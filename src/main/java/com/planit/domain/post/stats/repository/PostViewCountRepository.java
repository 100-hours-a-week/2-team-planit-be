package com.planit.domain.post.stats.repository;

import com.planit.domain.post.stats.entity.PostViewCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostViewCountRepository extends JpaRepository<PostViewCount, Long> {

    @Modifying
    @Query(
            value = """
                    insert into post_view_count (post_id, view_count, updated_at)
                    values (:postId, 1, now(6))
                    on duplicate key update
                        view_count = view_count + 1,
                        updated_at = now(6)
                    """,
            nativeQuery = true
    )
    void upsertAndIncrease(@Param("postId") Long postId);

    @Modifying
    @Query(
            value = """
                    insert into post_view_count (post_id, view_count, updated_at)
                    values (:postId, :count, now(6))
                    on duplicate key update
                        view_count = :count,
                        updated_at = now(6)
                    """,
            nativeQuery = true
    )
    void upsertAndSet(@Param("postId") Long postId, @Param("count") long count);
}
