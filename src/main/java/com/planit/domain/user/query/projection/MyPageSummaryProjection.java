package com.planit.domain.user.query.projection;

public interface MyPageSummaryProjection {
    Long getUserId();
    String getLoginId();
    String getNickname();
    String getProfileImageKey();
    Long getPostCount();
    Long getCommentCount();
    Long getLikeCount();
    Long getNotificationCount();
}
