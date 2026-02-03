package com.planit.infrastructure.storage;

import com.planit.infrastructure.storage.dto.PresignedUrlResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * S3 Presigned URL 발급 서비스.
 * - profile/, post/ 폴더별로 클라이언트 직접 업로드용 URL 생성
 */
@Service
@ConditionalOnProperty(prefix = "cloud.aws", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class S3PresignedUrlService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Duration URL_EXPIRY = Duration.ofMinutes(10);

    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * 프로필 이미지용 Presigned PUT URL 발급
     *
     * @param userId   사용자 ID (폴더 경로에 사용)
     * @param fileExt  확장자 (jpg, png 등)
     * @param mimeType Content-Type
     */
    public PresignedUrlResponse createProfilePresignedUrl(Long userId, String fileExt, String mimeType) {
        validateExtension(fileExt);
        String key = buildKey("profile", userId, fileExt);
        return createPresignedUrl(key, mimeType);
    }

    /**
     * 게시물 이미지용 Presigned PUT URL 발급
     *
     * @param userId   사용자 ID (폴더 경로에 사용)
     * @param fileExt  확장자
     * @param mimeType Content-Type
     */
    public PresignedUrlResponse createPostPresignedUrl(Long userId, String fileExt, String mimeType) {
        validateExtension(fileExt);
        String key = buildKey("post", userId, fileExt);
        return createPresignedUrl(key, mimeType);
    }

    private void validateExtension(String fileExt) {
        if (!StringUtils.hasText(fileExt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*지원하지 않는 이미지 형식입니다.");
        }
        String normalized = fileExt.toLowerCase().replaceFirst("^\\.", "");
        if (!ALLOWED_EXTENSIONS.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "*jpg/jpeg/png/webp 형식만 업로드할 수 있습니다.");
        }
    }

    private String buildKey(String folder, Long userId, String fileExt) {
        String ext = fileExt.toLowerCase().replaceFirst("^\\.", "");
        return String.format("%s/%d/%s.%s", folder, userId, UUID.randomUUID(), ext);
    }

    private PresignedUrlResponse createPresignedUrl(String key, String mimeType) {
        S3Presigner presigner = this.s3Presigner;
        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(StringUtils.hasText(mimeType) ? mimeType : "image/jpeg")
            .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(URL_EXPIRY)
            .putObjectRequest(putRequest)
            .build();
        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
        Instant expiresAt = Instant.now().plus(URL_EXPIRY);
        return new PresignedUrlResponse(presigned.url().toString(), key, expiresAt);
    }
}
