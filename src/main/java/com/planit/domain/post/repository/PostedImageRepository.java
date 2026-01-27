package com.planit.domain.post.repository;

import com.planit.domain.post.entity.PostedImage;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 게시글에 연관된 이미지를 다루는 JpaRepository
 * - PostedImage는 post_id, image_id, is_main_image 정보를 저장한다.
 * - JpaRepository를 통해 기본 저장/조회/삭제 기능을 그대로 재사용.
 */
public interface PostedImageRepository extends JpaRepository<PostedImage, Long> {
}
