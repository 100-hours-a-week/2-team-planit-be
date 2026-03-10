package com.planit.domain.keywordalert.controller;

import com.planit.domain.keywordalert.dto.KeywordSubscriptionCreateRequest;
import com.planit.domain.keywordalert.dto.KeywordSubscriptionResponse;
import com.planit.domain.keywordalert.query.service.KeywordSubscriptionQueryService;
import com.planit.domain.keywordalert.service.KeywordSubscriptionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/keyword-subscriptions")
@RequiredArgsConstructor
public class KeywordSubscriptionController {

    private final KeywordSubscriptionService keywordSubscriptionService;
    private final KeywordSubscriptionQueryService keywordSubscriptionQueryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public KeywordSubscriptionResponse create(@AuthenticationPrincipal UserDetails principal,
                                              @Valid @RequestBody KeywordSubscriptionCreateRequest request) {
        return keywordSubscriptionService.create(requireLogin(principal), request);
    }

    @GetMapping
    public List<KeywordSubscriptionResponse> list(@AuthenticationPrincipal UserDetails principal) {
        return keywordSubscriptionQueryService.list(requireLogin(principal));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserDetails principal,
                       @PathVariable Long id) {
        keywordSubscriptionService.delete(requireLogin(principal), id);
    }

    private String requireLogin(UserDetails principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "*로그인이 필요한 요청입니다.");
        }
        return principal.getUsername();
    }
}
