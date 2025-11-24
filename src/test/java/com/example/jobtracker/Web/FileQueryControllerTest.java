// src/test/java/.../web/FileQueryControllerTest.java
package com.example.jobtracker.Web;

import com.example.jobtracker.domain.FileObject;
import com.example.jobtracker.repository.FileObjectRepository;
import com.example.jobtracker.service.CurrentTenant;
import com.example.jobtracker.web.FileQueryController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WebMvcTest(controllers = FileQueryController.class)
class FileQueryControllerTest {

    @Autowired MockMvc mvc;

    @MockBean
    FileObjectRepository repo;
    @MockBean
    CurrentTenant currentTenant;

    @Test
    @DisplayName("GET /files/{id}: 文件不存在 -> 404")
    void detail_notFound_404() throws Exception {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());

        mvc.perform(get("/files/{id}", id)
                        .header("X-Debug-Tenant", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /files/{id}: 跨租户访问 -> 403")
    void detail_crossTenant_forbidden() throws Exception {
        UUID id = UUID.randomUUID();
        UUID ownerTenant = UUID.randomUUID();
        UUID otherTenant = UUID.randomUUID();

        FileObject fo = new FileObject();
        fo.setId(id);
        fo.setTenantId(ownerTenant);

        when(repo.findById(id)).thenReturn(Optional.of(fo));
        when(currentTenant.requireTenantId()).thenReturn(otherTenant);
        when(currentTenant.isSystemAdmin()).thenReturn(false);

        mvc.perform(get("/files/{id}", id)
                        .header("X-Debug-Tenant", otherTenant.toString()))
                .andExpect(status().isForbidden());
    }
}
