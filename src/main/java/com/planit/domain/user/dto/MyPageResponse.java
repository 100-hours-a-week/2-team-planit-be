package com.planit.domain.user.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 마이페이지 응답 데이터.
 * 프로필 정보와 활동 요약/계획 미리보기를 포함.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MyPageResponse {
    private Long userId;
    private String loginId;
    private String nickname;
    private String profileImageUrl;
    private Long postCount;
    private Long commentCount;
    private Long likeCount;
    private Long notificationCount;
    private List<PlanPreview> planPreviews;
}
