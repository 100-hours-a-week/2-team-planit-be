package com.planit.domain.comment.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 댓글 응답 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class CommentResponse {
    private Long commentId;
    private String authorNickname;
    private Long authorProfileImageId;
    private String content;
    private String createdAt; // hh:mm / yyyy-MM-dd 형식
}
