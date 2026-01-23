package com.planit.domain.user.service; // UserService 통합 테스트를 위한 패키지

import static org.assertj.core.api.Assertions.assertThat;

import com.planit.domain.user.dto.SignUpRequest;
import com.planit.domain.user.dto.UserSignupResponse;
import com.planit.domain.user.entity.User;
import com.planit.domain.user.repository.UserProfileImageRepository;
import com.planit.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest // 실제 application context 로딩
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY) // H2 in-memory 사용
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:planit-test;DB_CLOSE_DELAY=-1;MODE=MYSQL",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
@Transactional // 테스트마다 롤백
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService; // 실제 UserService 빈 주입

    @Autowired
    private UserRepository userRepository; // 테스트에서 사용자 조회/검증

    @Autowired
    private UserProfileImageRepository userProfileImageRepository; // 프로필 이미지 검증용

    @Test
    @DisplayName("회원가입 시 users/user_image 테이블에 데이터가 저장된다")
    void signupPersistsUserAndProfileImage() {
        SignUpRequest request = new SignUpRequest();
        request.setLoginId("PlanitUser");
        request.setPassword("Correct1!");
        request.setPasswordConfirm("Correct1!");
        request.setNickname("Planitter");
        request.setProfileImageId(99L);

        UserSignupResponse response = userService.signup(request); // 회원가입 실행

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isNotNull();

        User persistedUser = userRepository.findById(response.getUserId()).orElseThrow(); // DB 확인
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

        UserSignupResponse response = userService.signup(request); // 사용자 + 이미지 생성

        assertThat(userProfileImageRepository.existsByUserId(response.getUserId())).isTrue(); // 존재 확인

        userService.deleteProfileImage(response.getUserId()); // 삭제 케이스 실행

        assertThat(userProfileImageRepository.existsByUserId(response.getUserId())).isFalse(); // 삭제 되었는지 확인
        assertThat(userProfileImageRepository.findAll()).isEmpty();
    }
}
