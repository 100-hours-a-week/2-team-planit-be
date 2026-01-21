package com.planit.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.planit.domain.User;
import com.planit.dto.SignUpRequest;
import com.planit.dto.UserSignupResponse;
import com.planit.repository.UserProfileImageRepository;
import com.planit.repository.UserRepository;
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

    @Autowired
    private UserProfileImageRepository userProfileImageRepository;

    @Test
    @DisplayName("회원가입 시 users/user_image 테이블에 데이터가 저장된다")
    void signupPersistsUserAndProfileImage() {
        SignUpRequest request = new SignUpRequest();
        request.setLoginId("PlanitUser");
        request.setPassword("Correct1!");
        request.setPasswordConfirm("Correct1!");
        request.setNickname("Planitter");
        request.setProfileImageId(99L);

        UserSignupResponse response = userService.signup(request);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isNotNull();

        User persistedUser = userRepository.findById(response.getUserId()).orElseThrow();
        assertThat(persistedUser.getLoginId()).isEqualTo("PlanitUser");
        assertThat(persistedUser.getNickname()).isEqualTo("Planitter");
        assertThat(persistedUser.isDeleted()).isFalse();

        assertThat(userRepository.existsByLoginIdAndDeletedFalse("PlanitUser")).isTrue();
        assertThat(userProfileImageRepository.findAll())
            .hasSize(1)
            .first()
            .satisfies(image -> {
                assertThat(image.getUserId()).isEqualTo(response.getUserId());
                assertThat(image.getImageId()).isEqualTo(99L);
            });
    }

    @Test
    @DisplayName("프로필 이미지 삭제 요청 시 연관된 user_image 행이 제거된다")
    void deleteProfileImageRemovesEntry() {
        SignUpRequest request = new SignUpRequest();
        request.setLoginId("PlanitUser2");
        request.setPassword("Correct1!");
        request.setPasswordConfirm("Correct1!");
        request.setNickname("PlanitUser");
        request.setProfileImageId(100L);

        UserSignupResponse response = userService.signup(request);

        assertThat(userProfileImageRepository.existsByUserId(response.getUserId())).isTrue();

        userService.deleteProfileImage(response.getUserId());

        assertThat(userProfileImageRepository.existsByUserId(response.getUserId())).isFalse();
        assertThat(userProfileImageRepository.findAll()).isEmpty();
    }
}
