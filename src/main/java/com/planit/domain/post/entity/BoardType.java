package com.planit.domain.post.entity;

/**
 * 게시판 구분을 enum으로 관리함으로써 DB의 문자열 board_type과 안전하게 매핑합니다.
 */

public enum BoardType {
    FREE, // 자유게시판 (v1 기본)
    PLAN_SHARE, // 여행 일정 공유 화면(추후 확장)
    PLACE_RECOMMEND // 장소 추천 콘텐츠(확장 예정)
}
