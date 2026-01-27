package com.planit.domain.like.repository;

import com.planit.domain.like.entity.Like;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<Like, Long> {

    boolean existsByPostIdAndAuthorId(Long postId, Long authorId);

    Optional<Like> findByPostIdAndAuthorId(Long postId, Long authorId);

    long countByPostId(Long postId);
}
