package com.planit.infrastructure.storage;

import com.planit.domain.user.config.ProfileImageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;

@Component
@RequiredArgsConstructor
public class S3ImageUrlResolver {

    private final ObjectProvider<S3Client> s3ClientProvider;
    private final ProfileImageProperties imageProperties;

    @Value("${cloud.aws.s3.bucket:planit-s3-bucket}")
    private String bucket;

    public String resolve(String key) {
        if (!StringUtils.hasText(key)) {
            return imageProperties.getDefaultImageUrl();
        }
        S3Client s3Client = s3ClientProvider.getIfAvailable();
        if (s3Client == null) {
            return imageProperties.getDefaultImageUrl();
        }
        GetUrlRequest request = GetUrlRequest.builder()
            .bucket(bucket)
            .key(key)
            .build();
        return s3Client.utilities().getUrl(request).toExternalForm();
    }
}
