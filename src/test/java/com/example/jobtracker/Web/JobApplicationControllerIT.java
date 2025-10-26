
// src/test/java/.../web/JobApplicationControllerIT.java
package com.example.jobtracker.Web;

import com.example.jobtracker.container.PostgresTCBase;
import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.repository.JobApplicationRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest // 全应用上下文
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JobApplicationControllerIT extends PostgresTCBase {

    @Autowired MockMvc mvc;
    @Autowired JobApplicationRepository repo;

    private UUID tenant1;

    @BeforeEach
    void initData() {
        repo.deleteAll();


        var t1 = UUID.randomUUID();


        tenant1 = t1;


        repo.save(JobApplication.builder()
                .tenant(Tenant.builder().id(t1).build()).company("XYZ LLC").position("SWE")
                .createdAt(Instant.now()).build());
    }

    @Test
    void search_returnsOwnTenantData() throws Exception {
        // 如果你的 Controller 通过 CurrentTenant 读取租户，可考虑用测试用的 Filter/Resolver 注入 tenantId，
        // 或者给接口加一个仅测试环境使用的 Header（例如 X-Debug-Tenant）来模拟。
        mvc.perform(get("/jobs/search")
                        .param("keyword", "ACME")
                        .param("page", "0").param("size", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        // 可继续断言 JSON：.andExpect(jsonPath("$.content[0].company").value("ACME Corp"))
    }
}

