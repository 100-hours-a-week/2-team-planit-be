package com.planit.domain.post.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.planit.domain.common.repository.ImageRepository;
import com.planit.domain.place.exception.PlaceSearchException;
import com.planit.domain.place.repository.PlaceRepository;
import com.planit.domain.placeRecommendation.dto.PlaceRecommendationDetailResponse;
import com.planit.domain.placeRecommendation.service.PlaceRecommendationService;
import com.planit.domain.post.dto.PostCreateRequest;
import com.planit.domain.post.dto.PostCreateResponse;
import com.planit.domain.post.dto.PostDetailResponse;
import com.planit.domain.post.entity.BoardType;
import com.planit.domain.post.entity.Post;
import com.planit.domain.post.entity.PostedPlan;
import com.planit.domain.post.repository.PostRepository;
import com.planit.domain.post.repository.PostedImageRepository;
import com.planit.domain.post.repository.PostedPlanRepository;
import com.planit.domain.post.service.ImageStorageService;
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.repository.TripRepository;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import com.planit.global.common.exception.BusinessException;
import com.planit.global.common.exception.ErrorCode;
import com.planit.infrastructure.storage.S3ImageUrlResolver;
import com.planit.infrastructure.storage.S3ObjectDeleter;
import com.planit.infrastructure.storage.S3PresignedUrlService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class) // Mockito 확장으로 모의 객체 주입
class PostServiceUnitTest {

    @Mock // PostRepository를 목 객체로 주입
    private PostRepository postRepository;
    @Mock // UserRepository 목
    private UserRepository userRepository;
    @Mock // ImageStorageService 목
    private ImageStorageService imageStorageService;
    @Mock // PostedImageRepository 목
    private PostedImageRepository postedImageRepository;
    @Mock // ImageRepository 목
    private ImageRepository imageRepository;
    @Mock // PresignedUrlService 공급자 목
    private ObjectProvider<S3PresignedUrlService> presignedUrlServiceProvider;
    @Mock // ObjectProvider<S3ObjectDeleter> 목
    private ObjectProvider<S3ObjectDeleter> s3ObjectDeleterProvider;
    @Mock // S3ImageUrlResolver 목
    private S3ImageUrlResolver imageUrlResolver;
    @Mock // TripRepository 목
    private TripRepository tripRepository;
    @Mock // PostedPlanRepository 목
    private PostedPlanRepository postedPlanRepository;
    @Mock // PlaceRecommendationService 목
    private PlaceRecommendationService placeRecommendationService;
    @Mock // PlaceRepository 목
    private PlaceRepository placeRepository;

    @InjectMocks // 위 목들을 조합하여 PostService 생성
    private PostService postService;

    private User user; // 공통으로 사용할 사용자

    @BeforeEach // 각 테스트 전 실행
    void setUp() {
        user = new User(); // 테스트 유저 생성
        user.setId(1L);
        user.setLoginId("tester");
        user.setPassword("pw");
        user.setNickname("tester");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(false);
        when(userRepository.findByLoginIdAndDeletedFalse("tester")).thenReturn(Optional.of(user)); // resolveRequester에서 반환
    }

    private PostCreateRequest buildRequest(BoardType boardType) {
        PostCreateRequest request = new PostCreateRequest();
        request.setTitle("title");
        request.setContent("content");
        request.setBoardType(boardType);
        request.setImageKeys(Collections.emptyList());
        return request;
    }

    private PostDetailResponse buildPostDetail(String googlePlaceId) {
        return new PostDetailResponse(
                1L,
                "board",
                "desc",
                "title",
                "content",
                LocalDateTime.now(),
                new PostDetailResponse.AuthorInfo(1L, "tester", null),
                Collections.emptyList(),
                0,
                0,
                false,
                Collections.emptyList(),
                false,
                "placeName",
                googlePlaceId,
                null,
                null,
                null,
                4,
                10L,
                20L,
                "trip",
                null
        );
    }

    @Test // PLAN_SHARE 정상 저장 시
    void PLAN_SHARE_정상저장() {
        PostCreateRequest request = buildRequest(BoardType.PLAN_SHARE);
        request.setPlanId(99L);
        Post savedPost = Post.create(user, request.getTitle(), request.getContent(), BoardType.PLAN_SHARE, LocalDateTime.now());
        savedPost.setId(50L);
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);
        Trip trip = new Trip(user, "trip", LocalDate.now(), LocalDate.now(), null, null, "city", 0);
        when(tripRepository.findById(99L)).thenReturn(Optional.of(trip));

        postService.createPost(request, "tester");

