package com.planit.domain.post.controller; // 게시글 관련 컨트롤러 패키지

import com.planit.domain.post.dto.PostCreateRequest;
import com.planit.domain.post.dto.PostCreateResponse;
import com.planit.domain.post.dto.PostDetailResponse;
import com.planit.domain.post.dto.PostListResponse;
import com.planit.infrastructure.storage.dto.PresignedUrlRequest;
import com.planit.domain.post.dto.PostUpdateRequest;
import com.planit.infrastructure.storage.dto.PresignedUrlResponse;
import com.planit.domain.post.entity.BoardType; // 게시판 타입 enum
import com.planit.domain.post.service.PostService; // 게시판 서비스
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 현재 인증자
import org.springframework.security.core.userdetails.UserDetails; // UserDetails 인터페이스
import org.springframework.validation.annotation.Validated; // 검증 활성화
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping; // 클래스 단위 경로
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController; // REST 컨트롤러
import org.springframework.web.server.ResponseStatusException; // 예외 처리

@RestController // REST API를 반환하는 컨트롤러
@RequestMapping("/posts") // /api/posts 컨텍스트에 매핑
@Validated // 매개변수 검증 활성화
@Tag(name = "게시물", description = "자유게시판 목록/상세/등록 API")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    /**
     * 자유게시판 목록.
     * 검색/정렬/페이징(mutable, posts/users/posted_images/comments/likes/post_ranking_snapshots 조합) 처리
     */
    @Operation(summary = "자유게시판 목록 조회",
            description = """
            posts/users/posted_images/comments/likes/post_ranking_snapshots 테이블을 조합하여 제목·대표 이미지·좋아요/댓글·랭킹 점수를 반환합니다.
            검색어는 helper text 기준으로 2~24자/한글 초성·특수 문자 제한을 검증하고 최신/댓글/좋아요 정렬을 지원하며 무한 스크롤을 위해 pagesize 단위 offset을 제공합니다.
            """)
    @GetMapping
    public PostListResponse listPosts(
            @RequestParam(defaultValue = "FREE") String boardType,
            @Parameter(description = "검색어(히스토리/단어 길이 2~24자, 특수문자/초성 불가)") @RequestParam(required = false) String search,
            @Parameter(description = "정렬 옵션(latest/comment/like)") @RequestParam(required = false) String sort,
            @Parameter(description = "페이지 번호(0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 사이즈(최대 50)") @RequestParam(defaultValue = "20") int size
    ) {
        BoardType resolvedBoardType = parseBoardType(boardType);
        if (BoardType.FREE != resolvedBoardType) {
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "v1 미구현 기능");
        }
        validateSearch(search); // helper text 기준으로 검색어 검증
        PostService.SortOption sortOption = resolveSortOption(sort);
        return postService.listPosts(resolvedBoardType, normalizeSearch(search), sortOption, page, size);
    }

    /**
     * 게시글 상세: 작성자 정보, 최대 5장 이미지, 좋아요·댓글 상태/카운트, 댓글 리스트/삭제 권한 제공
     */
    @Operation(summary = "게시글 상세 조회",
            description = "게시판 정보·작성자 프로필·최대 5장 이미지·좋아요/댓글 상태·댓글 목록(최대 20개) 등 상세 콘텐츠를 반환합니다.")
    @GetMapping("/{postId}")
    public PostDetailResponse getPostDetail(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        String loginId = principal == null ? null : principal.getUsername();
        return postService.getPostDetail(postId, loginId);
    }

    /** 게시물 이미지 Presigned URL 발급 */
    @PostMapping("/images/presigned-url")
    public PresignedUrlResponse getPostPresignedUrl(
            @AuthenticationPrincipal UserDetails principal,
            @jakarta.validation.Valid @RequestBody PresignedUrlRequest request
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "*로그인이 필요한 요청입니다.");
        }
        return postService.getPostPresignedUrl(
                principal.getUsername(),
                request.getFileExtension(),
                request.getContentType()
        );
    }

    @Operation(summary = "자유게시판 글 등록",
            description = "Presigned URL로 이미지 업로드 후 imageKeys와 함께 JSON으로 등록합니다.")
    @PostMapping
    public PostCreateResponse createPost(
            @jakarta.validation.Valid @RequestBody PostCreateRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "*로그인이 필요한 요청입니다.");
        }
        return postService.createPost(request, principal.getUsername());
    }

    @Operation(summary = "자유게시판 글 수정",
            description = "제목/본문/이미지(imageKeys)를 JSON으로 수정합니다.")
    @PatchMapping("/{postId}")
    public PostCreateResponse updatePost(
            @PathVariable Long postId,
            @jakarta.validation.Valid @RequestBody PostUpdateRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "*로그인이 필요한 요청입니다.");
        }
        return postService.updatePost(postId, request, principal.getUsername());
    }

    @Operation(summary = "게시물 이미지 삭제")
    @DeleteMapping("/{postId}/images/{imageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePostImage(
            @PathVariable Long postId,
            @PathVariable Long imageId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "*로그인이 필요한 요청입니다.");
        }
        postService.deletePostImage(postId, imageId, principal.getUsername());
    }

    @Operation(summary = "자유게시판 글 삭제",
            description = "작성자만 삭제 가능하며, 논리 삭제됩니다.")
    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "*로그인이 필요한 요청입니다.");
        }
        postService.deletePost(postId, principal.getUsername());
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

    private PostService.SortOption resolveSortOption(String sort) {
        if (sort == null || sort.isBlank()) {
            return PostService.SortOption.LATEST;
        }
        switch (sort.trim().toLowerCase(Locale.ROOT)) {
            case "latest":
                return PostService.SortOption.LATEST;
            case "comment":
                return PostService.SortOption.COMMENTS;
            case "like":
                return PostService.SortOption.LIKES;
            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*지원하지 않는 정렬 방식입니다.");
        }
    }

    private BoardType parseBoardType(String boardType) {
        if (boardType == null || boardType.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*지원하지 않는 게시판입니다.");
        }
        String normalized = boardType.trim().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "freely" , "free" , "자유게시판" , "자유" -> BoardType.FREE;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*지원하지 않는 게시판입니다.");
        };
    }
}