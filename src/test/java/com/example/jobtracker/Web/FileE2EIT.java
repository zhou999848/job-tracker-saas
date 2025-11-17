
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
@AutoConfigureMockMvc(addFilters = false)
@Import({TestDatabaseConfig.class, TestS3Config.class})
@Testcontainers  // 启用 Testcontainers
@ActiveProfiles("test")  // 这一行是关键！
class FileE2EIT extends MinioTCBase {

    @Autowired MockMvc mvc;
    @Autowired FileObjectRepository repo;

    // 启动 PostgreSQL 容器
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    // 动态注入数据库连接信息
    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    UUID t1 = UUID.randomUUID();
    UUID t2 = UUID.randomUUID();

    @Test
    @DisplayName("同租户上传并获取预签名成功，跨租户访问 403")
    void uploadAndPresign_thenCrossTenant403() throws Exception {
        // 上传文件
        var uploadResponse = mvc.perform(multipart("/files/upload")
                        .file("file", "hello".getBytes())
                        .header("X-Debug-Tenant", t1.toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", not(emptyString())))
                .andReturn().getResponse().getContentAsString();

        // 提取 ID（更稳健方式）
        String id = extractJsonValue(uploadResponse, "id");

        // 同租户获取预签名 URL
        mvc.perform(get("/files/{id}/presigned", id)
                        .header("X-Debug-Tenant", t1.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url", startsWith("http")));

        // 跨租户访问文件 → 403
        mvc.perform(get("/files/{id}", id)
                        .header("X-Debug-Tenant", t2.toString()))
                .andExpect(status().isForbidden());
    }

    // 工具方法：从 JSON 提取字段
    private String extractJsonValue(String json, String field) {
        return json.replaceAll(".*\"" + field + "\"\\s*:\\s*\"([^\"]+)\".*", "$1");
    }
}