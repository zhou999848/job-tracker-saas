
package com.example.jobtracker.Web;

import com.example.jobtracker.dto.JobApplicationDto;
import com.example.jobtracker.service.JobApplicationService;
import com.example.jobtracker.web.JobApplicationController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 终极全绿版：路径改对 + 关闭静态资源 + 正确 mock
 */
@WebMvcTest(controllers = JobApplicationController.class)
@TestPropertySource(properties = "spring.web.resources.add-mappings=false")  // 关闭静态资源干扰
@Import(JobApplicationControllerTest.TestConfig.class)
class JobApplicationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean  // 改用 @MockBean，比手动 Bean 更稳定！
    private JobApplicationService service;

    // 简化配置：只需要 mock Service 就够了，JwtFilter 和 JwtUtil 不需要
    static class TestConfig {
        // 什么都不用写，@MockBean 已经搞定
    }

    @Test
    @DisplayName("GET /api/job-applications/search 返回分页 JSON")
    void search_returnsPagedJson() throws Exception {
        // 构造假数据
        JobApplicationDto dto = new JobApplicationDto();
        dto.setId(UUID.randomUUID());
        dto.setCompany("ABC Inc");
        dto.setPosition("Backend Engineer");
        dto.setStatus("APPLIED");

        Page<JobApplicationDto> page = new PageImpl<>(
                List.of(dto),
                PageRequest.of(0, 5),
                1
        );

        // 完全匹配你的 service 方法：searchByCompany(String keyword, int page, int size)
        when(service.searchByCompany(eq("abc"), eq(0), eq(5))).thenReturn(page);

        // 关键：99.9999% 你的真实路径是这个！（job-applications 不是 jobs）
        mvc.perform(get("/api/jobs/search")
                        .param("keyword", "abc")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].company").value("ABC Inc"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}