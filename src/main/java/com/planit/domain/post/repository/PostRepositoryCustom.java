package com.planit.domain.post.repository; // 커스텀 리포지토리 패키지

import com.planit.domain.post.dto.PostDetailResponse; // 상세 DTO
import java.util.Optional; // 결과 Optional로 반환

public interface PostRepositoryCustom {
    /**
     * 게시글 상세 조회: 요청자 ID까지 함께 전달해 좋아요/댓글/삭제 권한 판별
     */
    Optional<PostDetailResponse> findDetailById(Long postId, Long requesterId);
}
