package com.planit.domain.like.service;

import com.planit.domain.like.dto.PostLikeResponse;
import com.planit.domain.like.entity.Like;
import com.planit.domain.like.repository.PostLikeRepository;
import com.planit.domain.notification.service.NotificationService;
import com.planit.domain.post.entity.Post;
import com.planit.domain.post.repository.PostRepository;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public PostLikeResponse getPostLikeInfo(Long postId, String loginId) {
        boolean likedByMe = false;
        if (loginId != null) {
            Optional<User> user = userRepository.findByLoginIdAndDeletedFalse(loginId);
            likedByMe = user.isPresent() && postLikeRepository.existsByPostIdAndAuthorId(postId, user.get().getId());
        }
        long likeCount = postLikeRepository.countByPostId(postId);
        return PostLikeResponse.of(postId, likeCount, likedByMe);
    }

    @Transactional
    public void addLike(Long postId, String loginId) {
        User user = resolveUser(loginId);
        Post post = postRepository.findById(postId).orElseThrow();

        Optional<Like> existingLike = postLikeRepository.findByPostIdAndAuthorId(postId, user.getId());
        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
            return;
        }

        try {
            postLikeRepository.save(Like.of(post, user));
            publishLikeNotificationIfNeeded(post, user);
        } catch (DataIntegrityViolationException ex) {
            // Another request already inserted the like, so skip without failing
        }
    }

    @Transactional
    public void removeLike(Long postId, String loginId) {
        User user = resolveUser(loginId);
        postLikeRepository.deleteByPostIdAndAuthorId(postId, user.getId());
    }

    private User resolveUser(String loginId) {
        return userRepository.findByLoginIdAndDeletedFalse(loginId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "*로그인이 필요한 요청입니다."));
    }

    private void publishLikeNotificationIfNeeded(Post post, User actor) {
        Long authorId = post.getAuthor().getId();
        if (actor.getId().equals(authorId)) {
            return;
        }
        notificationService.createLikeNotification(authorId, post.getId(), actor.getNickname());
    }
}
