package com.planit.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

/** S3 객체 삭제 (프로필/게시물 이미지) */
@Component
@ConditionalOnProperty(prefix = "cloud.aws", name = "enabled", havingValue = "true")
public class S3ObjectDeleter {

    private final S3Client s3Client;
    private final String bucket;

    public S3ObjectDeleter(S3Client s3Client, @Value("${cloud.aws.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    public void delete(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }
}