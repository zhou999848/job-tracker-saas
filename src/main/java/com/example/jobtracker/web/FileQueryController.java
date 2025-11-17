// src/main/java/.../web/FileQueryController.java
package com.example.jobtracker.web;

import com.example.jobtracker.domain.FileObject;
import com.example.jobtracker.repository.FileObjectRepository;
import com.example.jobtracker.service.CurrentTenant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileQueryController {

    private final FileObjectRepository repo;
    private final CurrentTenant currentTenant;

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','TENANT_ADMIN','SYSTEM_ADMIN')")
    public Page<FileObject> list(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        var tenantId = currentTenant.requireTenantId();

        var pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return repo.findByTenantId(tenantId, pr);
}

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','TENANT_ADMIN','SYSTEM_ADMIN')")
    public ResponseEntity<?> detail(@PathVariable UUID id) {
        var tenantId = currentTenant.requireTenantId();
        var fo = repo.findById(id).orElse(null);
        if (fo == null) return ResponseEntity.notFound().build();
        if (!tenantId.equals(fo.getTenantId()) && !currentTenant.isSystemAdmin()) {
            return ResponseEntity.status(403).body(Map.of("error", "forbidden"));
        }
        return ResponseEntity.ok(Map.of(
                "id", fo.getId(),
                "filename", fo.getFilename(),
                "size", fo.getSize(),
                "contentType", fo.getContentType(),
                "createdAt", fo.getCreatedAt()
        ));
    }
}

