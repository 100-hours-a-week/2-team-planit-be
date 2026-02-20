package com.planit.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StubUploadUrlProviderTest {

    @Test
    void issuePresignedPutUrl_createsStubKeyAndUrl() throws Exception {
        Path tempDir = Files.createTempDirectory("stub-upload-test");
        StorageProperties properties = new StorageProperties();
        properties.setMode("stub");
        properties.setStubBaseUrl("http://localhost:8080/api");
        properties.setStubUploadDir(tempDir.toString());

        StubUploadStorage storage = new StubUploadStorage(properties);
        StubUploadUrlProvider provider = new StubUploadUrlProvider(properties, storage);

        UploadContext context = UploadContext.profile(10L, "png", "image/png");
        var response = provider.issuePresignedPutUrl(context);

        assertThat(response.getKey()).startsWith("profile/10/stub/");
        assertThat(response.getUploadUrl()).startsWith("http://localhost:8080/api/stub-upload?key=");
        assertThat(response.getExpiresAt()).isNotNull();
    }
}
