package com.planit.domain.post.dto;

import java.time.LocalDateTime;

public interface PostSummaryProjection {
    Long getPostId();
    String getTitle();
    Long getAuthorId();
    String getAuthorNickname();
    String getAuthorProfileImageKey();
    Long getRepresentativeImageId();
    String getRepresentativeImageKey();
    String getPlaceName();
    String getPlaceImageUrl();
    Long getCommentCount();
    Long getCommentCount1Year();
    Long getLikeCount();
    Long getLikeCount1Year();
    Boolean getLikedByLoginUser();
    LocalDateTime getCreatedAt();
}
