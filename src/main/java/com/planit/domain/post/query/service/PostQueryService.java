package com.planit.domain.post.query.service;

import com.planit.domain.place.exception.PlaceSearchException;
import com.planit.domain.placeRecommendation.dto.PlaceRecommendationDetailResponse;
import com.planit.domain.placeRecommendation.service.PlaceRecommendationService;
import com.planit.domain.post.dto.PostDetailResponse;
import com.planit.domain.post.dto.PostSummaryResponse;
import com.planit.domain.post.entity.BoardType;
import com.planit.domain.post.query.projection.PostDetailProjection;
import com.planit.domain.post.query.projection.PostSummaryProjection;
import com.planit.domain.post.query.repository.PostQueryRepository;
import com.planit.domain.post.stats.service.PostStatsAggregationService;
import com.planit.global.common.response.PageResponse;
import com.planit.global.config.PageablePolicy;
import com.planit.infrastructure.storage.S3ImageUrlResolver;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class PostQueryService {
    private static final Logger log = LoggerFactory.getLogger(PostQueryService.class);
    private static final String PLAN_SHARE_PLACEHOLDER_IMAGE = "/images/plan-share-default.png";
    private static final int COMMENT_PAGE_SIZE = 20;

    private final PostQueryRepository postQueryRepository;
    private final S3ImageUrlResolver imageUrlResolver;
    private final PlaceRecommendationService placeRecommendationService;
    private final PostStatsAggregationService postStatsAggregationService;

    public PostQueryService(
            PostQueryRepository postQueryRepository,
            S3ImageUrlResolver imageUrlResolver,
            PlaceRecommendationService placeRecommendationService,
            PostStatsAggregationService postStatsAggregationService
    ) {
        this.postQueryRepository = postQueryRepository;
        this.imageUrlResolver = imageUrlResolver;
        this.placeRecommendationService = placeRecommendationService;
        this.postStatsAggregationService = postStatsAggregationService;
    }

    public PageResponse<PostSummaryResponse> getPostSummaries(
            BoardType boardType,
            String keyword,
            Pageable pageable
    ) {
        Pageable safePageable = PageablePolicy.clamp(pageable, Sort.by(Sort.Direction.DESC, "created_at"));
        String normalizedSearch = keyword == null ? "" : keyword;
        Page<PostSummaryProjection> result = postQueryRepository.findPostSummaries(
                boardType.name(),
                normalizedSearch,
                safePageable
        );
        Page<PostSummaryResponse> mapped = result.map(summary -> {
            String thumbnailUrl = null;
            if (summary.getRepresentativeImageKey() != null) {
                thumbnailUrl = imageUrlResolver.resolve(summary.getRepresentativeImageKey());
            }
            String placeImageUrl = resolvePlaceListImageUrl(summary);
            if (thumbnailUrl == null
                    && summary.getBoardType() == BoardType.PLACE_RECOMMEND
                    && StringUtils.hasText(placeImageUrl)) {
                thumbnailUrl = placeImageUrl;
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
                    placeImageUrl,
                    summary.getPlaceName(),
                    summary.getTripTitle()
            );
        });
        if (boardType == BoardType.PLAN_SHARE) {
            overridePlanShareImages(mapped.getContent());
        }
        return PageResponse.from(mapped);
    }

    @Transactional
    public PostDetailResponse getPostDetail(Long postId, String loginId) {
        Long requesterId = resolveRequesterId(loginId);
        PostDetailProjection projection = postQueryRepository.findPostDetail(postId, requesterId == null ? -1L : requesterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."));
        postStatsAggregationService.increaseViewCount(postId);
        return enrichPlaceDetail(toDetailResponse(projection, requesterId));
    }

    private void overridePlanShareImages(List<PostSummaryResponse> items) {
        if (items.isEmpty()) {
            return;
        }
        List<Long> postIds = items.stream().map(PostSummaryResponse::getPostId).toList();
        List<PostQueryRepository.PostTripIdProjection> mappings = postQueryRepository.findTripIdsByPostIds(postIds);
        if (mappings.isEmpty()) {
            return;
        }
        Map<Long, Long> postToTrip = new LinkedHashMap<>();
        for (PostQueryRepository.PostTripIdProjection mapping : mappings) {
            postToTrip.put(mapping.getPostId(), mapping.getTripId());
        }
        if (postToTrip.isEmpty()) {
            return;
        }
        List<Long> tripIds = new ArrayList<>(new LinkedHashSet<>(postToTrip.values()));
        List<PostQueryRepository.TripPlaceProjection> tripPlaces = postQueryRepository.findFirstPlacesByTripIds(tripIds);
        if (tripPlaces.isEmpty()) {
            return;
        }
        Map<Long, Long> tripToPlace = new LinkedHashMap<>();
        for (PostQueryRepository.TripPlaceProjection info : tripPlaces) {
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

    private PostDetailResponse toDetailResponse(PostDetailProjection projection, Long requesterId) {
        PostDetailResponse.AuthorInfo author = new PostDetailResponse.AuthorInfo(
                projection.getAuthorId(),
                projection.getAuthorNickname(),
                imageUrlResolver.resolveOrNull(projection.getAuthorProfileImageKey())
        );
        List<PostDetailResponse.PostImage> images = postQueryRepository.findPostImages(projection.getPostId())
                .stream()
                .map(image -> new PostDetailResponse.PostImage(
                        image.getImageId(),
                        image.getS3Key(),
                        imageUrlResolver.resolveOrNull(image.getS3Key())
                ))
                .toList();
        List<PostDetailResponse.CommentInfo> comments = postQueryRepository.findPostComments(projection.getPostId(), COMMENT_PAGE_SIZE)
                .stream()
                .map(comment -> new PostDetailResponse.CommentInfo(
                        comment.getCommentId(),
                        comment.getAuthorId(),
                        comment.getAuthorNickname(),
                        imageUrlResolver.resolve(comment.getAuthorProfileImageKey()),
                        comment.getContent(),
                        comment.getCreatedAt(),
                        requesterId != null && requesterId.equals(comment.getAuthorId())
                ))
                .toList();
        boolean editable = requesterId != null && requesterId.equals(projection.getAuthorId());

        return new PostDetailResponse(
                projection.getPostId(),
                toBoardName(projection.getBoardType()),
                toBoardDescription(projection.getBoardType()),
                projection.getTitle(),
                projection.getContent(),
                projection.getCreatedAt(),
                author,
                images,
                projection.getLikeCount() == null ? 0 : projection.getLikeCount().intValue(),
                projection.getCommentCount() == null ? 0 : projection.getCommentCount().intValue(),
                projection.getLikedByRequester() != null && projection.getLikedByRequester() == 1,
                comments,
                editable,
                projection.getPlaceName(),
                projection.getGooglePlaceId(),
                null,
                projection.getPlaceCity(),
                projection.getPlaceCountry(),
                projection.getPlaceRating(),
                projection.getPlanTripId(),
                projection.getPlanTripId(),
                projection.getTripTitle(),
                resolvePlanThumbnailUrl(projection.getPlanThumbnailKey())
        );
    }

    private String toBoardName(BoardType boardType) {
        return switch (boardType) {
            case FREE -> "자유게시판";
            case PLAN_SHARE -> "일정 공유";
            case PLACE_RECOMMEND -> "장소 추천";
        };
    }

    private String toBoardDescription(BoardType boardType) {
        return switch (boardType) {
            case FREE -> "자유롭게 이야기하는 공간";
            case PLAN_SHARE -> "함께 일정 계획을 공유";
            case PLACE_RECOMMEND -> "좋은 장소 추천하기";
        };
    }

    private String resolvePlanThumbnailUrl(String s3Key) {
        if (!StringUtils.hasText(s3Key)) {
            return null;
        }
        return imageUrlResolver.resolveOrNull(s3Key);
    }

    private String resolvePlaceListImageUrl(PostSummaryProjection summary) {
        if (StringUtils.hasText(summary.getPlaceImageUrl())) {
            return summary.getPlaceImageUrl();
        }
        if (summary.getBoardType() != BoardType.PLACE_RECOMMEND) {
            return null;
        }
        if (!StringUtils.hasText(summary.getGooglePlaceId())) {
            return null;
        }
        try {
            return placeRecommendationService.getPlaceDetail(summary.getGooglePlaceId()).getPhotoUrl();
        } catch (PlaceSearchException ex) {
            log.warn("place list image lookup failed for placeId={} reason={}",
                    summary.getGooglePlaceId(), ex.getMessage());
            return null;
        }
    }

    private Long resolveRequesterId(String loginId) {
        if (!StringUtils.hasText(loginId)) {
            return null;
        }
        Optional<Long> userId = postQueryRepository.findActiveUserIdByLoginId(loginId);
        return userId.orElse(null);
    }

    private PostDetailResponse enrichPlaceDetail(PostDetailResponse detail) {
        String placeId = detail.getGooglePlaceId();
        if (!StringUtils.hasText(placeId)) {
            return detail;
        }
        try {
            PlaceRecommendationDetailResponse placeDetail = placeRecommendationService.getPlaceDetail(placeId);
            return detail.withPlaceDetail(placeDetail);
        } catch (PlaceSearchException ex) {
            log.warn("place detail lookup failed for placeId={} reason={}", placeId, ex.getMessage());
            return detail;
        }
    }
}
