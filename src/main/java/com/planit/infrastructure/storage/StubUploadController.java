package com.planit.infrastructure.storage;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "storage", name = "mode", havingValue = "stub")
public class StubUploadController {
    private final StubUploadStorage stubUploadStorage;

    @PutMapping("/stub-upload")
    public ResponseEntity<Void> upload(@RequestParam String key, HttpServletRequest request) throws IOException {
        String decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8);
        stubUploadStorage.save(decodedKey, request.getInputStream());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stub-files/**")
    public ResponseEntity<Resource> download(HttpServletRequest request) {
        String rawPath = request.getRequestURI();
        String prefix = "/stub-files/";
        int index = rawPath.indexOf(prefix);
        String key = index >= 0 ? rawPath.substring(index + prefix.length()) : "";
        String decodedKey = URLDecoder.decode(key, StandardCharsets.UTF_8);
        Resource resource = stubUploadStorage.loadAsResource(decodedKey);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
