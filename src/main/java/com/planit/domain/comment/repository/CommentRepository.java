package com.planit.domain.comment.repository;

import com.planit.domain.comment.entity.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
        select c
        from Comment c
        join fetch c.author
        where c.post.id = :postId
          and c.deletedAt is null
        order by c.createdAt asc
    """)
    List<Comment> findAllByPostIdAndDeletedAtIsNullOrderByCreatedAtAsc(
        @Param("postId") Long postId
    );

    @Query("""
        select new com.planit.domain.comment.dto.CommentDetail(
            c.id,
            c.content,
            c.createdAt,
            c.author.id,
            c.author.nickname,
            c.author.profileImageUrl
        )
        from Comment c
        where c.post.id = :postId
          and c.deletedAt is null
        order by c.createdAt asc
    """)
    List<com.planit.domain.comment.dto.CommentDetail> findDetailsByPostId(
        @Param("postId") Long postId
    );

}
