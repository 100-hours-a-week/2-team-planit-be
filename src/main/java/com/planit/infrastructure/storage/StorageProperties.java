package com.planit.infrastructure.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {
    /**
     * storage.mode = stub | s3
     */
    private String mode = "s3";

    /**
     * Stub 업로드/서빙에 사용할 base URL (예: http://localhost:8080/api)
     */
    private String stubBaseUrl = "http://localhost:8080/api";

    /**
     * Stub 업로드 파일 저장 경로
     */
    private String stubUploadDir = "./tmp/uploads";
}
