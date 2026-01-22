package com.planit.domain.user.service;

import com.planit.domain.user.dto.LoginRequest;
import com.planit.domain.user.dto.LoginResponse;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public LoginResponse login(@Valid LoginRequest request) {
        User user = userRepository.findByLoginIdAndDeletedFalse(request.getLoginId())
            .orElseThrow(this::credentialsInvalid);

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw credentialsInvalid();
        }
        String token = jwtProvider.generateToken(user.getLoginId());
        return new LoginResponse(user.getId(), user.getLoginId(), user.getNickname(), token);
    }

    private ResponseStatusException credentialsInvalid() {
        return new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "*아이디 또는 비밀번호를 확인해주세요"
        );
    }
}
