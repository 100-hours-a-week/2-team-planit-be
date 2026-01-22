package com.planit.domain.user.controller;

import com.planit.domain.user.dto.TokenValidationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthVerificationController {

    @GetMapping("/verify")
    public TokenValidationResponse verify(Authentication authentication) {
        return new TokenValidationResponse(authentication.getName(), "*토큰이 유효합니다.");
    }
}
