package com.planit.domain.post.query.projection;

import com.planit.domain.post.entity.BoardType;
import java.time.LocalDateTime;

public interface PostDetailProjection {
    Long getPostId();
    String getTitle();
    String getContent();
    BoardType getBoardType();
    LocalDateTime getCreatedAt();
    Long getAuthorId();
    String getAuthorNickname();
    String getAuthorProfileImageKey();
    Long getLikeCount();
    Long getCommentCount();
    Long getViewCount();
    Integer getLikedByRequester();
    Long getPlanTripId();
    String getTripTitle();
    String getPlanThumbnailKey();
    String getPlaceName();
    String getGooglePlaceId();
    String getPlaceCity();
    String getPlaceCountry();
    Integer getPlaceRating();
}
