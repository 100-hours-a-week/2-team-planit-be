package com.planit.domain.post.service;

import com.planit.domain.post.dto.PostDetailResponse;
import com.planit.domain.post.dto.PostListResponse;
import com.planit.domain.post.dto.PostSummaryResponse;
import com.planit.domain.post.entity.BoardType;
import com.planit.domain.post.repository.PostRepository;
import com.planit.domain.user.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository; // Repository/ 커스텀 and list
    private final UserRepository userRepository; // 사용자 정보 조회

    /**
     * 자유 게시판 목록 조회: posts/users/posted_images/comments/likes/post_ranking_snapshots를 조합한 DTO를 반환
     */
    public PostListResponse listPosts(
        BoardType boardType,
        String search,
        SortOption sortOption,
        int page,
        int size
    ) {
        // Pageable을 생성하여 정렬/페이징을 적용
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
        // 무한 스크롤을 위한 다음 페이지 존재 여부 전달
        return new PostListResponse(items, result.hasNext());
    }

    /**
     * 상세 페이지: 게시판 이름/설명, 작성자/좋아요/이미지/댓글 상태까지 포함한 DTO 반환
     */
    public PostDetailResponse getPostDetail(Long postId, String loginId) {
        Long requesterId = resolveRequesterId(loginId);
        return postRepository.findDetailById(postId, requesterId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."));
    }

    // 요청자 loginId로 실제 사용자 PK를 얻어 Authentication 기반 권한 판단에 사용
    private Long resolveRequesterId(String loginId) {
        if (loginId == null) {
            return null;
        }
        return userRepository.findByLoginIdAndDeletedFalse(loginId)
            .map(com.planit.domain.user.entity.User::getId)
            .orElse(null);
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
