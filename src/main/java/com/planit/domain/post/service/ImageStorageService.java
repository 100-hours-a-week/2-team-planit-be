package com.planit.domain.post.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 업로드 메타 데이터를 저장하는 서비스 스펙.
 * - MultipartFile을 받아 ImageRepository에 저장한 뒤 생성된 ID를 반환합니다.
 * - 실제 스토리지(로컬/S3 등) 연동이 필요하면 구현체에서 처리하여 metadata ID만 전달합니다.
 */
public interface ImageStorageService {
    /**
     * 업로드된 MultipartFile을 저장하고 생성된 image_id를 반환합니다.
     *
     * @param file 업로드 파일
     * @return 저장된 이미지 ID
     */
    Long store(MultipartFile file);
}
