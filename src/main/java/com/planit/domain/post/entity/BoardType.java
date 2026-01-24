package com.planit.domain.post.entity;

/**
 * 게시판 구분을 enum으로 관리함으로써 DB의 문자열 board_type과 안전하게 매핑합니다.
 */
public enum BoardType {
    FREE, // 자유게시판
    PLAN_SHARE, // 여행 일정 공유 게시판
    PLACE_RECOMMEND // 장소 추천 게시판
}
