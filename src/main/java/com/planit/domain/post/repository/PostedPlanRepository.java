package com.planit.domain.post.repository;

import com.planit.domain.post.entity.PostedPlan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * PostedPlan 저장소 정의
 */
public interface PostedPlanRepository extends JpaRepository<PostedPlan, Long> {

    /**
     * 특정 게시글에 연결된 일정 존재 여부
     *
     * @param postId 게시글 ID
     * @return boolean
     */
    boolean existsByPostId(Long postId);

    /**
     * 게시글 ID로 계획 조회
     *
     * @param postId 게시글 ID
     * @return Optional<PostedPlan>
     */
    Optional<PostedPlan> findByPostId(Long postId);

    /**
     * 일정 ID로 공유 여부 확인
     *
     * @param tripId 일정 ID
     * @return boolean
     */
    boolean existsByTripId(Long tripId);
}
