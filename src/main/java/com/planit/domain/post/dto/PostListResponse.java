package com.planit.domain.post.dto; // 게시글 목록 응답 DTO

import java.util.List;
import lombok.Getter;

@Getter
public class PostListResponse {
    private final List<PostSummaryResponse> posts; // 실제 카드 표현을 위한 게시글 모음
    private final boolean hasMore; // 무한 스크롤 여부 (다음 페이지 존재 여부)

    public PostListResponse(List<PostSummaryResponse> posts, boolean hasMore) {
        this.posts = posts;
        this.hasMore = hasMore;
    }
}
