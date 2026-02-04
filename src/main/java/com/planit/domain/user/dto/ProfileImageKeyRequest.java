package com.planit.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 프로필 이미지 key 저장 요청 (Presigned URL로 업로드 완료 후) */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileImageKeyRequest {

    @NotBlank(message = "*이미지 key를 입력해주세요.")
    private String key;
}