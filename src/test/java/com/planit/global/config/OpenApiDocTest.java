package com.planit.global.config; // OpenAPI 설정 디렉터리와 동일한 패키지에 둠

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:planit-openapi;DB_CLOSE_DELAY=-1;MODE=MYSQL",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false"
})
class OpenApiDocTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Swagger 문서에 마이페이지 사용자 엔드포인트/스키마/보안 요약이 포함됨")
    void openApiDocsIncludeUserMeAndSecurity() throws Exception {
        mockMvc.perform(get("/api/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/users/me'].get").exists())
            .andExpect(jsonPath("$.paths['/api/users/me'].put").exists())
            .andExpect(jsonPath("$.paths['/api/users/me'].delete").exists())
            .andExpect(jsonPath("$.paths['/api/users/me/plans/{planId}'].delete").exists())
            .andExpect(jsonPath("$.components.schemas.UserProfileResponse.properties.nickname").exists())
            .andExpect(jsonPath("$.components.schemas.UserProfileResponse.properties.planHistory.items").exists())
            .andExpect(jsonPath("$.components.schemas.UserUpdateRequest.properties.nickname").exists())
            .andExpect(jsonPath("$.security").isArray());
    }
}
