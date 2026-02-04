package com.planit.domain.post.dto; // 게시글 수정 요청 DTO 패키지

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * 게시글 수정 요청 DTO
 * - 제목/내용/이미지까지 게시글 수정 시 제출되는 form-data 필드를 정의
 * - posts/users 테이블 기준으로 validation 포함
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

    @Parameter(description = "추가/교체할 이미지 최대 5장(jpg/png/webp)")
    @Size(max = 5, message = "*이미지는 최대 5장까지 업로드 가능합니다.")
    private List<MultipartFile> images;

    public List<MultipartFile> getImages() {
        return images == null ? Collections.emptyList() : images;
    }
}
