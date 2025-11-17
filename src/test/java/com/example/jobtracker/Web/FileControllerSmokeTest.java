// src/test/java/com/example/jobtracker/Web/FileControllerSmokeTest.java
package com.example.jobtracker.Web;

import com.example.jobtracker.repository.FileObjectRepository;
import com.example.jobtracker.security.JwtFilter;
import com.example.jobtracker.security.JwtUtil;
import com.example.jobtracker.service.CurrentTenant;
import com.example.jobtracker.service.storage.FileStorageService;
import com.example.jobtracker.service.storage.FileUploadResponse;
import com.example.jobtracker.web.FileController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FileController.class)
@AutoConfigureMockMvc(addFilters = false)  // 直接关掉所有过滤器
class FileControllerSmokeTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private FileStorageService storage;

    @MockBean
    private FileObjectRepository fileObjectRepository;

    @MockBean
    private JwtFilter jwtFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    CurrentTenant currentTenant;   // 加这行


    @Test
    void upload_200() throws Exception {
        when(storage.upload(any(MultipartFile.class)))
                .thenReturn(
                        // 修复 2：FileUploadResponse 需要 5 个参数，按你实际构造函数顺序填
                        new FileUploadResponse(
                                UUID.randomUUID(),        // id
                                "test-resume.pdf",        // filename
                                102400L,                  // size (随便填个数字)
                                UUID.randomUUID(),        // tenantId (随便填)
                                "https://s3.example.com/test-resume.pdf"  // url
                        )
                );

        mvc.perform(multipart("/files/upload")  // 必须是 /api/files/upload
                        .file(new MockMultipartFile("file", "resume.pdf", "application/pdf", "fake pdf".getBytes()))
                        .param("type", "RESUME")  // 如果 Controller 需要这个参数
                        .with(request -> {
                            request.setServletPath("/api/files/upload");
                            return request;
                        }))
                .andExpect(status().isOk());
    }

    @Test
    void presigned_200() throws Exception {
        when(storage.createPresignedGetUrl(any()))
                .thenReturn(new URL("http://example.com/p"));
        mvc.perform(get("/files/{id}/presigned", java.util.UUID.randomUUID()))
                .andExpect(status().isOk());
    }
}