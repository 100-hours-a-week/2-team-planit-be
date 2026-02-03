package com.planit.domain.comment.service;

import com.planit.domain.comment.dto.CommentDetail;
import com.planit.domain.comment.dto.CommentRequest;
import com.planit.domain.comment.dto.CommentResponse;
import com.planit.domain.comment.entity.Comment;
import com.planit.domain.comment.repository.CommentRepository;
import com.planit.domain.notification.service.NotificationService;
import com.planit.domain.post.entity.Post;
import com.planit.domain.post.repository.PostRepository;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import com.planit.infrastructure.storage.S3ImageUrlResolver;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 관련 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final S3ImageUrlResolver imageUrlResolver;

    @Transactional(readOnly = true)
    public List<CommentDetail> listComments(Long postId) {
        return commentRepository.findDetailsByPostId(postId).stream()
            .map(detail -> new CommentDetail(
                detail.getCommentId(),
                detail.getContent(),
                detail.getCreatedAt(),
                detail.getAuthorId(),
                detail.getAuthorNickname(),
                imageUrlResolver.resolve(detail.getAuthorProfileImageKey())
            ))
            .collect(Collectors.toList());
    }

    @Transactional
    public CommentResponse addComment(Long postId, String loginId, CommentRequest request) {
        Post post = postRepository.findById(postId).orElseThrow();
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId).orElseThrow();
        Comment comment = new Comment();
        comment.setPost(post);
        comment.setAuthor(user);
        comment.setContent(request.getContent());
        comment.setCreatedAt(LocalDateTime.now());
        Comment saved = commentRepository.save(comment);
        postRepository.incrementCommentCount(postId);
        publishCommentNotification(post, user, request.getContent());
        CommentResponse response = new CommentResponse();
        response.setCommentId(saved.getId());
        response.setAuthorNickname(user.getNickname());
        response.setAuthorProfileImageUrl(imageUrlResolver.resolve(user.getProfileImageKey()));
        response.setContent(saved.getContent());
        response.setCreatedAt(saved.getCreatedAt().toString());
        return response;
    }

    @Transactional
    public void deleteComment(Long commentId, String loginId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow();
        if (!comment.getAuthor().getLoginId().equals(loginId)) {
            throw new IllegalStateException("작성자만 삭제할 수 있습니다.");
        }
        if (comment.getDeletedAt() != null) {
            return;
        }
        comment.markDeleted();
        postRepository.decrementCommentCount(comment.getPost().getId());
    }

    private void publishCommentNotification(Post post, User actor, String content) {
        if (post.getAuthor().getId().equals(actor.getId())) {
            return;
        }
        String preview = buildPreview(content);
        notificationService.createCommentNotification(
            post.getAuthor().getId(),
            post.getId(),
            actor.getNickname(),
            preview
        );
    }

    private String buildPreview(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String trimmed = content.strip();
        int limit = 50;
        if (trimmed.length() <= limit) {
            return trimmed;
        }
        return trimmed.substring(0, limit).stripTrailing() + "...";
    }
}
