package com.planit.infrastructure.storage;

import com.planit.domain.user.config.ProfileImageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class S3ImageUrlResolver {

    private final ProfileImageProperties imageProperties;
    private final StorageProperties storageProperties;

    @Value("${planit.cloudfront.domain:}")
    private String cloudfrontDomain;

    public String resolve(String key) {
        if (!StringUtils.hasText(key)) {
            return imageProperties.getDefaultImageUrl();
        }
        return resolveOrNull(key);
    }

    /** key가 없으면 null 반환 (게시물 이미지 등 profile이 아닌 경우) */
    public String resolveOrNull(String key) {
        if (!StringUtils.hasText(key)) {
            return null;
        }
        if ("stub".equalsIgnoreCase(storageProperties.getMode())) {
            String stubBaseUrl = storageProperties.getStubBaseUrl();
            if (StringUtils.hasText(stubBaseUrl)) {
                String base = stubBaseUrl.trim();
                if (base.endsWith("/")) {
                    base = base.substring(0, base.length() - 1);
                }
                String path = key.startsWith("/") ? key.substring(1) : key;
                return base + "/stub-files/" + path;
            }
            return null;
        }
        if (!StringUtils.hasText(cloudfrontDomain)) {
            return null;
        }
        String base = cloudfrontDomain.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String path = key.startsWith("/") ? key : "/" + key;
        return "https://" + base + path;
    }
}
