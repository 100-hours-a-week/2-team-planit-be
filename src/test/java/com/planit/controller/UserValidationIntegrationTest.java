package com.planit.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planit.config.SecurityConfig;
import com.planit.service.UserService;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import com.planit.exception.GlobalExceptionHandler;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("프로필 이미지 없이 회원가입하면 프로필 사진 도움말 메시지 반환")
    void signupWithoutProfileImage() throws Exception {
        Map<String, Object> request = Map.of(
            "loginId", "planit_user",
            "password", "Correct1!",
            "passwordConfirm", "Correct1!",
            "nickname", "planitter"
        );

        mockMvc.perform(post("/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("*프로필 사진을 추가해주세요."));
    }

    @Test
    @DisplayName("대문자 포함 아이디 중복 체크 시 스펙 메시지 반환")
    void checkLoginIdRejectsUppercase() throws Exception {
        mockMvc.perform(get("/users/check-login-id")
                .param("loginId", "Planit123")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "*올바른 아이디 형식을 입력해주세요. 아이디는 4자 이상, 20자 이하이며, 영문 소문자와 숫자, _ 만 포함해야 합니다"));
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

        mockMvc.perform(post("/users/signup")
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

        mockMvc.perform(post("/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("*아이디를 입력해주세요."))
            .andExpect(jsonPath("$.errors[0].field").value("loginId"))
            .andExpect(jsonPath("$.errors[0].message").value("*아이디를 입력해주세요."));
    }

    @Test
    @DisplayName("프로필 이미지 삭제 엔드포인트는 204를 반환한다")
    void deleteProfileImageReturnsNoContent() throws Exception {
        doNothing().when(userService).deleteProfileImage(5L);

        mockMvc.perform(delete("/users/5/profile-image"))
            .andExpect(status().isNoContent());

        verify(userService).deleteProfileImage(5L);
    }
}
