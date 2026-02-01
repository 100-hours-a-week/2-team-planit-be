package com.planit.domain.user.controller; // 사용자 컨트롤러 테스트 패키지

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planit.domain.user.dto.UserProfileResponse;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
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

    @Test
    @DisplayName("프로필 이미지 없이 회원가입하면 프로필 사진 도움말 메시지 반환")
    void signupWithoutProfileImage() throws Exception {
        Map<String, Object> request = Map.of(
            "loginId", "planit_user",
            "password", "Correct1!",
            "passwordConfirm", "Correct1!",
            "nickname", "planitter"
        );

        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("*프로필 사진을 추가해주세요."));
    }

    @Test
    @DisplayName("대문자 포함 아이디 중복 체크 시 스펙 메시지 반환")
    void checkLoginIdRejectsUppercase() throws Exception {
        mockMvc.perform(get("/api/users/check-login-id")
                .param("loginId", "Planit123")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "*아이디는 영문 소문자와 숫자, _ 만 포함할 수 있습니다."));
    }

    @Test
    @DisplayName("비밀번호 확인이 다르면 오류 메시지 반환")
    void signupRejectsPasswordMismatch() throws Exception {
        Map<String, Object> request = Map.of(
            "loginId", "planit_user",
            "password", "Correct1!",
            "passwordConfirm", "Wrong1!",
            "nickname", "planitter",
            "profileImageId", 42L
        );

        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("*비밀번호가 다릅니다."));
    }

    @Test
    @DisplayName("Validation 오류는 errors 배열에 필드 이름과 메시지를 담아 반환")
    void validationErrorIncludesFieldList() throws Exception {
        Map<String, Object> request = Map.of(
            "password", "Correct1!",
            "passwordConfirm", "Correct1!",
            "nickname", "Planitter",
            "profileImageId", 42L
        );

        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("*아이디를 입력해주세요."))
            .andExpect(jsonPath("$.errors[0].field").value("loginId"))
            .andExpect(jsonPath("$.errors[0].message").value("*아이디를 입력해주세요."));
    }

    @Test
    @DisplayName("프로필 이미지 삭제 엔드포인트는 204를 반환한다")
    @WithMockUser
    void deleteProfileImageReturnsNoContent() throws Exception {
        doNothing().when(userService).deleteProfileImage(5L); // 서비스 호출 모킹

        mockMvc.perform(delete("/api/users/5/profile-image"))
            .andExpect(status().isNoContent());

        verify(userService).deleteProfileImage(5L);
    }

    @Test
    @DisplayName("프로필 이미지 삭제는 인증이 없으면 401")
    void deleteProfileImageRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/users/5/profile-image"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("*로그인이 필요한 요청입니다."));
    }

    @Test
    @DisplayName("내 정보 조회는 인증되면 사용자 정보를 내려준다")
    @WithMockUser(username = "planit_user")
    void meReturnsProfileWhenAuthenticated() throws Exception {
        when(userService.getProfile("planit_user"))
            .thenReturn(new UserProfileResponse(5L, "planit_user", "planitter")); // 인증된 사용자 반환 데이터 모킹

        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(5))
            .andExpect(jsonPath("$.loginId").value("planit_user"))
            .andExpect(jsonPath("$.nickname").value("planitter"));
    }

    @Test
    @DisplayName("내 정보 조회는 인증 없으면 401")
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("*로그인이 필요한 요청입니다."));
    }

    @Test
    @DisplayName("health/healthcheck/api/healthcheck 모두 인증 없이 200 반환")
    void healthcheckIsAccessible() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/api/healthcheck"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/api/healthcheck"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }
}
