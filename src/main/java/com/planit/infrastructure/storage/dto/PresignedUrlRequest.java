package com.planit.infrastructure.storage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Presigned URL 발급 요청 (프로필/게시물 공통) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PresignedUrlRequest {

    @NotBlank(message = "*파일 확장자를 입력해주세요.")
    @Pattern(regexp = "^(?i)(jpg|jpeg|png|webp)$", message = "*jpg/jpeg/png/webp 형식만 가능합니다.")
    private String fileExtension;

    private String contentType;
}