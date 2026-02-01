package com.planit.domain.like.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class PostLikeResponse {

    private final Long postId;
    private final long likeCount;
    private final boolean likedByMe;

    public static PostLikeResponse of(Long postId, long likeCount, boolean likedByMe) {
        return new PostLikeResponse(postId, likeCount, likedByMe);
    }
}
