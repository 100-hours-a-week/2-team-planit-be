package com.planit.domain.post.dto; // 게시글 목록 응답 DTO

import java.util.List;
import lombok.Getter;

@Getter
public class PostListResponse {
    private final List<PostSummaryResponse> posts; // 게시글 카드로 사용할 리스트 (여러 테이블 조합 결과)
    private final boolean hasMore; // 무한 스크롤을 위한 flag, 다음 페이지 데이터 존재 여부

    public PostListResponse(List<PostSummaryResponse> posts, boolean hasMore) {
        this.posts = posts;
        this.hasMore = hasMore;
    }
}
