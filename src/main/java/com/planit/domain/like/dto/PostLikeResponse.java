package com.planit.domain.like.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostLikeResponse {

    private final Long postId;
    private final long likeCount;
    private final boolean likedByMe;

    public PostLikeResponse(Long postId, long likeCount, boolean likedByMe) {
        this.postId = postId;
        this.likeCount = likeCount;
        this.likedByMe = likedByMe;
    }

    public static PostLikeResponse of(Long postId, long likeCount, boolean likedByMe) {
        return new PostLikeResponse(postId, likeCount, likedByMe);
    }
}
