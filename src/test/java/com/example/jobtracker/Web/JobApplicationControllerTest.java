package com.example.jobtracker.Web;

import com.example.jobtracker.dto.JobApplicationDto;
import com.example.jobtracker.service.JobApplicationService;
import com.example.jobtracker.web.JobApplicationController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
 * 终极全绿版：路径 + 静态资源 + 依赖全部搞定
 */
@WebMvcTest(controllers = JobApplicationController.class)
@TestPropertySource(properties = "spring.web.resources.add-mappings=false")  // 必须打开这行！Spring Boot 3.5+ 必备
class JobApplicationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private JobApplicationService service;

    // 必须加上这俩！否则 JwtFilter 启动报错（CI 环境最常见杀手）
    @MockBean
    private com.example.jobtracker.security.JwtUtil jwtUtil;

    @MockBean
    private com.example.jobtracker.security.JwtFilter jwtFilter;

    @Test
    @DisplayName("GET /api/job-applications/search 返回分页 JSON")
    void search_returnsPagedJson() throws Exception {
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

        when(service.searchByCompany(eq("abc"), eq(0), eq(5))).thenReturn(page);

        // 关键第1行：改成你真实路径（99.999% 是这个！）
        mvc.perform(get("/api/job-applications/search")
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