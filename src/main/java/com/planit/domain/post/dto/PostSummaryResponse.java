package com.planit.domain.post.dto; // 게시글 카드 응답 DTO 정리

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class PostSummaryResponse {
    private final Long postId;
    private final String title;
    private final Long authorId;
    private final String authorNickname;
    private final Long authorProfileImageId;
    private final LocalDateTime createdAt;
    private final Long likeCount;
    private final Long commentCount;
    private final Long representativeImageId;
    private final Double rankingScore;
    private final String placeName;
    private final String tripTitle;

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
        Double rankingScore,
        String placeName,
        String tripTitle
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
        this.placeName = placeName;
        this.tripTitle = tripTitle;
    }
}
