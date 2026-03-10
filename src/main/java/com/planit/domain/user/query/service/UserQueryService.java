package com.planit.domain.user.query.service;

import com.planit.domain.user.dto.MyPageResponse;
import com.planit.domain.user.dto.PlanPreview;
import com.planit.domain.user.dto.UserAvailabilityResponse;
import com.planit.domain.user.dto.UserProfileResponse;
import com.planit.domain.user.query.projection.MyPageSummaryProjection;
import com.planit.domain.user.query.projection.UserProfileProjection;
import com.planit.domain.user.query.repository.UserQueryRepository;
import com.planit.infrastructure.storage.S3ImageUrlResolver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private static final int MY_PAGE_PREVIEW_SIZE = 3;

    private final UserQueryRepository userQueryRepository;
    private final S3ImageUrlResolver imageUrlResolver;

    public UserAvailabilityResponse checkLoginId(String loginId) {
        if (userQueryRepository.existsActiveByLoginId(loginId)) {
            return new UserAvailabilityResponse(false, "*중복된 아이디 입니다.");
        }
        return new UserAvailabilityResponse(true, "사용 가능한 아이디 입니다.");
    }

    public UserAvailabilityResponse checkNickname(String nickname) {
        if (userQueryRepository.existsActiveByNickname(nickname)) {
            return new UserAvailabilityResponse(false, "*중복된 닉네임 입니다.");
        }
        return new UserAvailabilityResponse(true, "사용 가능한 닉네임 입니다.");
    }

    public UserProfileResponse getProfile(String loginId) {
        UserProfileProjection profile = userQueryRepository.findProfileByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));
        return new UserProfileResponse(
                profile.getUserId(),
                profile.getLoginId(),
                profile.getNickname(),
                imageUrlResolver.resolve(profile.getProfileImageKey())
        );
    }

    public MyPageResponse getMyPage(String loginId) {
        MyPageSummaryProjection summary = userQueryRepository.findMyPageSummaryByLoginId(loginId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));

        List<PlanPreview> previews = userQueryRepository
                .findMyPagePostPreviewsByLoginId(loginId, MY_PAGE_PREVIEW_SIZE)
                .stream()
                .map(preview -> new PlanPreview(
                        preview.getPostId(),
                        preview.getTitle(),
                        "내 계획",
                        preview.getBoardType()
                ))
                .toList();

        return new MyPageResponse(
                summary.getUserId(),
                summary.getLoginId(),
                summary.getNickname(),
                imageUrlResolver.resolve(summary.getProfileImageKey()),
                summary.getPostCount(),
                summary.getCommentCount(),
                summary.getLikeCount(),
                summary.getNotificationCount(),
                previews
        );
    }
}
