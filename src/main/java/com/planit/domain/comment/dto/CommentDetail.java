package com.planit.domain.comment.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 댓글 상세 정보를 담는 DTO
 */
@Getter
@Setter
@NoArgsConstructor
public class CommentDetail {
    private Long commentId;
    private String content;
    private LocalDateTime createdAt;
    private Long authorId;
    private String authorNickname;
    private String authorProfileImageUrl;

    public CommentDetail(Long commentId, String content, LocalDateTime createdAt, Long authorId,
                         String authorNickname, String authorProfileImageUrl) {
        this.commentId = commentId;
        this.content = content;
        this.createdAt = createdAt;
        this.authorId = authorId;
        this.authorNickname = authorNickname;
            this.authorProfileImageUrl = authorProfileImageUrl;
    }
}
