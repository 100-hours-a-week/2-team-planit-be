package com.planit.domain.post.service;

import com.planit.domain.post.dto.PostCreateRequest;
import com.planit.domain.post.dto.PostCreateResponse;
import com.planit.domain.post.dto.PostDetailResponse;
import com.planit.domain.post.dto.PostListResponse;
import com.planit.domain.post.dto.PostSummaryResponse;
import com.planit.domain.post.dto.PostUpdateRequest;
import com.planit.domain.post.entity.BoardType;
import com.planit.domain.post.entity.Post;
import com.planit.domain.post.entity.PostedImage;
import com.planit.domain.post.repository.PostRepository;
import com.planit.domain.post.repository.PostedImageRepository;
import com.planit.domain.post.service.ImageStorageService;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Business logic for post listing/create/detail operations */
@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository; // 게시글 조회 repository
    private final UserRepository userRepository; // 인증 + author 조회
    private final ImageStorageService imageStorageService; // 이미지 저장
    private final PostedImageRepository postedImageRepository; // 게시글 이미지 인서트

    /** 자유게시판 리스트를 DTO로 반환한다 */
    public PostListResponse listPosts(
        BoardType boardType,
        String search,
        SortOption sortOption,
        int page,
        int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        String pattern = buildSearchPattern(search);
        Page<PostRepository.PostSummary> result = postRepository.searchByBoardType(
            boardType.name(),
            pattern,
            sortOption.name(),
            pageable
        );
        List<PostSummaryResponse> items = result.getContent()
            .stream()
            .map(summary -> new PostSummaryResponse(
                summary.getPostId(),
                summary.getTitle(),
                summary.getAuthorId(),
                summary.getAuthorNickname(),
                summary.getAuthorProfileImageUrl(),
                summary.getCreatedAt(),
                summary.getLikeCount(),
                summary.getCommentCount(),
                summary.getRepresentativeImageId(),
                summary.getRankingScore(),
                summary.getPlaceName(),
                summary.getTripTitle()
            ))
            .collect(Collectors.toList());
        return new PostListResponse(items, result.hasNext());
    }

    /** 게시글 작성: User 인증 + multipart 이미지 저장 */
    @Transactional
    public PostCreateResponse createPost(PostCreateRequest request, List<MultipartFile> images, String loginId) {
        User user = resolveRequester(loginId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "*로그인이 필요한 요청입니다.");
        }
        LocalDateTime now = LocalDateTime.now(); // 기기 시간 기준
        Post post = Post.create(user, request.getTitle(), request.getContent(), BoardType.FREE, now);
        Post saved = postRepository.save(post);
        List<Long> imageIds = new ArrayList<>();
        List<MultipartFile> uploadImages = images == null ? Collections.emptyList() : images;
        for (MultipartFile file : uploadImages) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (file.getSize() > 5 * 1024 * 1024) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*이미지 크기는 최대 5MB까지만 허용됩니다.");
            }
            Long imageId = imageStorageService.store(file);
            imageIds.add(imageId);
            PostedImage postedImage = new PostedImage(saved.getId(), imageId, imageIds.size() == 1, now);
            postedImageRepository.save(postedImage);
        }
        return new PostCreateResponse(saved.getId(),
            saved.getBoardType(),
            saved.getTitle(),
            saved.getContent(),
            saved.getCreatedAt(),
            saved.getAuthor().getId(),
            imageIds);
    }

    /**
     * 게시글 수정: 기존 Post를 조회하여 제목/본문/이미지 업데이트
     */
    @Transactional
    public PostCreateResponse updatePost(Long postId, PostUpdateRequest request, String loginId) {
        User user = resolveRequester(loginId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "*로그인이 필요한 요청입니다.");
        }
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."));
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "작성자만 수정 가능합니다.");
        }
        LocalDateTime now = LocalDateTime.now();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.touchUpdatedAt(now);
        List<Long> imageIds = new ArrayList<>();
        for (MultipartFile file : request.getImages()) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (file.getSize() > 5 * 1024 * 1024) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*이미지 크기는 최대 5MB까지만 허용됩니다.");
            }
            Long imageId = imageStorageService.store(file);
            imageIds.add(imageId);
            PostedImage postedImage = new PostedImage(post.getId(), imageId, imageIds.size() == 1, now);
            postedImageRepository.save(postedImage);
        }
        Post saved = postRepository.save(post);
        return new PostCreateResponse(saved.getId(),
            saved.getBoardType(),
            saved.getTitle(),
            saved.getContent(),
            saved.getCreatedAt(),
            saved.getAuthor().getId(),
            imageIds);
    }

    /**
     * 게시글 삭제: 작성자만 가능하며 논리 삭제
     */
    @Transactional
    public void deletePost(Long postId, String loginId) {
        User user = resolveRequester(loginId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "*로그인이 필요한 요청입니다.");
        }
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."));
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "작성자만 삭제할 수 있습니다.");
        }
        post.markDeleted(LocalDateTime.now());
        postRepository.save(post);
    }

    /** 상세 조회: Post + requester 기준 권한 판단 */
    public PostDetailResponse getPostDetail(Long postId, String loginId) {
        User requester = resolveRequester(loginId);
        Long requesterId = requester == null ? null : requester.getId();
        return postRepository.findDetailById(postId, requesterId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."));
    }

    /** 로그인 사용자를 User 엔티티로 반환 */
    private User resolveRequester(String loginId) {
        if (loginId == null) {
            return null;
        }
        return userRepository.findByLoginIdAndDeletedFalse(loginId)
            .orElse(null);
    }

    /** 리스트 정렬 옵션 */
    public enum SortOption {
        LATEST("createdAt"),
        COMMENTS("commentCount"),
        LIKES("likeCount");

        private final String sortProperty;

        SortOption(String sortProperty) {
            this.sortProperty = sortProperty;
        }

        public String getSortProperty() {
            return sortProperty;
        }
    }

    private String buildSearchPattern(String search) {
        if (search == null || search.isBlank()) {
            return "%";
        }
        return "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
    }
}
