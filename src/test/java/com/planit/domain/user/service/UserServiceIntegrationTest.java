package com.planit.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.planit.domain.user.dto.SignUpRequest;
import com.planit.domain.user.dto.UserSignupResponse;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:planit-test;DB_CLOSE_DELAY=-1;MODE=MYSQL",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("회원가입 시 users 테이블에 데이터가 저장된다")
    void signupPersistsUser() {
        SignUpRequest request = new SignUpRequest();
        request.setLoginId("PlanitUser");
        request.setPassword("Correct1!");
        request.setPasswordConfirm("Correct1!");
        request.setNickname("Planitter");

        UserSignupResponse response = userService.signup(request);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isNotNull();

        User persisted = userRepository.findById(response.getUserId()).orElseThrow();
        assertThat(persisted.getLoginId()).isEqualTo("PlanitUser");
        assertThat(persisted.getNickname()).isEqualTo("Planitter");
        assertThat(persisted.getProfileImageUrl()).isNull();
        assertThat(persisted.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("탈퇴 시 soft delete 플래그가 켜진다")
    void deleteAccountMarksDeleted() {
        User user = new User();
        user.setLoginId("toDelete");
        user.setPassword("Correct1!");
        user.setNickname("Planiter");
        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setDeleted(false);
        userRepository.save(user);

        userService.deleteAccount(user.getLoginId());

        User deleted = userRepository.findById(user.getId()).orElseThrow();
        assertThat(deleted.isDeleted()).isTrue();
    }
}
