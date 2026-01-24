package com.planit.domain.post.service;

import com.planit.domain.post.dto.PostListResponse;
import com.planit.domain.post.dto.PostSummaryResponse;
import com.planit.domain.post.entity.BoardType;
import com.planit.domain.post.repository.PostRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;

    /**
     * 자유 게시판 기준으로 posts, users, comments, likes, posted_images, post_ranking_snapshots를 결합해
     * 검색/정렬/페이징된 게시물 카드를 구성합니다.
     */
    public PostListResponse listPosts(
        BoardType boardType,
        String search,
        SortOption sortOption,
        int page,
        int size
    ) {
        // 정렬 기준에 따라 Pageable 생성
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortOption.getSortProperty()).descending());
        // 검색어를 like 패턴으로 변환
        String pattern = search == null || search.isBlank() ? "%" : "%" + search + "%";
        Page<PostRepository.PostSummary> result = postRepository.searchByBoardType(
            boardType.name(),
            search,
            pattern,
            pageable
        );
        List<PostSummaryResponse> items = result.getContent().stream()
            .map(summary -> new PostSummaryResponse(
                summary.getPostId(),
                summary.getTitle(),
                summary.getAuthorId(),
                summary.getAuthorNickname(),
                summary.getAuthorProfileImageId(),
                summary.getCreatedAt(),
                summary.getLikeCount(),
                summary.getCommentCount(),
                summary.getRepresentativeImageId(),
                summary.getRankingScore()
            ))
            // DTO 리스트로 수집
            .collect(Collectors.toList());
        // 다음 페이지 존재 여부를 함께 반환
        return new PostListResponse(items, result.hasNext());
    }

    public enum SortOption {
        LATEST("createdAt"), // 최신순 (무한 스크롤 기준)
        COMMENTS("commentCount"), // 댓글 많은 순 (최근 1년 집계)
        LIKES("likeCount"); // 좋아요 많은 순 (최근 1년 집계)

        private final String sortProperty;

        SortOption(String sortProperty) {
            this.sortProperty = sortProperty;
        }

        public String getSortProperty() {
            return sortProperty;
        }
    }
}
