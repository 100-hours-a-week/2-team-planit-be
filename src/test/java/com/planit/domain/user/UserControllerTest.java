package com.planit.domain.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planit.domain.user.controller.UserController;
import com.planit.domain.user.dto.UserProfileResponse;
import com.planit.domain.user.query.service.UserQueryService;
import com.planit.domain.user.security.JwtAuthenticationFilter;
import com.planit.domain.user.service.UserService;
import com.planit.global.common.exception.BusinessException;
import com.planit.global.common.exception.ErrorCode;
import com.planit.global.common.exception.GlobalExceptionHandler;
import com.planit.global.security.PlanMineAuthenticationFilter;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private UserQueryService userQueryService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private PlanMineAuthenticationFilter planMineAuthenticationFilter;

    @Test
    @DisplayName("회원_생성_성공")
    void 회원_생성_성공() throws Exception {
        when(userService.createUser(any())).thenReturn(new UserProfileResponse(1L, "test@planit.com", "planit", null));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "test@planit.com",
                                "nickname", "planit",
                                "password", "password123"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.loginId").value("test@planit.com"))
                .andExpect(jsonPath("$.nickname").value("planit"));
    }

    @Test
    @DisplayName("입력값_검증_실패")
    void 입력값_검증_실패() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "invalid-email",
                                "password", "12345678"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("이메일_형식_오류")
    void 이메일_형식_오류() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "wrong-email",
                                "nickname", "planit",
                                "password", "12345678"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].reason").value("이메일 형식이 아닙니다"));
    }

    @Test
    @DisplayName("닉네임_누락")
    void 닉네임_누락() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "test@planit.com",
                                "password", "12345678"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("nickname"));
    }

    @Test
    @DisplayName("회원_조회_성공")
    void 회원_조회_성공() throws Exception {
        when(userService.getUser(1L)).thenReturn(new UserProfileResponse(1L, "test@planit.com", "planit", null));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.loginId").value("test@planit.com"));
    }

    @Test
    @DisplayName("존재하지_않는_유저_조회_USER_NOT_FOUND")
    void 존재하지_않는_유저_조회_USER_NOT_FOUND() throws Exception {
        when(userService.getUser(999L)).thenThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }
}
