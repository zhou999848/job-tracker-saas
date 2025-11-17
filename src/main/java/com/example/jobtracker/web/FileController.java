
// src/main/java/.../web/FileController.java
package com.example.jobtracker.web;

import com.example.jobtracker.domain.FileObject;
import com.example.jobtracker.repository.FileObjectRepository;
import com.example.jobtracker.service.CurrentTenant;
import com.example.jobtracker.service.storage.FileStorageService;
import com.example.jobtracker.service.storage.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.*;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {
    private final FileObjectRepository repo;
    private final CurrentTenant currentTenant;

    private final FileStorageService storage;
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> upload(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "empty_file",
                    "message", "No file content"
            ));
        }
        try {
            FileUploadResponse fo = storage.upload(file);
            String filename = (fo.getFilename() == null || fo.getFilename().isBlank())
                    ? Optional.ofNullable(file.getOriginalFilename()).orElse("unnamed")
                    : fo.getFilename();
            long safeSize = fo.getSize() != 0L ? fo.getSize() : file.getSize();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("id", fo.getId());          // save 后一定有值
            body.put("filename", filename);
            body.put("size", safeSize);
            return ResponseEntity.ok(body);
        } catch (IOException e) {
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
