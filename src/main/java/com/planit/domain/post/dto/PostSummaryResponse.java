package com.planit.domain.post.dto; // 게시글 카드 응답 DTO 정리

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class PostSummaryResponse {
    private final Long postId; // 게시글 PK
    private final String title; // 게시글 제목
    private final Long authorId; // 작성자 PK
    private final String authorNickname; // 작성자 닉네임
    private final Long authorProfileImageId; // 작성자 프로필 이미지 ID
    private final LocalDateTime createdAt; // 작성 시점
    private final Long likeCount; // 좋아요 집계
    private final Long commentCount; // 댓글 집계
    private final Long representativeImageId; // 대표 이미지 ID
    private final Double rankingScore; // 랭킹 스냅샷 기반 순위 지표

    public PostSummaryResponse(
        Long postId,
        String title,
        Long authorId,
        String authorNickname,
        Long authorProfileImageId,
        LocalDateTime createdAt,
        Long likeCount,
        Long commentCount,
        Long representativeImageId,
        Double rankingScore
    ) {
        this.postId = postId;
        this.title = title;
        this.authorId = authorId;
        this.authorNickname = authorNickname;
        this.authorProfileImageId = authorProfileImageId;
        this.createdAt = createdAt;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.representativeImageId = representativeImageId;
        this.rankingScore = rankingScore;
    }
}
