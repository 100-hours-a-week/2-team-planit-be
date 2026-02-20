package com.planit.infrastructure.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "storage", name = "mode", havingValue = "stub")
public class StubUploadStorage {
    private final StorageProperties storageProperties;

    public void save(String key, InputStream body) {
        if (!StringUtils.hasText(key)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*이미지 key가 비어있습니다.");
        }
        Path target = resolvePath(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(body, target);
            System.out.println("stub모드 이미지업로드 완료, key: "+key);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "*stub 업로드 저장에 실패했습니다.");
        }
    }

    public void delete(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        Path target = resolvePath(key);
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "*stub 업로드 삭제에 실패했습니다.");
        }
    }

    public Resource loadAsResource(String key) {
        if (!StringUtils.hasText(key)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "*파일을 찾을 수 없습니다.");
        }
        Path target = resolvePath(key);
        try {
            if (!Files.exists(target)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "*파일을 찾을 수 없습니다.");
            }
            return new UrlResource(target.toUri());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "*파일 로딩에 실패했습니다.");
        }
    }

    private Path resolvePath(String key) {
        Path base = Paths.get(storageProperties.getStubUploadDir()).toAbsolutePath().normalize();
        Path target = base.resolve(key).normalize();
        if (!target.startsWith(base)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*유효하지 않은 이미지 key입니다.");
        }
        return target;
    }
}
