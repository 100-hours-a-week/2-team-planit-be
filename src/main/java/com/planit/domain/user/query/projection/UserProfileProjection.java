package com.planit.domain.user.query.projection;

public interface UserProfileProjection {
    Long getUserId();
    String getLoginId();
    String getNickname();
    String getProfileImageKey();
}
