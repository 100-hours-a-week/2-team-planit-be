package com.planit.domain.post.dto; // 게시글 생성 요청 DTO 패키지

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank; // 빈 문자열 검사
import jakarta.validation.constraints.Size;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 게시글 작성 요청 DTO
 * - posts/users/posted_images/images/post_places/places 조합을 고려한 입력 필드
 * - 자유게시판 화면(제목/본문/이미지 5장) 스펙을 반영
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostCreateRequest {

    @Parameter(description = "제목(최대 24자)")
    @NotBlank(message = "*제목을 입력해주세요.")
    @Size(max = 24, message = "*제목은 최대 24자까지 작성 가능합니다.")
    private String title; // 사용자 입력 제목

    @Parameter(description = "본문(최대 2,000자)")
    @NotBlank(message = "*내용을 입력해주세요.")
    @Size(max = 2000, message = "*내용은 최대 2,000자까지 작성할 수 있습니다.")
    private String content; // 게시글 본문

    private List<Long> placeIds; // 추후 place 연계용 ID 리스트 (현재 optional)

    /** Presigned URL 업로드 완료 후 S3 key 목록 (최대 5개) */
    @Size(max = 5, message = "*이미지는 최대 5장까지 업로드 가능합니다.")
    private List<String> imageKeys;

    public List<String> getImageKeys() {
        return imageKeys == null ? Collections.emptyList() : imageKeys;
    }
}
