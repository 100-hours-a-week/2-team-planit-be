package com.planit.domain.comment.query.service;

import com.planit.domain.comment.dto.CommentResponse;
import com.planit.domain.comment.query.projection.CommentSummaryProjection;
import com.planit.domain.comment.query.repository.CommentQueryRepository;
import com.planit.global.common.response.PageResponse;
import com.planit.global.config.PageablePolicy;
import com.planit.infrastructure.storage.S3ImageUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentQueryService {

    private static final String DEFAULT_SORT_COLUMN = "created_at";

    private final CommentQueryRepository commentQueryRepository;
    private final S3ImageUrlResolver imageUrlResolver;

    public PageResponse<CommentResponse> listComments(Long postId, Pageable pageable) {
        if (!commentQueryRepository.existsActivePost(postId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다.");
        }

        Pageable safePageable = PageablePolicy.clamp(pageable, Sort.by(Sort.Direction.ASC, DEFAULT_SORT_COLUMN));
        Pageable nativePageable = PageRequest.of(safePageable.getPageNumber(), safePageable.getPageSize());

        Page<CommentSummaryProjection> page = commentQueryRepository.findCommentSummariesByPostId(postId, nativePageable);
        Page<CommentResponse> mapped = page.map(this::toCommentResponse);
        return PageResponse.from(mapped);
    }

    private CommentResponse toCommentResponse(CommentSummaryProjection summary) {
        CommentResponse response = new CommentResponse();
        response.setCommentId(summary.getCommentId());
        response.setAuthorNickname(summary.getAuthorNickname());
        response.setAuthorProfileImageUrl(imageUrlResolver.resolve(summary.getAuthorProfileImageKey()));
        response.setContent(summary.getContent());
        response.setCreatedAt(summary.getCreatedAt().toString());
        return response;
    }
}
