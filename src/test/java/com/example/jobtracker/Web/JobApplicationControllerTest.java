
package com.example.jobtracker.Web;

import com.example.jobtracker.service.JobApplicationService;
import com.example.jobtracker.dto.JobApplicationDto;
import com.example.jobtracker.web.JobApplicationController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 最小可运行版本：不加载安全过滤器；把 Service 打桩。
 */
@WebMvcTest(controllers = JobApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
class JobApplicationControllerTest {

    @Autowired MockMvc mvc;

    @MockBean  // ✅ 改成 MockBean，让 Spring 容器能装配控制器
    JobApplicationService service;

    @Test
    @DisplayName("GET /jobs/search 返回分页 JSON")
    void search_returnsPagedJson() throws Exception {
        // 构造最小 DTO（按你的实际字段改）
        JobApplicationDto dto = new JobApplicationDto();
        var page = new PageImpl<>(List.of(dto), PageRequest.of(0, 5), 1);

        when(service.searchByCompany("abc", anyInt(), anyInt())).thenReturn(page);

        mvc.perform(get("/jobs/search")
                        .param("keyword", "abc")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}


