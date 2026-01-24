package com.planit.domain.post.controller; // 게시글 관련 컨트롤러 패키지

import com.planit.domain.post.dto.PostDetailResponse; // 상세 DTO
import com.planit.domain.post.dto.PostListResponse; // 목록 DTO
import com.planit.domain.post.entity.BoardType; // 게시판 타입 enum
import com.planit.domain.post.service.PostService; // 게시판 서비스
import jakarta.validation.constraints.Pattern; // 정규 표현식 검증
import jakarta.validation.constraints.Size; // 길이 제한
import java.util.Locale; // 로케일 기반 대소문자 처리
import org.springframework.http.HttpStatus; // HTTP 상태
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 현재 인증자
import org.springframework.security.core.userdetails.UserDetails; // UserDetails 인터페이스
import org.springframework.validation.annotation.Validated; // 검증 활성화
import org.springframework.web.bind.annotation.GetMapping; // GET 매핑
import org.springframework.web.bind.annotation.PathVariable; // 경로 변수
import org.springframework.web.bind.annotation.RequestMapping; // 클래스 단위 경로
import org.springframework.web.bind.annotation.RequestParam; // 쿼리 파라미터
import org.springframework.web.bind.annotation.RestController; // REST 컨트롤러
import org.springframework.web.server.ResponseStatusException; // 예외 처리

@RestController // REST API를 반환하는 컨트롤러
@RequestMapping("/posts") // /api/posts 컨텍스트에 매핑
@Validated // 매개변수 검증 활성화
public class PostController {

    private final PostService postService; // 게시글 서비스 주입

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * 자유게시판 목록.
     * 검색/정렬/페이징(mutable, posts/users/posted_images/comments/likes/post_ranking_snapshots 조합) 처리
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
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "v1 미구현 기능");
        }
        validateSearch(search); // helper text 기준으로 검색어 검증
        PostService.SortOption sortOption;
        try {
            sortOption = PostService.SortOption.valueOf(sort.toUpperCase());
        } catch (IllegalArgumentException e) {
            sortOption = PostService.SortOption.LATEST;
        }
        BoardType resolvedBoardType = BoardType.valueOf(boardType.trim().toUpperCase(Locale.ROOT));
        return postService.listPosts(resolvedBoardType, normalizeSearch(search), sortOption, page, size);
    }

    /**
     * 게시글 상세: 작성자 정보, 최대 5장 이미지, 좋아요·댓글 상태/카운트, 댓글 리스트/삭제 권한 제공
     */
    @GetMapping("/{postId}")
    public PostDetailResponse getPostDetail(
        @PathVariable Long postId,
        @AuthenticationPrincipal UserDetails principal
    ) {
        String loginId = principal == null ? null : principal.getUsername();
        return postService.getPostDetail(postId, loginId);
    }

    private void validateSearch(String search) {
        if (search == null || search.isBlank()) {
            return;
        }
        if (search.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*최소 2글자 부터 검색 가능합니다.");
        }
        if (search.length() > 24) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*최대 24자까지 검색 가능합니다.");
        }
        if (search.matches(".*[ㄱ-ㅎㅏ-ㅣ]+.*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*올바른 검색어를 입력해주세요");
        }
        if (!search.matches("^[가-힣a-zA-Z0-9\\s]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*특수문자는 입력 불가합니다");
        }
    }

    private String normalizeSearch(String search) {
        return search == null ? "" : search.trim();
    }
}
