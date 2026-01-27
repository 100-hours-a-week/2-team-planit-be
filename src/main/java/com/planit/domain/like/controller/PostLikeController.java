package com.planit.domain.like.controller;

import com.planit.domain.like.dto.PostLikeResponse;
import com.planit.domain.like.service.PostLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/{postId}/likes")
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeService likeService;

    @GetMapping
    @Operation(
        summary = "게시글 좋아요 정보 조회",
        description = "현재 게시글의 좋아요 총 개수와 요청한 사용자가 좋아요를 눌렀는지를 함께 반환합니다."
    )
    public PostLikeResponse getLikeInfo(
        @PathVariable Long postId,
        @AuthenticationPrincipal UserDetails principal
    ) {
        String loginId = principal != null ? principal.getUsername() : null;
        return likeService.getPostLikeInfo(postId, loginId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "게시글 좋아요 토글",
        description = "로그인한 사용자가 해당 게시글의 좋아요/좋아요 취소를 수행합니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public void toggleLike(
        @PathVariable Long postId,
        @AuthenticationPrincipal UserDetails principal
    ) {
        String loginId = principal.getUsername();
        likeService.toggleLike(postId, loginId);
    }
}
