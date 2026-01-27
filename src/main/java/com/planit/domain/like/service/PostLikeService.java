package com.planit.domain.like.service;

import com.planit.domain.like.dto.PostLikeResponse;
import com.planit.domain.like.entity.Like;
import com.planit.domain.like.repository.PostLikeRepository;
import com.planit.domain.post.entity.Post;
import com.planit.domain.post.repository.PostRepository;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

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
    public void toggleLike(Long postId, String loginId) {
        User user = userRepository.findByLoginIdAndDeletedFalse(loginId).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow();
        Optional<Like> existing = postLikeRepository.findByPostIdAndAuthorId(postId, user.getId());
        if (existing.isPresent()) {
            postLikeRepository.delete(existing.get());
        } else {
            postLikeRepository.save(Like.of(post, user));
        }
    }
}
