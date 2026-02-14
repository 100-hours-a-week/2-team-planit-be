package com.planit.domain.post.service;

import com.planit.domain.common.entity.Image;
import com.planit.domain.common.repository.ImageRepository;
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
import com.planit.infrastructure.storage.S3ImageUrlResolver;
import com.planit.infrastructure.storage.UploadContext;
import com.planit.infrastructure.storage.UploadUrlProvider;
import com.planit.infrastructure.storage.dto.PresignedUrlResponse;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/** Business logic for post listing/create/detail operations */
@Service
@RequiredArgsConstructor
public class PostService {
    private static final Logger log = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;
    private final PostedImageRepository postedImageRepository;
    private final ImageRepository imageRepository;
    private final ObjectProvider<UploadUrlProvider> uploadUrlProvider;
    private final S3ImageUrlResolver imageUrlResolver;

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
                        imageUrlResolver.resolve(summary.getAuthorProfileImageKey()),
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

    /** Presigned URL 발급 (게시물 이미지) */
    public PresignedUrlResponse getPostPresignedUrl(String loginId, String fileExtension, String contentType) {
        User user = resolveRequester(loginId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "*로그인이 필요한 요청입니다.");
        }
        UploadUrlProvider provider = uploadUrlProvider.getIfAvailable();
        if (provider == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "*이미지 업로드 기능이 비활성화 되어 있습니다.");
        }
        return provider.issuePresignedPutUrl(
                UploadContext.post(user.getId(), fileExtension, contentType)
        );
    }

    /**
     * 업로드만 하고 게시글에 저장하지 않은 이미지(고아 객체)를 S3에서 삭제.
     * key는 post/{userId}/... 형식이어야 하며, 현재 사용자 본인 key만 삭제 가능.
     */
    public void deletePostImageByKey(String key, String loginId) {
        User user = resolveRequester(loginId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "*로그인이 필요한 요청입니다.");
        }
        validatePostImageKey(key);
        String expectedPrefix = "post/" + user.getId() + "/";
        if (!key.startsWith(expectedPrefix)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "*본인이 업로드한 이미지만 삭제할 수 있습니다.");
        }
        deleteFromS3IfPresent(key);
    }

    /** 게시글 작성: imageKeys (Presigned URL 업로드 완료 후) */
    @Transactional
    public PostCreateResponse createPost(PostCreateRequest request, String loginId) {
        User user = resolveRequester(loginId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "*로그인이 필요한 요청입니다.");
        }
        LocalDateTime now = LocalDateTime.now();
        Post post = Post.create(user, request.getTitle(), request.getContent(), BoardType.FREE, now);
        Post saved = postRepository.save(post);
        List<Long> imageIds = savePostImages(saved.getId(), request.getImageKeys(), now);
        return new PostCreateResponse(saved.getId(),
                saved.getBoardType(),
                saved.getTitle(),
                saved.getContent(),
                saved.getCreatedAt(),
                saved.getAuthor().getId(),
                imageIds);
    }

    /**
     * 게시글 수정: 제목/본문/이미지(imageKeys로 교체)
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
        deleteExistingPostImages(postId);
        List<Long> imageIds = savePostImages(postId, request.getImageKeys(), now);
        Post saved = postRepository.save(post);
        return new PostCreateResponse(saved.getId(),
                saved.getBoardType(),
                saved.getTitle(),
                saved.getContent(),
                saved.getCreatedAt(),
                saved.getAuthor().getId(),
                imageIds);
    }

    /** 게시글 이미지 단건 삭제 (imageId = Image.id) */
    @Transactional
    public void deletePostImage(Long postId, Long imageId, String loginId) {
        User user = resolveRequester(loginId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "*로그인이 필요한 요청입니다.");
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."));
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "작성자만 삭제 가능합니다.");
        }
        PostedImage postedImage = postedImageRepository.findByPostIdAndImageId(postId, imageId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 이미지입니다."));
        Image image = imageRepository.findById(postedImage.getImageId()).orElse(null);
        postedImageRepository.delete(postedImage);
        if (image != null) {
            String s3Key = image.getS3Key();
            imageRepository.delete(image);
            deleteFromS3IfPresent(s3Key);
        }
    }

    private List<Long> savePostImages(Long postId, List<String> imageKeys, LocalDateTime now) {
        List<Long> imageIds = new ArrayList<>();
        List<String> keys = imageKeys == null ? Collections.emptyList() : imageKeys;
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            if (!StringUtils.hasText(key)) {
                continue;
            }
            validatePostImageKey(key);
            Long imageId = imageStorageService.storeByS3Key(key);
            imageIds.add(imageId);
            PostedImage postedImage = new PostedImage(postId, imageId, i == 0, now);
            postedImageRepository.save(postedImage);
        }
        return imageIds;
    }

    private void deleteExistingPostImages(Long postId) {
        List<PostedImage> existing = postedImageRepository.findByPostId(postId);
        log.info("deleteExistingPostImages: postId={}, count={}", postId, existing.size());
        for (PostedImage pi : existing) {
            Image image = imageRepository.findById(pi.getImageId()).orElse(null);
            postedImageRepository.delete(pi);
            if (image != null) {
                String s3Key = image.getS3Key();
                imageRepository.delete(image);
                deleteFromS3IfPresent(s3Key);
            }
        }
    }

    /** S3 객체 삭제. 실패해도 트랜잭션은 커밋되도록 예외를 삼킨다. */
    private void deleteFromS3IfPresent(String s3Key) {
        if (!StringUtils.hasText(s3Key)) {
            log.info("S3 delete skip: image has no s3_key");
            return;
        }
        UploadUrlProvider provider = uploadUrlProvider.getIfAvailable();
        if (provider == null) {
            log.info("image delete skip: UploadUrlProvider not available (storage.mode?), key={}", s3Key);
            return;
        }
        try {
            provider.deleteByKey(s3Key);
            log.info("image deleted: {}", s3Key);
        } catch (Exception e) {
            log.warn("image delete failed for key={}, continuing (DB already updated)", s3Key, e);
        }
    }

    private void validatePostImageKey(String key) {
        if (!key.startsWith("post/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*유효하지 않은 이미지 key입니다.");
        }
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
        deleteExistingPostImages(postId);
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
