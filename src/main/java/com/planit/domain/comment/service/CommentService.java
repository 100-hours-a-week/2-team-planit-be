package com.planit.domain.comment.service;

import com.planit.domain.comment.dto.CommentRequest;
import com.planit.domain.comment.dto.CommentResponse;
import com.planit.domain.comment.entity.Comment;
import com.planit.domain.comment.repository.CommentRepository;
import com.planit.domain.notification.service.NotificationService;
import com.planit.domain.post.entity.Post;
import com.planit.domain.post.repository.PostRepository;
import com.planit.domain.post.stats.service.PostStatsAggregationService;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import com.planit.global.common.response.PageResponse;
import com.planit.global.config.PageablePolicy;
import com.planit.infrastructure.storage.S3ImageUrlResolver;
import java.time.LocalDateTime;
import java.util.function.Supplier;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private final PostStatsAggregationService postStatsAggregationService;

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> listComments(Long postId, Pageable pageable) {
        postRepository.findByIdAndDeletedFalse(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."));
        Pageable safePageable = PageablePolicy.clamp(pageable, Sort.by(Sort.Direction.ASC, "created_at"));
        Page<CommentRepository.CommentProjection> page = commentRepository.findDetailsPageByPostId(postId, safePageable);
        Page<CommentResponse> mapped = page.map(detail -> {
            CommentResponse response = new CommentResponse();
            response.setCommentId(detail.getCommentId());
            response.setAuthorNickname(detail.getAuthorNickname());
            response.setAuthorProfileImageUrl(imageUrlResolver.resolve(detail.getAuthorProfileImageKey()));
            response.setContent(detail.getContent());
            response.setCreatedAt(detail.getCreatedAt().toString());
            return response;
        });
        return PageResponse.from(mapped);
    }

    @Transactional
    public CommentResponse addComment(Long postId, String loginId, CommentRequest request) {
        return retryOnDeadlock(() -> {
            Post post = postRepository.findByIdAndDeletedFalse(postId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."));
            User user = userRepository.findByLoginIdAndDeletedFalse(loginId).orElseThrow();
            LocalDateTime now = LocalDateTime.now();
            Comment comment = Comment.create(post, user, request.getContent(), now);
            Comment saved = commentRepository.save(comment);
            postStatsAggregationService.increaseCommentCount(postId);
            publishCommentNotification(post, user, request.getContent());
            String profileImageUrl = imageUrlResolver.resolve(user.getProfileImageKey());
            return CommentResponse.from(saved, profileImageUrl);
        });
    }

    @Transactional
    public void deleteComment(Long commentId, String loginId) {
        retryOnDeadlock(() -> {
            Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 댓글을 찾을 수 없습니다."));
            if (!comment.getAuthor().getLoginId().equals(loginId)) {
                throw new IllegalStateException("작성자만 삭제할 수 있습니다.");
            }
            Post post = comment.getPost();
            LocalDateTime deletedAt = LocalDateTime.now();
            int updatedRows = commentRepository.markAsDeleted(commentId, deletedAt);
            if (updatedRows == 0) {
                throw new IllegalStateException("이미 삭제된 댓글입니다.");
            }
            postStatsAggregationService.decreaseCommentCount(post.getId());
            return null;
        });
    }

    private <T> T retryOnDeadlock(Supplier<T> supplier) {
        int attempts = 0;
        while (true) {
            try {
                return supplier.get();
            } catch (CannotAcquireLockException | DeadlockLoserDataAccessException e) {
                if (++attempts >= 3) {
                    throw e;
                }
                try {
                    Thread.sleep(50L * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("retry interrupted", ie);
                }
            }
        }
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
