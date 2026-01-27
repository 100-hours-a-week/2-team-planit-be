package com.planit.domain.comment.controller;

import com.planit.domain.comment.dto.CommentDetail;
import com.planit.domain.comment.dto.CommentRequest;
import com.planit.domain.comment.dto.CommentResponse;
import com.planit.domain.comment.service.CommentService;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public List<CommentDetail> listComments(@PathVariable Long postId) {
        return commentService.listComments(postId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(
        @PathVariable Long postId,
        @AuthenticationPrincipal UserDetails principal,
        @Valid @RequestBody CommentRequest request
    ) {
        String loginId = principal.getUsername();
        return commentService.addComment(postId, loginId, request);
    }
}
