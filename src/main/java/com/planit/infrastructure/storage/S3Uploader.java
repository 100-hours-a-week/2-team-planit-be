package com.planit.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@ConditionalOnProperty(prefix = "cloud.aws", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class S3Uploader {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    public String uploadProfileImage(MultipartFile file, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*프로필 이미지를 선택해주세요.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*프로필 이미지는 5MB 이하만 업로드 가능합니다.");
        }
        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(original);
        if (!StringUtils.hasText(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*지원하지 않는 이미지 형식입니다.");
        }
        String normalizedExt = extension.toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(normalizedExt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*jpg/jpeg/png 형식만 업로드할 수 있습니다.");
        }
        String key = String.format("profile/%d/%s.%s", userId, UUID.randomUUID(), normalizedExt);
        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .contentType(StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream")
                .contentLength(file.getSize())
                .build();
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "*이미지 업로드 중 오류가 발생했습니다.");
        }
        return buildPublicUrl(key);
    }

    private String buildPublicUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }
}
