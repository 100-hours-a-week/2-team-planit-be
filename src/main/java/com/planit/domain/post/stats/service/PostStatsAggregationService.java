package com.planit.domain.post.stats.service;

import com.planit.domain.post.stats.repository.PostCommentCountRepository;
import com.planit.domain.post.stats.repository.PostLikeCountRepository;
import com.planit.domain.post.stats.repository.PostViewCountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PostStatsAggregationService {

    private final PostLikeCountRepository postLikeCountRepository;
    private final PostCommentCountRepository postCommentCountRepository;
    private final PostViewCountRepository postViewCountRepository;

    public PostStatsAggregationService(
            PostLikeCountRepository postLikeCountRepository,
            PostCommentCountRepository postCommentCountRepository,
            PostViewCountRepository postViewCountRepository
    ) {
        this.postLikeCountRepository = postLikeCountRepository;
        this.postCommentCountRepository = postCommentCountRepository;
        this.postViewCountRepository = postViewCountRepository;
    }

    public void increaseLikeCount(Long postId) {
        postLikeCountRepository.upsertAndAdjust(postId, 1L);
    }

    public void decreaseLikeCount(Long postId) {
        postLikeCountRepository.upsertAndAdjust(postId, -1L);
    }

    public void increaseCommentCount(Long postId) {
        postCommentCountRepository.upsertAndAdjust(postId, 1L);
    }

    public void decreaseCommentCount(Long postId) {
        postCommentCountRepository.upsertAndAdjust(postId, -1L);
    }

    public void increaseViewCount(Long postId) {
        postViewCountRepository.upsertAndIncrease(postId);
    }

    public void replaceCounts(Long postId, long likeCount, long commentCount, long viewCount) {
        postLikeCountRepository.upsertAndSet(postId, likeCount);
        postCommentCountRepository.upsertAndSet(postId, commentCount);
        postViewCountRepository.upsertAndSet(postId, viewCount);
    }
}
