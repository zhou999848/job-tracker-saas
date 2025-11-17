package com.example.jobtracker.Web;

import com.example.jobtracker.repository.FileObjectRepository;
import com.example.jobtracker.security.JwtFilter;
import com.example.jobtracker.security.JwtUtil;
import com.example.jobtracker.service.CurrentTenant;
import com.example.jobtracker.service.storage.FileStorageService;
import com.example.jobtracker.web.FileController;
import com.example.jobtracker.web.FileQueryController;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URL;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// src/test/java/.../web/FileControllerTest.java
@WebMvcTest(controllers = { FileController.class, FileQueryController.class })
@AutoConfigureMockMvc(addFilters = false)
class FileControllerTest {
    @Autowired MockMvc mvc;
    @MockBean
    private FileObjectRepository fileObjectRepository;
    @MockBean FileStorageService storage;

    @MockBean
    JwtFilter jwtFilter;
    @MockBean
    JwtUtil jwtUtil;
    @MockBean
    CurrentTenant currentTenant;   // 加这行
    @Test
    void presigned_ok() throws Exception {
        var id = java.util.UUID.randomUUID();
        when(storage.createPresignedGetUrl(id))
                .thenReturn(new java.net.URL("http://localhost:9000/jobtracker/tenant/x/file"));

        mvc.perform(get("/files/{id}/presigned", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").exists())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void delete_ok() throws Exception {
        var id = java.util.UUID.randomUUID();
        mvc.perform(delete("/files/{id}", id)).andExpect(status().isOk());
        Mockito.verify(storage).delete(id);
    }
}
