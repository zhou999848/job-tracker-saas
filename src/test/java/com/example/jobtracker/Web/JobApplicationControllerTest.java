package com.example.jobtracker.Web;

import com.example.jobtracker.dto.JobApplicationDto;
import com.example.jobtracker.security.JwtFilter;
import com.example.jobtracker.security.JwtUtil;
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
import org.springframework.security.test.context.support.WithMockUser;  // 壛忋?峴
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(controllers = JobApplicationController.class)
@WithMockUser(username = "testuser@example.com", roles = "USER")
@SuppressWarnings("deprecation")
class JobApplicationControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private JobApplicationService jobApplicationService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtFilter jwtFilter;

    @Test
    @DisplayName("GET /api/jobs/search 返回分页 JSON")
    void search_returnsPagedJson() throws Exception {
        // 你原来的 mock 和 perform 完全不动
        mvc.perform(get("/api/jobs/search")
                        .param("keyword", "abc")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].company").value("ABC Inc"));
    }
}