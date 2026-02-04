package com.planit.domain.post.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 게시글 수정 요청 DTO
 * - Presigned URL 업로드 완료 후 imageKeys로 교체
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostUpdateRequest {

    @Parameter(description = "변경할 제목(최대 24자)")
    @NotBlank(message = "*제목을 입력해주세요.")
    @Size(max = 24, message = "*제목은 최대 24자까지 작성 가능합니다.")
    private String title;

    @Parameter(description = "변경할 본문(최대 2000자)")
    @NotBlank(message = "*내용을 입력해주세요.")
    @Size(max = 2000, message = "*내용은 최대 2000자까지 작성할 수 있습니다.")
    private String content;

    @Parameter(description = "Presigned URL 업로드 완료 후 S3 key 목록 (최대 5장, 기존 이미지 교체)")
    @Size(max = 5, message = "*이미지는 최대 5장까지 업로드 가능합니다.")
    private List<String> imageKeys;

    public List<String> getImageKeys() {
        return imageKeys == null ? Collections.emptyList() : imageKeys;
    }
}