        verify(postedPlanRepository).save(any(PostedPlan.class));
    }

    @Test // 일정이 없으면 TRIP_NOT_FOUND
    void 존재하지않는_trip_예외() {
        PostCreateRequest request = buildRequest(BoardType.PLAN_SHARE);
        request.setPlanId(11L);
        when(tripRepository.findById(11L)).thenReturn(Optional.empty());

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> postService.createPost(request, "tester"));
        Assertions.assertEquals(ErrorCode.TRIP_NOT_FOUND, exception.getErrorCode());
        verify(postedPlanRepository, never()).save(any(PostedPlan.class));
    }

    @Test // 타인 여행 접근 시 FORBIDDEN_TRIP_ACCESS
    void 타인이_소유한_trip_예외() {
        PostCreateRequest request = buildRequest(BoardType.PLAN_SHARE);
        request.setPlanId(22L);
        Trip trip = new Trip(new User(), "foreign", LocalDate.now(), LocalDate.now(), null, null, "city", 0);
        trip.getUser().setId(999L);
        when(tripRepository.findById(22L)).thenReturn(Optional.of(trip));

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> postService.createPost(request, "tester"));
        Assertions.assertEquals(ErrorCode.FORBIDDEN_TRIP_ACCESS, exception.getErrorCode());
    }

    @Test
    void 자유게시판_정상저장() {
        PostCreateRequest request = buildRequest(BoardType.FREE);
        request.setImageKeys(List.of("post/1/img"));
        Post savedPost = Post.create(user, request.getTitle(), request.getContent(), BoardType.FREE, LocalDateTime.now());
        savedPost.setId(100L);
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);
        when(imageStorageService.storeByS3Key("post/1/img")).thenReturn(10L);

        PostCreateResponse response = postService.createPost(request, "tester");

        Assertions.assertNotNull(response);
        verify(imageStorageService).storeByS3Key("post/1/img");
    }

    @Test
    void 제목_없음_예외() {
        PostCreateRequest request = buildRequest(BoardType.FREE);
        request.setTitle("");

        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class,
                () -> postService.createPost(request, "tester"));
        Assertions.assertEquals("*제목을 입력해주세요.", exception.getReason());
    }

    @Test
    void 내용_없음_예외() {
        PostCreateRequest request = buildRequest(BoardType.FREE);
        request.setContent("");

        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class,
                () -> postService.createPost(request, "tester"));
        Assertions.assertEquals("*내용을 입력해주세요.", exception.getReason());
    }

    @Test
    void PLAN_SHARE_planId_없음_예외() {
        PostCreateRequest request = buildRequest(BoardType.PLAN_SHARE);
        request.setPlanId(null);
        request.setTripId(null);

        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class,
                () -> postService.createPost(request, "tester"));
        Assertions.assertEquals("*연결할 일정을 선택해주세요.", exception.getReason());
    }

    @Test
    void PLACE_RECOMMEND_rating0_예외() {
        PostCreateRequest request = buildRequest(BoardType.PLACE_RECOMMEND);
        request.setPlaceName("Place");
        request.setPlaceId(5L);
        request.setUserRating(0);
        request.setGooglePlaceId("place:5");

        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class,
                () -> postService.createPost(request, "tester"));
        Assertions.assertEquals("*별점은 1~5 사이여야 합니다.", exception.getReason());
    }

    @Test
    void PLACE_RECOMMEND_rating6_예외() {
        PostCreateRequest request = buildRequest(BoardType.PLACE_RECOMMEND);
        request.setPlaceName("Place");
        request.setPlaceId(5L);
        request.setUserRating(6);
        request.setGooglePlaceId("place:5");

        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class,
                () -> postService.createPost(request, "tester"));
        Assertions.assertEquals("*별점은 1~5 사이여야 합니다.", exception.getReason());
    }

    @Test
    void PLACE_RECOMMEND_googlePlaceId_없음_예외() {
        PostCreateRequest request = buildRequest(BoardType.PLACE_RECOMMEND);
        request.setPlaceName("Place");
        request.setPlaceId(5L);
        request.setUserRating(3);
        request.setGooglePlaceId("");

        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class,
                () -> postService.createPost(request, "tester"));
        Assertions.assertEquals("*Google place ID를 입력해주세요.", exception.getReason());
    }

    @Test
    void PLACE_RECOMMEND_rating_없음_예외() {
        PostCreateRequest request = buildRequest(BoardType.PLACE_RECOMMEND);
        request.setPlaceName("Place");
        request.setPlaceId(5L);
        request.setGooglePlaceId("place:5");

        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class,
                () -> postService.createPost(request, "tester"));
        Assertions.assertEquals("*별점을 선택해주세요.", exception.getReason());
    }

    @Test
    void PLACE_RECOMMEND_detail_enriches_place_detail() {
        PostDetailResponse raw = buildPostDetail("place:123");
        when(postRepository.findDetailById(44L, 1L)).thenReturn(Optional.of(raw));
        PlaceRecommendationDetailResponse placeDetail =
                new PlaceRecommendationDetailResponse("place:123", "name", "Seoul", "Korea", 37.5, 127.0, "photo", "maps");
        when(placeRecommendationService.getPlaceDetail("place:123")).thenReturn(placeDetail);

        PostDetailResponse response = postService.getPostDetail(44L, "tester");

        Assertions.assertEquals("photo", response.getPlaceImageUrl());
        Assertions.assertEquals("Seoul", response.getCity());
        Assertions.assertEquals("Korea", response.getCountry());
        verify(placeRecommendationService).getPlaceDetail("place:123");
    }

    @Test
    void PLACE_RECOMMEND_detail_fallbacks_when_place_detail_unavailable() {
        PostDetailResponse raw = buildPostDetail("place:123");
        when(postRepository.findDetailById(55L, 1L)).thenReturn(Optional.of(raw));
        when(placeRecommendationService.getPlaceDetail("place:123"))
                .thenThrow(new PlaceSearchException(ErrorCode.PLACE_003, HttpStatus.NOT_FOUND, "fail"));

        PostDetailResponse response = postService.getPostDetail(55L, "tester");

        Assertions.assertNull(response.getPlaceImageUrl());
        Assertions.assertNull(response.getCity());
        Assertions.assertNull(response.getCountry());
        verify(placeRecommendationService).getPlaceDetail("place:123");
    }

}
