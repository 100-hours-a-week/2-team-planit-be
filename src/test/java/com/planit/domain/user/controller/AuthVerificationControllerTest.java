package com.planit.domain.user.controller; // 사용자 인증 컨트롤러 테스트 패키지

import static org.mockito.Mockito.when; // Mockito 서브 동작 설정
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import com.planit.domain.user.security.JwtAuthenticationFilter;
import com.planit.domain.user.security.JwtProvider;
import com.planit.global.common.exception.GlobalExceptionHandler;
import com.planit.global.config.SecurityConfig;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthVerificationController.class) // 컨트롤러 단위 테스트
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtProvider.class, JwtAuthenticationFilter.class})
class AuthVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc; // MockMvc로 요청 시뮬레이션

    @Autowired
    private JwtProvider jwtProvider; // 실제 JWT 생성 기능 사용

    @MockBean
    private UserRepository userRepository; // 레포지토리 의존성 mocking

    private User user; // 테스트용 사용자 엔티티

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setLoginId("planit_user");
        user.setPassword("encoded");
        user.setNickname("planitter");
        user.setDeleted(false);
        when(userRepository.findByLoginIdAndDeletedFalse(user.getLoginId()))
            .thenReturn(Optional.of(user)); // JWT 필터에서 조회 가능하도록 mocking
    }

    @Test
    @DisplayName("유효한 JWT가 있으면 로그인 ID와 메시지가 내려온다")
    void verifyReturnsLoginIdWhenTokenValid() throws Exception {
        String token = jwtProvider.generateToken(user.getLoginId()); // JWT 발급

        mockMvc.perform(get("/auth/verify")
                .header("Authorization", "Bearer " + token)) // Bearer 토큰 추가
            .andExpect(status().isOk()) // 200 OK
            .andExpect(jsonPath("$.loginId").value("planit_user")) // loginId 응답 확인
            .andExpect(jsonPath("$.message").value("*토큰이 유효합니다.")); // helper 텍스트 검증
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증 입력 메시지 반환")
    void verifyRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/auth/verify")) // 헤더 없이 호출
            .andExpect(status().isUnauthorized()) // 401 발생
            .andExpect(jsonPath("$.message").value("*로그인이 필요한 요청입니다.")); // 인증 entry point 메시지
    }
}
