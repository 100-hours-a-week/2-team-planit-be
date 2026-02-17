package com.planit.domain.post.repository;

import com.planit.domain.post.entity.PostedPlace;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * posted_places 테이블 관리용 리포지토리
 */
public interface PostedPlaceRepository extends JpaRepository<PostedPlace, Long> {
}
