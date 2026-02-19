package com.planit.domain.comment.dto;

import com.planit.domain.comment.entity.Comment;
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
    private String authorProfileImageUrl;
    private String content;
    private String createdAt; // hh:mm / yyyy-MM-dd 형식

    public static CommentResponse from(Comment comment, String authorProfileImageUrl) {
        CommentResponse response = new CommentResponse();
        response.setCommentId(comment.getId());
        response.setAuthorNickname(comment.getAuthor().getNickname());
        response.setAuthorProfileImageUrl(authorProfileImageUrl);
        response.setContent(comment.getContent());
        response.setCreatedAt(comment.getCreatedAt().toString());
        return response;
    }
}
