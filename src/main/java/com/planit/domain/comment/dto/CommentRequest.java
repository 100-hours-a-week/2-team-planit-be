package com.planit.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 게시글 상세 조회에서 새로운 댓글 등록 요청 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
public class CommentRequest {
    @NotBlank(message = "*댓글을 입력해주세요.")
    private String content;
}

