package com.planit.domain.post.controller;

import com.planit.domain.post.dto.PostListResponse;
import com.planit.domain.post.entity.BoardType;
import com.planit.domain.post.service.PostService;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Collections;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/posts")
@Validated
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * 자유 게시판을 전제로 게시글 제목/내용 OR 검색(최소 2자 최대 24자, 특수문자/초성 불가),
     * 정렬(dropdown: 최신, 댓글 순, 좋아요 순), 무한 스크롤 페이징을 조합하여 목록을 제공합니다.
     * 매 요청은 posts/users/comments/likes/posted_images/post_ranking_snapshots 테이블을 조합한 결과이며,
     * boardType이 FREE가 아닌 경우 v1 미구현 메시지를 반환합니다.
     */
    @GetMapping
    public PostListResponse listPosts(
        @RequestParam(defaultValue = "FREE") String boardType,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "LATEST") String sort,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        if (!"FREE".equalsIgnoreCase(boardType)) {
            // v1에서는 자유 게시판 외 탭 클릭 시 toplevel 뷰에 'v1 미구현 기능' 토스트를 띄우고 새로고침을 유도
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "v1 미구현 기능");
        }
        validateSearch(search);
        PostService.SortOption sortOption;
        try {
            sortOption = PostService.SortOption.valueOf(sort.toUpperCase());
        } catch (IllegalArgumentException e) {
            sortOption = PostService.SortOption.LATEST;
        }
        BoardType resolvedBoardType = BoardType.valueOf(boardType.trim().toUpperCase(Locale.ROOT));
        return postService.listPosts(resolvedBoardType, normalizeSearch(search), sortOption, page, size);
    }

    private void validateSearch(String search) {
        if (search == null || search.isBlank()) {
            return;
        }
        // 검색어 최소 옵션
        if (search.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*최소 2글자 부터 검색 가능합니다.");
        }
        // 최대 글자 제한
        if (search.length() > 24) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*최대 24자까지 검색 가능합니다.");
        }
        // 한국어 초성 입력 방지
        if (search.matches(".*[ㄱ-ㅎㅏ-ㅣ]+.*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*올바른 검색어를 입력해주세요");
        }
        // 한글/영어/숫자/공백 외 특수문자 금지
        if (!search.matches("^[가-힣a-zA-Z0-9\\s]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*특수문자는 입력 불가합니다");
        }
    }

    private String normalizeSearch(String search) {
        return search == null ? "" : search.trim();
    }
}
