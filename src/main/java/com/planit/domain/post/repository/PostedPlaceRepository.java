package com.planit.domain.post.repository;

import com.planit.domain.post.entity.PostedPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * posted_places 테이블 관리용 리포지토리
 */
public interface PostedPlaceRepository extends JpaRepository<PostedPlace, Long> {

    /**
     * 게시글에 연결된 장소 삭제
     */
    @Modifying
    @Query("delete from PostedPlace p where p.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);
}
