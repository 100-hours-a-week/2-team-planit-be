package com.planit.infrastructure.storage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = StubUploadController.class)
@TestPropertySource(properties = "storage.mode=stub")
class StubUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StubUploadStorage stubUploadStorage;

    @Test
    void uploadEndpointReturnsOk() throws Exception {
        String key = "profile/1/stub/test.jpg";
        mockMvc.perform(put("/stub-upload")
                .param("key", key)
                .content("test"))
            .andExpect(status().isOk());

        verify(stubUploadStorage).save(eq(key), any(InputStream.class));
    }

    @TestConfiguration
    static class StubUploadControllerTestConfig {
        @Bean
        StubUploadController stubUploadController(StubUploadStorage storage) {
            return new StubUploadController(storage);
        }
    }
}
