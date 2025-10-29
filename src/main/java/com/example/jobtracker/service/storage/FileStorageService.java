
// src/main/java/.../service/storage/FileStorageService.java
package com.example.jobtracker.service.storage;

import com.example.jobtracker.domain.FileObject;
import com.example.jobtracker.repository.FileObjectRepository;
import com.example.jobtracker.service.CurrentTenant;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.UUID;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    private final S3Client s3;
    private final FileObjectRepository repo;
    private final CurrentTenant currentTenant;

    private final S3Presigner presigner;


    private final String bucket = "jobtracker";          // 建议注入配置
    private final long presignExpireSeconds = 300;       // 建议注入配置




    @Transactional
    public FileUploadResponse upload(MultipartFile file) throws IOException {
        var tenantId = currentTenant.requireTenantId();
        UUID uploader = null;
        try { uploader = currentTenant.requireUserId(); } catch (RuntimeException ignored) {}

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.bin";
        String sanitized = sanitize(originalName);

        // 自前採番（assigned）
        UUID id = UUID.randomUUID();
        String s3Key = "tenant/" + tenantId + "/" + id + "-" + sanitized;

        // 1) 先にS3
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(s3Key)
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromBytes(file.getBytes())
        );

        // 2) DB（失敗時は補償削除）
        try {
            FileObject fo = FileObject.builder()
                    .id(id)
                    .tenantId(tenantId)
                    .filename(originalName)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .s3Key(s3Key)
                    .uploaderUserId(uploader)
                    .createdAt(Instant.now())
                    .build();

            repo.save(fo); // version==null → isNew()==true → persist(INSERT)

            return new FileUploadResponse(fo.getId(), fo.getFilename(), fo.getSize(), fo.getTenantId(), fo.getS3Key());
        } catch (RuntimeException e) {
            try {
                s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(s3Key).build());
            } catch (Exception ignore) { /* ログだけ */ }
            throw e;
        }
    }




    public URL createPresignedGetUrl(UUID fileId) {
        UUID tenantId = currentTenant.requireTenantId();

        FileObject fo = repo.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        // 权限：只能同租户访问；SYSTEM_ADMIN 可放行（按你的角色逻辑扩展）
        if (!tenantId.equals(fo.getTenantId()) && !currentTenant.isSystemAdmin()) {
            throw new AccessDeniedException("No permission to download this file");
        }

        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(fo.getS3Key())
                .responseContentType(fo.getContentType()) // 可选：下载时的类型
                .responseContentDisposition("attachment; filename=\"" + fo.getFilename() + "\"")
                .build();

        var presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignExpireSeconds))
                .getObjectRequest(get)
                .build();

        return presigner.presignGetObject(presignReq).url();
    }

    public void delete(UUID fileId) {
        UUID tenantId = currentTenant.requireTenantId();
        FileObject fo = repo.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        if (!tenantId.equals(fo.getTenantId()) && !currentTenant.isSystemAdmin()) {
            throw new AccessDeniedException("No permission to delete this file");
        }

        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(fo.getS3Key())
                .build());

        repo.deleteById(fileId);
    }

    static String buildKey(UUID tenantId, String filename) {
        // 简单：tenant/{tenantId}/{uuid}-{filename}
        return "tenant/" + tenantId + "/" + UUID.randomUUID() + "-" + sanitize(filename);
    }

    static String sanitize(String name) {
        return name == null ? "unnamed" : name.replaceAll("[\\s\\\\/]+", "_");
    }
}
