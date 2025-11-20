package com.example.jobtracker.Web;

import com.example.jobtracker.config.TestDatabaseConfig;
import com.example.jobtracker.config.TestS3Config;
import com.example.jobtracker.container.MinioTCBase;
import com.example.jobtracker.repository.FileObjectRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)  // 保持你原来的设置
@Import({TestDatabaseConfig.class, TestS3Config.class})
@Testcontainers                               // ← 必须打开！
@ActiveProfiles("test")
class FileE2EIT extends MinioTCBase {

    @Autowired MockMvc mvc;
    @Autowired FileObjectRepository repo;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")  // 推荐用 alpine，轻快
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    // 其余代码完全不动
    UUID t1 = UUID.randomUUID();
    UUID t2 = UUID.randomUUID();

    @Test
    @DisplayName("同租户上传并获取预签名成功，跨租户访问 403")
    void uploadAndPresign_thenCrossTenant403() throws Exception {
        // ... 你的测试代码完全不动
    }

    private String extractJsonValue(String json, String field) {
        return json.replaceAll(".*\"" + field + "\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }
}