package com.planit.domain.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KeywordNotificationRequest {

    @NotNull(message = "*게시물을 지정해주세요.")
    @Positive(message = "*게시물 ID는 양수여야 합니다.")
    private Long postId;

    @NotBlank(message = "*본문 요약을 입력해주세요.")
    @Size(max = 255, message = "*요약은 최대 255자까지 작성 가능합니다.")
    private String previewText;

    @Size(max = 50, message = "*행위자 이름은 최대 50자까지 가능합니다.")
    private String actorName;
}
