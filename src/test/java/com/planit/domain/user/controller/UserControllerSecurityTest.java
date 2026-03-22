package com.planit.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planit.domain.user.dto.UserProfileResponse;
import com.planit.domain.user.query.service.UserQueryService;
import com.planit.domain.user.repository.UserRepository;
import com.planit.domain.user.security.JwtProvider;
import com.planit.domain.user.service.UserService;
import com.planit.global.common.exception.GlobalExceptionHandler;
import com.planit.global.config.SecurityConfig;
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

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private UserQueryService userQueryService;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private UserRepository userRepository;

    @Test
    @DisplayName("JWT_인증_필요_API_접근시_401_반환")
    void JWT_인증_필요_API_접근시_401_반환() throws Exception {
        Map<String, Object> request = Map.of(
                "nickname", "tester",
                "email", "tester@planit.com"
        );

        mockMvc.perform(put("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "planit_user")
    @DisplayName("인증된_사용자만_수정_가능")
    void 인증된_사용자만_수정_가능() throws Exception {
        when(userService.updateProfile(eq("planit_user"), any()))
                .thenReturn(new UserProfileResponse(1L, "planit_user", "newnick", null));

        Map<String, Object> request = Map.of(
                "nickname", "newnick",
                "email", "planit_user@planit.com"
        );

        mockMvc.perform(put("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.loginId").value("planit_user"))
                .andExpect(jsonPath("$.nickname").value("newnick"));
    }
}
