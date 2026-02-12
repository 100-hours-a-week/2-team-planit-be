package com.planit.domain.post.repository;

import com.planit.domain.post.entity.PostedPlan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * PostedPlan 엔티티에 대한 레포지토리 인터페이스
 */
public interface PostedPlanRepository extends JpaRepository<PostedPlan, Long> {

    /**
     * 특정 게시글 ID에 대한 매핑이 존재하는지 확인
     *
     * @param postId 게시글 ID
     * @return 존재 여부(boolean)
     */
    boolean existsByPostId(Long postId);

    /**
     * 게시글 ID를 통해 연결된 PostedPlan을 조회
     *
     * @param postId 게시글 ID
     * @return Optional로 감싼 PostedPlan
     */
    Optional<PostedPlan> findByPostId(Long postId);
}
