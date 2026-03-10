package com.planit.domain.comment.query.repository;

import com.planit.domain.comment.entity.Comment;
import com.planit.domain.comment.query.projection.CommentSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CommentQueryRepository extends Repository<Comment, Long> {

    @Query(
            value = """
                    select c.comment_id as commentId,
                           u.nickname as authorNickname,
                           u.profile_image_key as authorProfileImageKey,
                           c.content as content,
                           c.created_at as createdAt
                    from comments c
                    join users u on u.user_id = c.author_id and u.is_deleted = 0
                    join posts p on p.post_id = c.post_id and p.is_deleted = 0
                    where c.post_id = :postId
                      and c.deleted_at is null
                    order by c.created_at asc
                    """,
            countQuery = """
                    select count(*)
                    from comments c
                    join posts p on p.post_id = c.post_id and p.is_deleted = 0
                    where c.post_id = :postId
                      and c.deleted_at is null
                    """,
            nativeQuery = true
    )
    Page<CommentSummaryProjection> findCommentSummariesByPostId(@Param("postId") Long postId, Pageable pageable);

    @Query(
            value = """
                    select count(*)
                    from posts p
                    where p.post_id = :postId
                      and p.is_deleted = 0
                    """,
            nativeQuery = true
    )
    long countActivePost(@Param("postId") Long postId);
}
