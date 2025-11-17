
// src/test/java/com/example/jobtracker/Web/JobApplicationControllerIT.java
package com.example.jobtracker.Web;

import com.example.jobtracker.config.TestDatabaseConfig;
import com.example.jobtracker.config.TestS3Config;
import com.example.jobtracker.container.PostgresTCBase;
import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.repository.JobApplicationRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@Import({TestDatabaseConfig.class, TestS3Config.class})
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc(addFilters = false)
class JobApplicationControllerIT extends PostgresTCBase {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JobApplicationRepository repo;

    private UUID tenant1;

    @DynamicPropertySource
    static void registerPgProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PostgresTCBase.POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", PostgresTCBase.POSTGRES::getUsername);
        registry.add("spring.datasource.password", PostgresTCBase.POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @BeforeEach
    void initData() {
        repo.deleteAll();

        tenant1 = UUID.randomUUID();

        repo.save(JobApplication.builder()
                .tenant(Tenant.builder().id(tenant1).build())
                .company("XYZ LLC")
                .position("SWE")
                .createdAt(Instant.now())
                .build());
    }

    @Test
    void search_returnsOwnTenantData() throws Exception {
        mvc.perform(get("/jobs/search")
                        .param("keyword", "XYZ")
                        .param("page", "0")
                        .param("size", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].company").value("XYZ LLC"));
    }
}