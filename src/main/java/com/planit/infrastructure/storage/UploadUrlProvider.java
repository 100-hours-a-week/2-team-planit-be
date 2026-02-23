package com.planit.infrastructure.storage;

import com.planit.infrastructure.storage.dto.PresignedUrlResponse;

public interface UploadUrlProvider {
    PresignedUrlResponse issuePresignedPutUrl(UploadContext context);

    void deleteByKey(String key);
}
