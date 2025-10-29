
// src/main/java/.../web/FileController.java
package com.example.jobtracker.web;

import com.example.jobtracker.domain.FileObject;
import com.example.jobtracker.service.storage.FileStorageService;
import com.example.jobtracker.service.storage.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService storage;

    @PostMapping("/upload")
    //@PreAuthorize("hasAnyRole('USER','TENANT_ADMIN','SYSTEM_ADMIN')")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> upload(@RequestPart("file") MultipartFile file) {
        try {
            FileUploadResponse fo = storage.upload(file);
            return ResponseEntity.ok(Map.of(
                    "id", fo.getId(),
                    "filename", fo.getFilename(),
                    "size", fo.getSize()
            ));
        } catch (java.io.IOException e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "upload_failed",
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}/presigned")
    //@PreAuthorize("hasAnyRole('USER','TENANT_ADMIN','SYSTEM_ADMIN')")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> presigned(@PathVariable UUID id) {
        URL url = storage.createPresignedGetUrl(id);
        // 可返回 302 重定向，或返回 JSON：
        return ResponseEntity.ok(Map.of("url", url.toString()));
    }

    @DeleteMapping("/{id}")
   // @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        storage.delete(id);
        return ResponseEntity.ok().build();
    }
}
