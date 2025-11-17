
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
 * ??慡?斉丗楬宎夵? + ??惷??尮 + 惓? mock
 */
@WebMvcTest(controllers = JobApplicationController.class)
//@TestPropertySource(properties = "spring.web.resources.add-mappings=false")  // ??惷??尮姳?
@Import(JobApplicationControllerTest.TestConfig.class)
class JobApplicationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean  // 夵梡 @MockBean丆斾庤? Bean 峏?掕両
    private JobApplicationService service;

    // ?壔攝抲丗扅廀梫 mock Service 廇?椆丆JwtFilter 榓 JwtUtil 晄廀梫
    static class TestConfig {
        // 廦?搒晄梡幨丆@MockBean 涍??掕
    }

    @Test
    @DisplayName("GET /api/job-applications/search 曉夞暘? JSON")
    void search_returnsPagedJson() throws Exception {
        // ?憿橈悢悩
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

        // 姰慡旵攝?揑 service 曽朄丗searchByCompany(String keyword, int page, int size)
        when(service.searchByCompany(eq("abc"), eq(0), eq(5))).thenReturn(page);

        // ??丗99.9999% ?揑恀?楬宎惀?槩両乮job-applications 晄惀 jobs乯
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