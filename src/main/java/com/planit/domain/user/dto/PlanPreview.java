package com.planit.domain.user.dto;

/**
 * 마이페이지에서 계획 미리보기 정보를 담는 DTO.
 */
public record PlanPreview(
    Long postId,
    String title,
    String status,
    String boardType
) {}
