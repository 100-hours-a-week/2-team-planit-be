package com.planit.infrastructure.storage;

import com.planit.infrastructure.storage.dto.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "storage", name = "mode", havingValue = "s3")
public class S3UploadUrlProvider implements UploadUrlProvider {
    private final S3PresignedUrlService presignedUrlService;
    private final ObjectProvider<S3ObjectDeleter> s3ObjectDeleterProvider;

    @Override
    public PresignedUrlResponse issuePresignedPutUrl(UploadContext context) {
        if (context == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*업로드 요청이 비어있습니다.");
        }
        return switch (context.getDomain()) {
            case SIGNUP_PROFILE -> presignedUrlService.createSignupPresignedUrl(
                    context.getFileExtension(),
                    safeContentType(context.getContentType())
            );
            case PROFILE -> presignedUrlService.createProfilePresignedUrl(
                    requireUserId(context),
                    context.getFileExtension(),
                    safeContentType(context.getContentType())
            );
            case POST -> presignedUrlService.createPostPresignedUrl(
                    requireUserId(context),
                    context.getFileExtension(),
                    safeContentType(context.getContentType())
            );
        };
    }

    @Override
    public void deleteByKey(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        S3ObjectDeleter deleter = s3ObjectDeleterProvider.getIfAvailable();
        if (deleter == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "*이미지 삭제 기능이 비활성화 되어 있습니다.");
        }
        deleter.delete(key);
    }

    private Long requireUserId(UploadContext context) {
        if (context.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*사용자 정보가 필요합니다.");
        }
        return context.getUserId();
    }

    private String safeContentType(String contentType) {
        return StringUtils.hasText(contentType) ? contentType : "image/jpeg";
    }
}
