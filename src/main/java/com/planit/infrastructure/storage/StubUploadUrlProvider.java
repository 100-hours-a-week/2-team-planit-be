package com.planit.infrastructure.storage;

import com.planit.infrastructure.storage.dto.PresignedUrlResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "storage", name = "mode", havingValue = "stub")
public class StubUploadUrlProvider implements UploadUrlProvider {
    private static final Duration URL_EXPIRY = Duration.ofMinutes(10);

    private final StorageProperties storageProperties;
    private final StubUploadStorage stubUploadStorage;

    @Override
    public PresignedUrlResponse issuePresignedPutUrl(UploadContext context) {
        if (context == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*업로드 요청이 비어있습니다.");
        }
        String key = buildKey(context);
        String baseUrl = normalizeBaseUrl(storageProperties.getStubBaseUrl());
        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
        String uploadUrl = baseUrl + "/stub-upload?key=" + encodedKey;
        Instant expiresAt = Instant.now().plus(URL_EXPIRY);
        return new PresignedUrlResponse(uploadUrl, key, expiresAt);
    }

    @Override
    public void deleteByKey(String key) {
        stubUploadStorage.delete(key);
    }

    private String buildKey(UploadContext context) {
        String ext = normalizeExtension(context.getFileExtension());
        String suffix = "stub/" + UUID.randomUUID() + "." + ext;
        return switch (context.getDomain()) {
            case SIGNUP_PROFILE -> "signup/" + suffix;
            case PROFILE -> "profile/" + requireUserId(context) + "/" + suffix;
            case POST -> "post/" + requireUserId(context) + "/" + suffix;
        };
    }

    private Long requireUserId(UploadContext context) {
        if (context.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*사용자 정보가 필요합니다.");
        }
        return context.getUserId();
    }

    private String normalizeExtension(String fileExtension) {
        if (!StringUtils.hasText(fileExtension)) {
            return "jpg";
        }
        String normalized = fileExtension.toLowerCase(Locale.ROOT).replaceFirst("^\\.", "");
        return normalized.isBlank() ? "jpg" : normalized;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return "http://localhost:8080/api";
        }
        String trimmed = baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
