package com.planit.domain.user.dto; // 사용자 프로필 응답을 정의한 DTO 패키지

import lombok.Getter; // Getter 자동 생성
import lombok.RequiredArgsConstructor; // final 필드용 생성자 자동 생성

import java.util.Collections;
import java.util.List;

@Getter
public class UserProfileResponse {
    private final Long userId; // 사용자를 고유 식별하는 PK
    private final String loginId; // 로그인/아이디 helper text 기준 필드
    private final String nickname; // 마이페이지에 노출되는 닉네임
    private final Long profileImageId; // 등록된 경우 프로필 이미지 식별자
    private final boolean hasProfileImage; // 이미지가 존재하는지 여부 플래그
    private final List<PlanCard> planHistory; // 마이페이지 내 계획 카드 목록

    public UserProfileResponse(Long userId, String loginId, String nickname) {
        this(userId, loginId, nickname, null, false, Collections.emptyList());
    }

    public UserProfileResponse(
        Long userId,
        String loginId,
        String nickname,
        Long profileImageId,
        boolean hasProfileImage,
        List<PlanCard> planHistory
    ) {
        this.userId = userId;
        this.loginId = loginId;
        this.nickname = nickname;
        this.profileImageId = profileImageId;
        this.hasProfileImage = hasProfileImage;
        this.planHistory = planHistory == null ? Collections.emptyList() : List.copyOf(planHistory); // 불변 리스트 유지
    }

    @Getter
    public static class PlanCard {
        private final Long planId; // 카드 클릭 시 이동할 여행 ID
        private final String title; // 여행 제목 (1줄 말줄임)
        private final String status; // 상태 메시지 (예: 내 계획 - 방금 전)
        private final boolean deletable; // 그룹장 여부에 따라 삭제 아이콘 노출 여부

        public PlanCard(Long planId, String title, String status, boolean deletable) {
            this.planId = planId;
            this.title = title;
            this.status = status;
            this.deletable = deletable;
        }
    }
}
