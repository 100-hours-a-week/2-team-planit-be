package com.planit.domain.comment.query.projection;

import java.time.LocalDateTime;

public interface CommentSummaryProjection {
    Long getCommentId();
    String getAuthorNickname();
    String getAuthorProfileImageKey();
    String getContent();
    LocalDateTime getCreatedAt();
}
