package com.planit.domain.post.dto; // 게시글 상세 응답 DTO 패키지

import java.time.LocalDateTime; // 생성/작성 시간 표현
import java.util.Collections; // null 보호용 빈 리스트 반환
import java.util.List; // 이미지/댓글 컬렉션
import lombok.Getter; // Lombok getter 자동 생성

@Getter
public class PostDetailResponse {
    private final Long postId; // 게시글 PK
    private final String boardName; // 게시판 이름(예: 자유게시판)
    private final String boardDescription; // 게시판 설명
    private final String title; // 게시글 제목
    private final String content; // 게시글 본문 (최대 1,000자)
    private final LocalDateTime createdAt; // 작성 시각
    private final AuthorInfo author; // 작성자 정보
    private final List<PostImage> images; // 최대 5장 사진
    private final Integer likeCount; // 총 좋아요 수
    private final Integer commentCount; // 총 댓글 수
    private final Boolean likedByRequester; // 현재 요청자가 좋아요 눌렀는지
    private final List<CommentInfo> comments; // 댓글 목록 (20개)
    private final Boolean editable; // 현재 요청자가 작성자인지 (수정/삭제 버튼 노출 판단)

    public PostDetailResponse(Long postId,
                              String boardName,
                              String boardDescription,
                              String title,
                              String content,
                              LocalDateTime createdAt,
                              AuthorInfo author,
                              List<PostImage> images,
                              Integer likeCount,
                              Integer commentCount,
                              Boolean likedByRequester,
                              List<CommentInfo> comments,
                              Boolean editable) {
        this.postId = postId;
        this.boardName = boardName;
        this.boardDescription = boardDescription;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.author = author;
        this.images = images == null ? Collections.emptyList() : images;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.likedByRequester = likedByRequester;
        this.comments = comments == null ? Collections.emptyList() : comments;
        this.editable = editable;
    }

    @Getter
    public static class AuthorInfo {
        private final Long authorId; // 작성자 PK
        private final String nickname; // 작성자 닉네임
        private final Long profileImageId; // 프로필 이미지 (nullable)

        public AuthorInfo(Long authorId, String nickname, Long profileImageId) {
            this.authorId = authorId;
            this.nickname = nickname;
            this.profileImageId = profileImageId;
        }
    }

    @Getter
    public static class PostImage {
        private final Long imageId; // 게시글 이미지 ID

        public PostImage(Long imageId) {
            this.imageId = imageId;
        }
    }

    @Getter
    public static class CommentInfo {
        private final Long commentId; // 댓글 PK
        private final Long authorId; // 댓글 작성자 PK
        private final String authorNickname; // 댓글 작성자 닉네임
        private final Long authorProfileImageId; // 댓글 작성자 프로필 이미지
        private final String content; // 댓글 본문 (max 500자)
        private final LocalDateTime createdAt; // 댓글 작성 시간
        private final Boolean deletable; // 현재 요청자가 댓글 작성자인지 (삭제 버튼)

        public CommentInfo(Long commentId,
                           Long authorId,
                           String authorNickname,
                           Long authorProfileImageId,
                           String content,
                           LocalDateTime createdAt,
                           Boolean deletable) {
            this.commentId = commentId;
            this.authorId = authorId;
            this.authorNickname = authorNickname;
            this.authorProfileImageId = authorProfileImageId;
            this.content = content;
            this.createdAt = createdAt;
            this.deletable = deletable;
        }
    }
}
