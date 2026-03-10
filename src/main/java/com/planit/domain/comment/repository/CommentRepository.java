package com.planit.domain.comment.repository;

import com.planit.domain.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query(value = """
        select
            c.comment_id as commentId,
            c.content as content,
            c.created_at as createdAt,
            c.author_id as authorId,
            u.nickname as authorNickname,
            u.profile_image_key as authorProfileImageKey
        from comments c
        join users u on u.user_id = c.author_id and u.is_deleted = 0
        where c.post_id = :postId
          and c.deleted_at is null
    """,
            countQuery = """
        select count(*)
        from comments c
        where c.post_id = :postId
          and c.deleted_at is null
    """,
            nativeQuery = true)
    Page<CommentProjection> findDetailsPageByPostId(
            @Param("postId") Long postId,
            Pageable pageable
    );

    interface CommentProjection {
        Long getCommentId();
        String getContent();
        java.time.LocalDateTime getCreatedAt();
        Long getAuthorId();
        String getAuthorNickname();
        String getAuthorProfileImageKey();
    }

    @Modifying
    @Query("UPDATE Comment c SET c.deletedAt = :deletedAt WHERE c.id = :commentId AND c.deletedAt IS NULL")
    int markAsDeleted(
        @Param("commentId") Long commentId,
        @Param("deletedAt") java.time.LocalDateTime deletedAt
    );

}
