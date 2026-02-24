package com.planit.domain.post.repository;

import com.planit.domain.post.entity.PostedPlan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
     * 주어진 일정이 다른 게시물에 연결됐는지 확인
     */
    @Query("select count(p) > 0 from PostedPlan p where p.trip.id = :tripId and p.post.id <> :postId")
    boolean existsByTripIdAndPostIdNot(@Param("tripId") Long tripId, @Param("postId") Long postId);

    /**
     * 일정 ID로 공유 여부 확인
     *
     * @param tripId 일정 ID
     * @return boolean
     */
    boolean existsByTripId(Long tripId);

    /**
     * 게시글 ID 목록으로 연결된 trip ID 조회
     *
     * @param postIds 게시글 ID 목록
     * @return List<PostTripIdInfo>
     */
    @Query("select p.post.id as postId, p.trip.id as tripId from PostedPlan p where p.post.id in :postIds")
    List<PostTripIdInfo> findTripIdsByPostIds(@Param("postIds") List<Long> postIds);

    interface PostTripIdInfo {
        Long getPostId();
        Long getTripId();
    }
}
