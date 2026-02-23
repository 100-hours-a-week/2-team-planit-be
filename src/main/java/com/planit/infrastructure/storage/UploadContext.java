package com.planit.infrastructure.storage;

import lombok.Getter;

@Getter
public class UploadContext {
    private final UploadDomain domain;
    private final Long userId;
    private final String fileExtension;
    private final String contentType;

    private UploadContext(UploadDomain domain, Long userId, String fileExtension, String contentType) {
        this.domain = domain;
        this.userId = userId;
        this.fileExtension = fileExtension;
        this.contentType = contentType;
    }

    public static UploadContext signupProfile(String fileExtension, String contentType) {
        return new UploadContext(UploadDomain.SIGNUP_PROFILE, null, fileExtension, contentType);
    }

    public static UploadContext profile(Long userId, String fileExtension, String contentType) {
        return new UploadContext(UploadDomain.PROFILE, userId, fileExtension, contentType);
    }

    public static UploadContext post(Long userId, String fileExtension, String contentType) {
        return new UploadContext(UploadDomain.POST, userId, fileExtension, contentType);
    }
}
