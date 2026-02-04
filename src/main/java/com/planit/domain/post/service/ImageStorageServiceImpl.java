package com.planit.domain.post.service;

import com.planit.domain.common.entity.Image;
import com.planit.domain.common.repository.ImageRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ImageStorageServiceImpl implements ImageStorageService {

    private final ImageRepository imageRepository;

    @Override
    @Transactional
    public Long storeByS3Key(String s3Key) {
        if (!StringUtils.hasText(s3Key)) {
            throw new IllegalArgumentException("s3Key is required");
        }
        LocalDateTime now = LocalDateTime.now();
        Image image = new Image(s3Key, now);
        return imageRepository.save(image).getId();
    }
}