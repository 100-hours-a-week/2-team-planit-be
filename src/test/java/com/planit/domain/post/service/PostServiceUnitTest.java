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
import com.planit.domain.trip.entity.Trip;
import com.planit.domain.trip.repository.TripRepository;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
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

    @Test // PLAN_SHARE 정상 저장 시 동작 검증
    void PLAN_SHARE_포스트_정상저장() {
        PostCreateRequest request = new PostCreateRequest(
                "title",
                "content",
                Collections.emptyList(),
                BoardType.PLAN_SHARE,
                99L,
                Collections.emptyList()
        ); // 포스트 요청 구성
        Post savedPost = Post.create(user, request.getTitle(), request.getContent(), BoardType.PLAN_SHARE, LocalDateTime.now());
        savedPost.setId(50L); // 저장 후 ID 설정
        when(postRepository.save(any(Post.class))).thenReturn(savedPost); // 저장 시 반환
        when(postedPlanRepository.existsByPostId(50L)).thenReturn(false); // 중복 없음
        Trip trip = new Trip(user, "trip", LocalDate.now(), LocalDate.now(), null, null, "city", 0);
        when(tripRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.of(trip)); // 일정 존재

        postService.createPost(request, "tester"); // 호출

        verify(postedPlanRepository).save(any(PostedPlan.class)); // 일정 매핑 저장 검증
    }

    @Test // 이미 연결됐을 경우 409 예외 발생
    void PLAN_SHARE_postId_이미존재하면_CONFLICT() {
        PostCreateRequest request = new PostCreateRequest(
                "title",
                "content",
                Collections.emptyList(),
                BoardType.PLAN_SHARE,
                55L,
                Collections.emptyList()
        );
        Post savedPost = Post.create(user, request.getTitle(), request.getContent(), BoardType.PLAN_SHARE, LocalDateTime.now());
        savedPost.setId(60L);
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);
        when(postedPlanRepository.existsByPostId(60L)).thenReturn(true); // 중복 존재

        ResponseStatusException exception = Assertions.assertThrows(ResponseStatusException.class,
                () -> postService.createPost(request, "tester"));
        Assertions.assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(postedPlanRepository, never()).save(any(PostedPlan.class)); // 저장이 호출되지 않음
    }
}
