package com.planit.domain.user.controller; // 통합 테스트용 컨트롤러 패키지

import com.fasterxml.jackson.databind.ObjectMapper; // JSON 직렬화 도구
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserProfileImageRepository;
import com.planit.domain.user.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest // 실제 애플리케이션 컨텍스트로 테스트
@AutoConfigureMockMvc // MockMvc 자동 설정
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:planit-spec;DB_CLOSE_DELAY=-1;MODE=MYSQL",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
class UserSignupSpecTest {

    @Autowired
    private MockMvc mockMvc; // MockMvc로 엔드포인트 테스트

    @Autowired
    private ObjectMapper objectMapper; // 요청 JSON 직렬화

    @Autowired
    private UserRepository userRepository; // DB 조회/저장

    @Autowired
    private UserProfileImageRepository userProfileImageRepository; // 프로필 이미지 체크용 저장소

    @BeforeEach
    void setUp() {
        userProfileImageRepository.deleteAll(); // 테스트 전 데이터 정리
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userProfileImageRepository.deleteAll(); // 테스트 후 정리
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("정상 회원가입")
    void successfulSignup() throws Exception {
        Map<String, Object> request = Map.of(
            "loginId", "validuser",
            "password", "Correct1!",
            "passwordConfirm", "Correct1!",
            "nickname", "planitter",
            "profileImageId", 11L
        );

        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userId").exists()); // userId 존재 확인

        assertThat(userRepository.existsByLoginIdAndDeletedFalse("validuser")).isTrue(); // 회원 생성 여부 확인
        assertThat(userProfileImageRepository.existsByUserId(
            userRepository.findByLoginIdAndDeletedFalse("validuser").orElseThrow().getId())).isTrue();
    }

    @Test
    @DisplayName("loginId 중복")
    void loginIdDuplicate() throws Exception {
        User stuck = new User();
        stuck.setLoginId("duplicate123");
        stuck.setPassword("Correct1!");
        stuck.setNickname("planitter");
        stuck.setCreatedAt(java.time.LocalDateTime.now());
        stuck.setUpdatedAt(java.time.LocalDateTime.now());
        stuck.setDeleted(false);
        userRepository.save(stuck); // 선행 유저 저장

        Map<String, Object> request = Map.of(
            "loginId", "duplicate123",
            "password", "Correct1!",
            "passwordConfirm", "Correct1!",
            "nickname", "another",
            "profileImageId", 12L
        );

        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("*중복된 아이디 입니다."));
    }

    @Test
    @DisplayName("nickname 중복")
    void nicknameDuplicate() throws Exception {
        User stuck = new User();
        stuck.setLoginId("unique123");
        stuck.setPassword("Correct1!");
        stuck.setNickname("planitter");
        stuck.setCreatedAt(java.time.LocalDateTime.now());
        stuck.setUpdatedAt(java.time.LocalDateTime.now());
        stuck.setDeleted(false);
        userRepository.save(stuck);

        Map<String, Object> request = Map.of(
            "loginId", "new9999",
            "password", "Correct1!",
            "passwordConfirm", "Correct1!",
            "nickname", "planitter",
            "profileImageId", 13L
        );

        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("*중복된 닉네임 입니다."));
    }

    @Test
    @DisplayName("loginId 패턴 오류")
    void loginIdPatternError() throws Exception {
        Map<String, Object> request = Map.of(
            "loginId", "Invalid!",
            "password", "Correct1!",
            "passwordConfirm", "Correct1!",
            "nickname", "planitter",
            "profileImageId", 14L
        );

        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "*아이디는 영문 소문자와 숫자, _ 만 포함할 수 있습니다."));
    }

    @Test
    @DisplayName("password 정책 오류")
    void passwordPolicyError() throws Exception {
        Map<String, Object> request = Map.of(
            "loginId", "validuser2",
            "password", "nopolicy1",
            "passwordConfirm", "nopolicy1",
            "nickname", "planitter2",
            "profileImageId", 15L
        );

        mockMvc.perform(post("/api/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "*비밀번호는 8자 이상, 20자 이하이며, 대문자, 소문자, 숫자, 특수문자를 각각 최소 1개 포함해야 합니다."));
    }
}
