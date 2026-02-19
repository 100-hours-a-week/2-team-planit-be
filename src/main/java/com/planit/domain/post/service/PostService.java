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
import com.planit.domain.post.entity.PostedPlan;
import com.planit.domain.post.entity.PostedPlace;
import com.planit.domain.post.repository.PostRepository;
import com.planit.domain.post.repository.PostedImageRepository;
import com.planit.domain.post.repository.PostedPlanRepository;
import com.planit.domain.post.repository.PostedPlaceRepository;
import com.planit.domain.post.service.ImageStorageService;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.repository.ItineraryItemPlaceRepository;
import com.planit.domain.trip.repository.TripRepository;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import com.planit.global.common.exception.BusinessException;
import com.planit.global.common.exception.ErrorCode;
import com.planit.infrastructure.storage.S3ImageUrlResolver;
import com.planit.infrastructure.storage.S3ObjectDeleter;
import com.planit.infrastructure.storage.S3PresignedUrlService;
import com.planit.infrastructure.storage.dto.PresignedUrlResponse;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
    private static final String PLAN_SHARE_PLACEHOLDER_IMAGE = "/images/plan-share-default.png";

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ImageStorageService imageStorageService;
    private final PostedImageRepository postedImageRepository;
    private final ImageRepository imageRepository;
    private final ObjectProvider<S3PresignedUrlService> presignedUrlServiceProvider;
    private final ObjectProvider<S3ObjectDeleter> s3ObjectDeleterProvider;
    private final S3ImageUrlResolver imageUrlResolver;
    private final TripRepository tripRepository;
    private final PostedPlanRepository postedPlanRepository;
    private final PostedPlaceRepository postedPlaceRepository;
    private final ItineraryItemPlaceRepository itineraryItemPlaceRepository;

    /** 자유게시판 리스트를 DTO로 반환한다 */
    public PostListResponse listPosts(
            BoardType boardType,
            String search,
            SortOption sortOption,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        String normalizedSearch = search == null ? "" : search;
        Page<PostRepository.PostSummary> result = postRepository.searchByBoardType(
                boardType.name(),
                normalizedSearch,
                sortOption.name(),
                pageable
        );
        List<PostSummaryResponse> items = result.getContent()
                .stream()
                .map(summary -> {
                    String thumbnailUrl = null;
                    if (summary.getRepresentativeImageKey() != null) {
                        thumbnailUrl = imageUrlResolver.resolve(summary.getRepresentativeImageKey());
                    }
                    return new PostSummaryResponse(
                            summary.getPostId(),
                            summary.getTitle(),
                            summary.getAuthorId(),
                            summary.getAuthorNickname(),
                            imageUrlResolver.resolve(summary.getAuthorProfileImageKey()),
                            summary.getCreatedAt(),
                            summary.getLikeCount(),
                            summary.getCommentCount(),
                            summary.getRepresentativeImageId(),
                            thumbnailUrl,
                            summary.getRankingScore(),
                            summary.getPlaceName(),
                            summary.getTripTitle()
                    );
                })
                .collect(Collectors.toList());
        if (boardType == BoardType.PLAN_SHARE) {
            overridePlanShareImages(items);
        }
        return new PostListResponse(items, result.hasNext(), page, size);
    }

    /** Presigned URL 발급 (게시물 이미지) */
    public PresignedUrlResponse getPostPresignedUrl(String loginId, String fileExtension, String contentType) {
        User user = resolveRequester(loginId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "*로그인이 필요한 요청입니다.");
        }
        S3PresignedUrlService service = presignedUrlServiceProvider.getIfAvailable();
        if (service == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "*이미지 업로드 기능이 비활성화 되어 있습니다.");
        }
        return service.createPostPresignedUrl(user.getId(), fileExtension,
                StringUtils.hasText(contentType) ? contentType : "image/jpeg");
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
        BoardType boardType = request.getBoardType();
        validateText(request.getTitle(), request.getContent());
        Post post = Post.create(user, request.getTitle(), request.getContent(), boardType, now);
        switch (boardType) {
            case FREE -> {
                Post saved = postRepository.save(post);
                List<Long> imageIds = savePostImages(saved.getId(), request.getImageKeys(), now);
                return buildCreateResponse(saved, imageIds);
            }
            case PLAN_SHARE -> {
                Long planId = resolvePlanId(request);
                Trip plan = validateTrip(planId, user);
                post.setPlanInfo(planId);
                Post saved = postRepository.save(post);
                postedPlanRepository.save(new PostedPlan(saved, plan));
                return buildCreateResponse(saved, Collections.emptyList());
            }
            case PLACE_RECOMMEND -> {
                PlaceRecommendationPayload payload = validatePlaceRecommendation(request);
                post.setPlaceRecommendation(payload.placeName(), payload.rating());
                Post saved = postRepository.save(post);
                saveRecommendedPlace(saved, payload);
                return buildCreateResponse(saved, Collections.emptyList());
            }
            default -> throw new IllegalArgumentException("*지원하지 않는 게시판입니다.");
        }
    }

    private PostCreateResponse buildCreateResponse(Post saved, List<Long> imageIds) {
        return new PostCreateResponse(
                saved.getId(),
                saved.getBoardType(),
                saved.getTitle(),
                saved.getContent(),
                saved.getCreatedAt(),
                saved.getAuthor().getId(),
                imageIds
        );
    }

    private void saveRecommendedPlace(Post post, PlaceRecommendationPayload payload) {
        Long placeId = Objects.requireNonNull(payload.placeId(), "*장소를 선택해주세요.");
        postedPlaceRepository.save(
                new PostedPlace(post, placeId, payload.googlePlaceId(), payload.rating()));
    }

    private PlaceRecommendationPayload validatePlaceRecommendation(PostCreateRequest request) {
        Long placeId = request.getPlaceId();
        Integer rating = request.getRating();
        String placeName = request.getPlaceName();
        if (placeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*장소를 선택해주세요.");
        }
        if (placeName == null || placeName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*장소 이름을 입력해주세요.");
        }
        if (rating == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*별점을 선택해주세요.");
        }
        if (rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*별점은 1~5 사이여야 합니다.");
        }
        return new PlaceRecommendationPayload(placeId, request.getGooglePlaceId(), placeName, rating);
    }

    private PlaceRecommendationPayload validatePlaceRecommendationForUpdate(PostUpdateRequest request) {
        Long placeId = request.getPlaceId();
        Integer rating = request.getRating();
        String placeName = request.getPlaceName();
        if (placeId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*장소를 선택해주세요.");
        }
        if (placeName == null || placeName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*장소 이름을 입력해주세요.");
        }
        if (rating == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*별점을 선택해주세요.");
        }
        if (rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*별점은 1~5 사이여야 합니다.");
        }
        return new PlaceRecommendationPayload(placeId, request.getGooglePlaceId(), placeName, rating);
    }

    private record PlaceRecommendationPayload(Long placeId, String googlePlaceId, String placeName, Integer rating) {
    }

    private Long resolvePlanId(PostCreateRequest request) {
        Long resolved = request.getPlanId() != null ? request.getPlanId() : request.getTripId();
        if (resolved == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*연결할 일정을 선택해주세요.");
        }
        return resolved;
    }

    private Long resolvePlanIdForUpdate(PostUpdateRequest request) {
        Long resolved = request.getPlanId() != null ? request.getPlanId() : request.getTripId();
        if (resolved == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*연결할 일정을 선택해주세요.");
        }
        return resolved;
    }

    private void overridePlanShareImages(List<PostSummaryResponse> items) {
        if (items.isEmpty()) {
            return;
        }
        List<Long> postIds = items.stream()
                .map(PostSummaryResponse::getPostId)
                .collect(Collectors.toList());
        List<PostedPlanRepository.PostTripIdInfo> mappings = postedPlanRepository.findTripIdsByPostIds(postIds);
        if (mappings.isEmpty()) {
            return;
        }
        Map<Long, Long> postToTrip = mappings.stream()
                .collect(Collectors.toMap(
                        PostedPlanRepository.PostTripIdInfo::getPostId,
                        PostedPlanRepository.PostTripIdInfo::getTripId
                ));
        if (postToTrip.isEmpty()) {
            return;
        }
        LinkedHashSet<Long> tripIdOrder = new LinkedHashSet<>(postToTrip.values());
        List<Long> tripIds = new ArrayList<>(tripIdOrder);
        List<ItineraryItemPlaceRepository.TripPlaceInfo> tripPlaces =
                itineraryItemPlaceRepository.findFirstPlacesByTripIds(tripIds);
        if (tripPlaces.isEmpty()) {
            return;
        }
        Map<Long, Long> tripToPlace = new LinkedHashMap<>();
        for (ItineraryItemPlaceRepository.TripPlaceInfo info : tripPlaces) {
            tripToPlace.putIfAbsent(info.getTripId(), info.getPlaceId());
        }
        for (PostSummaryResponse item : items) {
            if (item.getRepresentativeImageUrl() != null) {
                continue;
            }
            Long tripId = postToTrip.get(item.getPostId());
            if (tripId == null || !tripToPlace.containsKey(tripId)) {
                continue;
            }
            item.setRepresentativeImageUrl(PLAN_SHARE_PLACEHOLDER_IMAGE);
        }
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
        List<Long> imageIds = Collections.emptyList();
        BoardType currentBoardType = post.getBoardType();
        switch (currentBoardType) {
            case FREE -> {
                deleteExistingPostImages(postId);
                imageIds = savePostImages(postId, request.getImageKeys(), now);
                post.setPlanInfo(null);
                post.setPlaceRecommendation(null, null);
                postedPlaceRepository.deleteByPostId(postId);
                postedPlanRepository.findByPostId(postId)
                        .ifPresent(postedPlanRepository::delete);
            }
            case PLAN_SHARE -> {
                Long planId = resolvePlanIdForUpdate(request);
                Trip plan = validateTripForUpdate(planId, postId, user);
                post.setPlanInfo(planId);
                post.setPlaceRecommendation(null, null);
                postedPlaceRepository.deleteByPostId(postId);
                Optional<PostedPlan> existingPlan = postedPlanRepository.findByPostId(postId);
                if (existingPlan.isPresent()) {
                    existingPlan.get().setTrip(plan);
                } else {
                    postedPlanRepository.save(new PostedPlan(post, plan));
                }
            }
            case PLACE_RECOMMEND -> {
                PlaceRecommendationPayload payload = validatePlaceRecommendationForUpdate(request);
                post.setPlanInfo(null);
                post.setPlaceRecommendation(payload.placeName(), payload.rating());
                postedPlanRepository.findByPostId(postId)
                        .ifPresent(postedPlanRepository::delete);
                postedPlaceRepository.deleteByPostId(postId);
                saveRecommendedPlace(post, payload);
            }
            default -> throw new IllegalArgumentException("*지원하지 않는 게시판입니다.");
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
        S3ObjectDeleter deleter = s3ObjectDeleterProvider.getIfAvailable();
        if (deleter == null) {
            log.info("S3 delete skip: S3ObjectDeleter not available (cloud.aws.enabled?), key={}", s3Key);
            return;
        }
        try {
            deleter.delete(s3Key);
            log.info("S3 object deleted: {}", s3Key);
        } catch (Exception e) {
            log.warn("S3 delete failed for key={}, continuing (DB already updated)", s3Key, e);
        }
    }

    private void validatePostImageKey(String key) {
        if (!key.startsWith("post/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*유효하지 않은 이미지 key입니다.");
        }
    }

    private void validateText(String title, String content) {
        if (!StringUtils.hasText(title)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*제목을 입력해주세요.");
        }
        if (!StringUtils.hasText(content)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "*내용을 입력해주세요.");
        }
    }

    private Trip validateTrip(Long tripId, User user) {
        if (tripId == null) {
            throw new BusinessException(ErrorCode.TRIP_NOT_FOUND);
        }
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
        if (!trip.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_TRIP_ACCESS);
        }
        return trip;
    }

    private Trip validateTripForUpdate(Long tripId, Long postId, User user) {
        if (tripId == null) {
            throw new BusinessException(ErrorCode.TRIP_NOT_FOUND);
        }
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
        if (!trip.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_TRIP_ACCESS);
        }
        return trip;
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
        LATEST,
        COMMENTS_1Y,
        LIKES_1Y
    }

}
