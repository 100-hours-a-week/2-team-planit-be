package com.planit.domain.post.dto; // 게시글 목록 응답 DTO

import java.util.List;
import lombok.Getter;

@Getter
public class PostListResponse {
    private final List<PostSummaryResponse> items; // 게시글 카드 리스트
    private final boolean hasNext; // 다음 페이지 존재 여부
    private final int page; // 현재 페이지
    private final int size; // 페이지 사이즈
    private final boolean isEmpty; // 항목 비어있음 여부

    public PostListResponse(List<PostSummaryResponse> items, boolean hasNext, int page, int size) {
        this.items = items;
        this.hasNext = hasNext;
        this.page = page;
        this.size = size;
        this.isEmpty = items.isEmpty();
    }
}
