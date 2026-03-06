package com.planit.domain.post.repository;

import com.planit.domain.post.entity.Post;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository
        extends JpaRepository<Post, Long> {

    /**
     * 내 게시글 조회
     */
    List<Post> findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(
            Long authorId,
            Pageable pageable
    );

    java.util.Optional<Post> findByIdAndDeletedFalse(Long postId);

}
