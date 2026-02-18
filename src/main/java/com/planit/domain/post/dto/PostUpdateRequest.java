package com.planit.domain.post.dto;

import com.planit.domain.post.entity.BoardType;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 게시글 수정 요청 DTO
 * - boardType 별 필수 입력을 담아 자유/일정/장소 모두 대응
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

    @Parameter(description = "게시판 유형")
    @NotNull(message = "*게시판 유형을 선택해주세요.")
    private BoardType boardType;

    @Parameter(description = "PLAN_SHARE일 때 연결할 plan ID")
    private Long planId;

    @Parameter(description = "PLAN_SHARE일 때 연결할 trip ID (레거시)")
    private Long tripId;

    @Parameter(description = "PLACE_RECOMMEND일 때 place ID")
    @Positive(message = "*장소 정보를 정확히 입력해주세요.")
    private Long placeId;

    @Parameter(description = "PLACE_RECOMMEND일 때 place name")
    @Size(max = 255, message = "*장소 이름은 최대 255자까지 입력 가능합니다.")
    private String placeName;

    @Parameter(description = "PLACE_RECOMMEND일 때 Google place ID")
    @Size(max = 255, message = "*Google Place ID는 최대 255자까지 입력 가능합니다.")
    private String googlePlaceId;

    @Parameter(description = "PLACE_RECOMMEND일 때 별점")
    @Min(value = 1, message = "*별점은 1점 이상이어야 합니다.")
    @Max(value = 5, message = "*별점은 5점 이하만 가능합니다.")
    private Integer rating;

    @Parameter(description = "Presigned URL 업로드 완료 후 S3 key 목록 (최대 5장, 기존 이미지 교체)")
    @Size(max = 5, message = "*이미지는 최대 5장까지 업로드 가능합니다.")
    private List<String> imageKeys;

    public List<String> getImageKeys() {
        return imageKeys == null ? Collections.emptyList() : imageKeys;
    }
}
