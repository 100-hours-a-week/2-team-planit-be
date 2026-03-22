package com.planit.domain.user.controller; // 사용자 컨트롤러 테스트 패키지

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planit.domain.user.dto.UserProfileResponse;
import com.planit.domain.user.query.service.UserQueryService;
import com.planit.domain.user.repository.UserRepository;
import com.planit.domain.user.security.JwtProvider;
import com.planit.global.common.exception.GlobalExceptionHandler;
import com.planit.global.config.SecurityConfig;
import com.planit.domain.user.service.UserService;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = UserController.class) // UserController 단위 테스트
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc; // mock HTTP 요청 도구

    @Autowired
    private ObjectMapper objectMapper; // JSON 직렬화 도구

    @MockBean
    private UserService userService; // 실제 서비스는 모킹

    @MockBean
    private UserQueryService userQueryService;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName("대문자 포함 아이디 중복 체크 시 스펙 메시지 반환")
    void checkLoginIdRejectsUppercase() throws Exception {
        mockMvc.perform(get("/users/check-login-id")
                .param("loginId", "Planit123")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("비밀번호 확인이 다르면 오류 메시지 반환")
    void signupRejectsPasswordMismatch() throws Exception {
        Map<String, Object> request = Map.of(
            "loginId", "planit_user",
            "password", "Correct1!",
            "passwordConfirm", "Wrong1!",
            "nickname", "planitter"
        );

        mockMvc.perform(post("/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.errors[0].reason").value("*비밀번호가 다릅니다."));
    }

    @Test
    @DisplayName("Validation 오류는 errors 배열에 필드 이름과 메시지를 담아 반환")
    void validationErrorIncludesFieldList() throws Exception {
        Map<String, Object> request = Map.of(
            "password", "Correct1!",
            "passwordConfirm", "Correct1!",
            "nickname", "Planitter"
        );

        mockMvc.perform(post("/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.errors[0].field").value("loginId"))
            .andExpect(jsonPath("$.errors[0].reason").value("*아이디를 입력해주세요."));
    }

    @Test
    @DisplayName("내 정보 조회는 인증되면 사용자 정보를 내려준다")
    @WithMockUser(username = "planit_user")
    void meReturnsProfileWhenAuthenticated() throws Exception {
        when(userQueryService.getProfile("planit_user"))
            .thenReturn(new UserProfileResponse(5L, "planit_user", "planitter", null)); // 인증된 사용자 반환 데이터 모킹

        mockMvc.perform(get("/users/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(5))
            .andExpect(jsonPath("$.loginId").value("planit_user"))
            .andExpect(jsonPath("$.nickname").value("planitter"));
    }

    @Test
    @DisplayName("내 정보 조회는 인증 없으면 401")
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/users/me"))
            .andExpect(status().isUnauthorized());
    }
}
