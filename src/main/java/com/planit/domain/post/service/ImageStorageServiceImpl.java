package com.planit.domain.post.service;

import com.planit.domain.common.entity.Image;
import com.planit.domain.common.repository.ImageRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service // Spring Bean으로 등록
@RequiredArgsConstructor // 생성자 주입을 lombok으로 처리
public class ImageStorageServiceImpl implements ImageStorageService {

    private final ImageRepository imageRepository; // 이미지 메타 저장소

    @Override
    @Transactional // DB 저장 시 트랜잭션
    public Long store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("image file is required");
        }
        LocalDateTime now = LocalDateTime.now(); // 저장 시간
        Image image = new Image(file.getOriginalFilename(), file.getSize(), now);
        return imageRepository.save(image).getId(); // 저장 후 PK 반환
    }
}
