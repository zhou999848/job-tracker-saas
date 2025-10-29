// src/test/java/.../web/FileControllerSmokeTest.java
package com.example.jobtracker.Web;

import com.example.jobtracker.service.storage.FileStorageService;
import com.example.jobtracker.web.FileController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;


import org.mockito.Mock;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URL;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FileController.class)
class FileControllerSmokeTest {

    @Autowired MockMvc mvc;
    @Mock
    FileStorageService storage;

    @Test
    void upload_200() throws Exception {
        when(storage.upload(any())).thenAnswer(inv -> null); // 只验证 200 即可
        mvc.perform(multipart("/files/upload").file("file","hi".getBytes()))
                .andExpect(status().isOk());
    }

    @Test
    void presigned_200() throws Exception {
        when(storage.createPresignedGetUrl(any())).thenReturn(new URL("http://example.com/p"));
        mvc.perform(get("/files/{id}/presigned", UUID.randomUUID()))
                .andExpect(status().isOk());
    }
}
