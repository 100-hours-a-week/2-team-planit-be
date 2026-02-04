package com.planit.domain.common.repository;

import com.planit.domain.common.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 이미지 메타(entity/common/entity/Image)를 CRUD/조회할 수 있는 JPA Repository
 * - Image 엔티티는 uploaded 파일의 fileName/fileSize/createdAt을 저장하며 auto increment PK를 사용합니다.
 * - JpaRepository를 상속해서 기본적인 저장/조회/삭제 메서드를 그대로 이용합니다.
 */
public interface ImageRepository extends JpaRepository<Image, Long> {
}
