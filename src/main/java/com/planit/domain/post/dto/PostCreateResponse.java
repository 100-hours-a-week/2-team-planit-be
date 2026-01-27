package com.planit.domain.post.dto; // 작성 응답 DTO

import com.planit.domain.post.entity.BoardType; // 게시판 타입
import java.time.LocalDateTime; // 작성 시각
import java.util.List; // 업로드 이미지 ID 리스트
import lombok.Getter;

@Getter
public class PostCreateResponse {
    private final Long postId; // 생성된 게시글 PK
    private final BoardType boardType; // 선택한 게시판 (v1: FREE)
    private final String title; // 저장된 제목
    private final String content; // 저장된 본문
    private final LocalDateTime createdAt; // 서버 기준 생성 시각
    private final Long userId; // 작성자 PK (author)
    private final List<Long> imageIds; // 생성된 이미지 메타 ID 목록

    public PostCreateResponse(Long postId,
                              BoardType boardType,
                              String title,
                              String content,
                              LocalDateTime createdAt,
                              Long userId,
                              List<Long> imageIds) {
        this.postId = postId;
        this.boardType = boardType;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.userId = userId;
        this.imageIds = imageIds;
    }
}
