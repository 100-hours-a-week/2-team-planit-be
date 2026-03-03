package com.planit.domain.post.dto;

import java.time.LocalDateTime;

public interface MyPostSummaryProjection {
    Long getPostId();
    String getTitle();
    String getBoardType();
    LocalDateTime getCreatedAt();
}
