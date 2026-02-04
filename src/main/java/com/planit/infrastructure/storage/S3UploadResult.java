package com.planit.infrastructure.storage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class S3UploadResult {
    private final String key;
    private final String url;
}
