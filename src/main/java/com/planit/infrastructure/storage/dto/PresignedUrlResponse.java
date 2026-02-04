package com.planit.infrastructure.storage.dto;

import java.time.Instant;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Presigned URL 발급 응답 */
@Getter
@RequiredArgsConstructor
public class PresignedUrlResponse {
    private final String uploadUrl;
    private final String key;
    private final Instant expiresAt;
}