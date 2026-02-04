package com.planit.domain.post.service;

/**
 * 이미지 메타데이터 저장 서비스.
 * - Presigned URL로 S3 업로드 완료 후 s3_key를 저장합니다.
 */
public interface ImageStorageService {
    /**
     * S3 key로 이미지 메타데이터를 저장하고 image_id를 반환합니다.
     *
     * @param s3Key S3 객체 key (post/{userId}/{uuid}.ext)
     * @return 저장된 이미지 ID
     */
    Long storeByS3Key(String s3Key);
}
