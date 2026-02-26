package com.planit.domain.post.repository; // 커스텀 리포지토리 패키지

import com.planit.domain.post.dto.PostDetailResponse; // 상세 DTO
import java.util.List; // 위치 필터 결과
import java.util.Optional; // 결과 Optional로 반환

public interface PostRepositoryCustom {
    /**
     * 게시글 상세 조회: 요청자 ID까지 함께 전달해 좋아요/댓글/삭제 권한 판별
     */
    Optional<PostDetailResponse> findDetailById(Long postId, Long requesterId);

    /**
     * 장소추천 게시판에서 country/city 문자열을 제목/본문에 포함하는 게시글 ID를 조회한다.
     */
    List<Long> findPlaceRecommendationPostIdsByLocation(String country, String city);
}
