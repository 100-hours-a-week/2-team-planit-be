package com.planit.domain.post.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.planit.domain.common.repository.ImageRepository;
import com.planit.domain.post.dto.PostCreateRequest;
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
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

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

    @Test // PLAN_SHARE 정상 저장 시
    void PLAN_SHARE_정상저장() {
        PostCreateRequest request = new PostCreateRequest(
                "title",
                "content",
                Collections.emptyList(),
                BoardType.PLAN_SHARE,
                99L,
                Collections.emptyList()
        );
        Post savedPost = Post.create(user, request.getTitle(), request.getContent(), BoardType.PLAN_SHARE, LocalDateTime.now());
        savedPost.setId(50L);
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);
        when(postedPlanRepository.existsByPostId(50L)).thenReturn(false);
        when(postedPlanRepository.existsByTripId(99L)).thenReturn(false);
        Trip trip = new Trip(user, "trip", LocalDate.now(), LocalDate.now(), null, null, "city", 0);
        when(tripRepository.findById(99L)).thenReturn(Optional.of(trip));

        postService.createPost(request, "tester");

        verify(postedPlanRepository).save(any(PostedPlan.class));
    }

    @Test // 일정이 없으면 TRIP_NOT_FOUND
    void 존재하지않는_trip_예외() {
        PostCreateRequest request = new PostCreateRequest(
                "title",
                "content",
                Collections.emptyList(),
                BoardType.PLAN_SHARE,
                11L,
                Collections.emptyList()
        );
        Post savedPost = Post.create(user, request.getTitle(), request.getContent(), BoardType.PLAN_SHARE, LocalDateTime.now());
        savedPost.setId(61L);
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);
        when(postedPlanRepository.existsByPostId(61L)).thenReturn(false);
        when(tripRepository.findById(11L)).thenReturn(Optional.empty());

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> postService.createPost(request, "tester"));
        Assertions.assertEquals(ErrorCode.TRIP_NOT_FOUND, exception.getErrorCode());
        verify(postedPlanRepository, never()).save(any(PostedPlan.class));
    }

    @Test // 타인 여행 접근 시 FORBIDDEN_TRIP_ACCESS
    void 타인이_소유한_trip_예외() {
        PostCreateRequest request = new PostCreateRequest(
                "title",
                "content",
                Collections.emptyList(),
                BoardType.PLAN_SHARE,
                22L,
                Collections.emptyList()
        );
        Post savedPost = Post.create(user, request.getTitle(), request.getContent(), BoardType.PLAN_SHARE, LocalDateTime.now());
        savedPost.setId(62L);
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);
        when(postedPlanRepository.existsByPostId(62L)).thenReturn(false);
        Trip trip = new Trip(new User(), "foreign", LocalDate.now(), LocalDate.now(), null, null, "city", 0);
        trip.getUser().setId(999L);
        when(tripRepository.findById(22L)).thenReturn(Optional.of(trip));

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> postService.createPost(request, "tester"));
        Assertions.assertEquals(ErrorCode.FORBIDDEN_TRIP_ACCESS, exception.getErrorCode());
    }

    @Test // 이미 공유된 trip일 경우 TRIP_ALREADY_SHARED
    void 이미_공유된_trip_예외() {
        PostCreateRequest request = new PostCreateRequest(
                "title",
                "content",
                Collections.emptyList(),
                BoardType.PLAN_SHARE,
                33L,
                Collections.emptyList()
        );
        Post savedPost = Post.create(user, request.getTitle(), request.getContent(), BoardType.PLAN_SHARE, LocalDateTime.now());
        savedPost.setId(63L);
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);
        when(postedPlanRepository.existsByPostId(63L)).thenReturn(false);
        Trip trip = new Trip(user, "trip", LocalDate.now(), LocalDate.now(), null, null, "city", 0);
        when(tripRepository.findById(33L)).thenReturn(Optional.of(trip));
        when(postedPlanRepository.existsByTripId(33L)).thenReturn(true);

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> postService.createPost(request, "tester"));
        Assertions.assertEquals(ErrorCode.TRIP_ALREADY_SHARED, exception.getErrorCode());
    }
}
