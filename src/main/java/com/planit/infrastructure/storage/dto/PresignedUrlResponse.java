package com.planit.infrastructure.storage.dto;

import java.time.Instant;
import lombok.Getter;

/** Presigned URL 발급 응답 */
@Getter
public class PresignedUrlResponse {
    private final String uploadUrl;
    private final String key;
    private final Instant expiresAt;

    public PresignedUrlResponse(String uploadUrl, String key, Instant expiresAt) {
        this.uploadUrl = uploadUrl;
        this.key = key;
        this.expiresAt = expiresAt;
    }
}
