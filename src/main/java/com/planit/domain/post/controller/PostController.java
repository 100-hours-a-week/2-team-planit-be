package com.planit.domain.post.controller; // 게시글 관련 컨트롤러 패키지

import com.planit.domain.post.dto.PostCreateRequest;
import com.planit.domain.post.dto.PostCreateResponse;
import com.planit.domain.post.dto.PostDetailResponse; // 상세 DTO
import com.planit.domain.post.dto.PostListResponse; // 목록 DTO
import com.planit.domain.post.dto.PostUpdateRequest;
import com.planit.domain.post.entity.BoardType; // 게시판 타입 enum
import com.planit.domain.post.service.PostService; // 게시판 서비스
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid; // DTO 검증
import jakarta.validation.constraints.NotBlank; // 필수 입력
import jakarta.validation.constraints.Pattern; // 정규 표현식 검증
import jakarta.validation.constraints.Size; // 길이 제한
import java.util.Collections;
import java.util.List;
import java.util.Locale; // 로케일 기반 대소문자 처리
import org.springframework.http.HttpStatus; // HTTP 상태
import org.springframework.http.MediaType; // 멀티파트 mime
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 현재 인증자
import org.springframework.security.core.userdetails.UserDetails; // UserDetails 인터페이스
import org.springframework.validation.annotation.Validated; // 검증 활성화
import org.springframework.web.bind.annotation.GetMapping; // GET 매핑
import org.springframework.web.bind.annotation.ModelAttribute; // ModelAttribute 바인딩
import org.springframework.web.bind.annotation.PathVariable; // 경로 변수
import org.springframework.web.bind.annotation.PostMapping; // POST 매핑
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping; // 클래스 단위 경로
import org.springframework.web.bind.annotation.RequestParam; // 쿼리 파라미터
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RestController; // REST 컨트롤러
import org.springframework.web.server.ResponseStatusException; // 예외 처리
import org.springframework.web.bind.annotation.DeleteMapping; // DELETE 매핑

@RestController // REST API를 반환하는 컨트롤러
@RequestMapping("/posts") // /api/posts 컨텍스트에 매핑
@Validated // 매개변수 검증 활성화
@Tag(name = "게시물", description = "자유게시판 목록/상세/등록 API")
public class PostController {

    private final PostService postService; // 게시글 서비스 주입

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
        @Parameter(description = "정렬 옵션(LATEST/COMMENTS/LIKES)") @RequestParam(defaultValue = "LATEST") String sort,
        @Parameter(description = "페이지 번호(0부터)") @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "페이지 사이즈(최대 50)") @RequestParam(defaultValue = "20") int size
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

    @Operation(summary = "자유게시판 글 등록",
        description = """
            드롭다운에서 자유게시판을 선택하고 제목(24자) + 내용(2000자) + 최대 5장 이미지(5MB, jpg/png/webp 등)를 Multipart 형식으로 업로드하면
            posts/users/posted_images/images/post_places/places 테이블을 조합해 게시글과 이미지/연계 데이터를 저장합니다.
            """,
        requestBody = @RequestBody(
            required = true,
            content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
        ))
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PostCreateResponse createPost(
        @Valid @ModelAttribute PostCreateRequest request,
        @AuthenticationPrincipal UserDetails principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "*로그인이 필요한 요청입니다.");
        }
        return postService.createPost(request, principal.getUsername());
    }

    @Operation(summary = "자유게시판 글 수정",
        description = """
            기존 게시글 제목/본문/이미지를 수정합니다.
            게시판은 변경 불가하고 최대 5장(multiform)까지 업로드 가능합니다.
            """)
    @PutMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PostCreateResponse updatePost(
        @PathVariable Long postId,
        @RequestPart("title") @NotBlank @Size(max = 24) String title,
        @RequestPart("content") @NotBlank @Size(max = 2000) String content,
        @RequestPart(value = "images", required = false) List<MultipartFile> images,
        @AuthenticationPrincipal UserDetails principal
    ) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "*로그인이 필요한 요청입니다.");
        }
        PostUpdateRequest request = new PostUpdateRequest(
            title,
            content,
            images == null ? Collections.emptyList() : images
        );
        return postService.updatePost(postId, request, principal.getUsername());
    }

    @Operation(summary = "자유게시판 글 삭제",
        description = """
            작성자만 삭제 버튼을 볼 수 있으며, 모달에서 확인 후 삭제 시 해당 게시글이 논리 삭제됩니다.
            삭제 완료 시 자유게시판 목록으로 리다이렉트되어야 하며 이미지/댓글은 그대로 유지하면서 삭제 플래그만 변경합니다.
            """)
    @DeleteMapping("/{postId}")
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
}
