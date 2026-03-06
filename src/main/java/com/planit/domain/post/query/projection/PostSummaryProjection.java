package com.planit.domain.post.query.projection;

import com.planit.domain.post.entity.BoardType;
import java.time.LocalDateTime;

public interface PostSummaryProjection {
    Long getPostId();
    String getTitle();
    Long getAuthorId();
    String getAuthorNickname();
    String getAuthorProfileImageKey();
    LocalDateTime getCreatedAt();
    Long getLikeCount();
    Long getCommentCount();
    Long getViewCount();
    Long getRepresentativeImageId();
    String getRepresentativeImageKey();
    Double getRankingScore();
    String getPlaceImageUrl();
    String getGooglePlaceId();
    String getPlaceName();
    String getTripTitle();
    BoardType getBoardType();
}
