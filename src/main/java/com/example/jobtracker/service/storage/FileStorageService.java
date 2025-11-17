
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
import software.amazon.awssdk.core.exception.SdkException;
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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    private final S3Client s3;
    private final FileObjectRepository repo;
    private final CurrentTenant currentTenant;

    private final S3Presigner presigner;


    private final String bucket = "jobtracker";          // 建议注入配置
    private final long presignExpireSeconds = 300;       // 建议注入配置


    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    @Transactional
    public FileUploadResponse upload(MultipartFile file) throws IOException {
        // 0) 基础校验：空文件直接拒绝（由上层统一转 400）
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // 1) 上下文信息
        var tenantId = currentTenant.requireTenantId();
        UUID uploader = null;
        try {
            uploader = currentTenant.requireUserId();
        } catch (RuntimeException ignored) {
            // 匿名/系统任务等场景允许为空
        }

        // 2) 元数据兜底（避免下游 null）
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            originalName = "upload.bin";
        }
        String safeName = truncate(sanitize(originalName), 120); // 避免过长 Key/文件名问题

        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        long size = file.getSize();
        if (size < 0) size = 0L; // 某些奇怪代理可能返回负数，兜底为 0

        // 3) 预先分配 ID & Key（含租户前缀，防串租户）
        UUID id = UUID.randomUUID();
        String s3Key = "tenant/" + tenantId + "/" + id + "-" + safeName;

        // 4) 先写 S3（失败则直接抛错，不落库）
        try {
            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(s3Key)
                    .contentType(contentType)
                    .contentLength(size) // 提示长度，部分网关更稳
                    .build();

            // 避免一次性读入全部字节，降低内存压力
            try (var in = file.getInputStream()) {
                s3.putObject(put, RequestBody.fromInputStream(in, size));
            }
        } catch (SdkException | IOException s3ex) {
            // S3 写失败直接上抛（事务回滚）
            throw (s3ex instanceof IOException) ? (IOException) s3ex : new IOException("S3 upload failed", s3ex);
        }

        // 5) 再写 DB（失败则补偿删除 S3）
        try {
            FileObject fo = FileObject.builder()

                    .tenantId(tenantId)
                    .filename(originalName)        // 对外展示仍用用户原名
                    .contentType(contentType)
                    .size(size)                    // 建议实体用 long 基本类型；若是 Long 也不会为 null
                    .s3Key(s3Key)
                    .uploaderUserId(uploader)
                    .createdAt(Instant.now())
                    .build();

            log.info("before save id={}", fo.getId());
            FileObject saved = repo.save(fo);
            log.info("after  save id={}", saved.getId());

            return new FileUploadResponse(
                    saved.getId(),             // ←← 必须用 saved.getId()
                    saved.getFilename(),
                    saved.getSize(),
                    saved.getTenantId(),
                    saved.getS3Key()
            );
        } catch (RuntimeException e) {
            // DB 失败补偿删除对象
            try {
                s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(s3Key).build());
            } catch (Exception ignore) { /* 记录日志即可 */ }
            throw e;
        }
    }
    /** 仅保留字母/数字/点/下划线/连字符，其余替换为下划线，避免路径/头注入问题；null 返回 "unnamed" */
    //private static String sanitize(String name) {
      //  if (name == null) return "unnamed";
       // return name.replaceAll("[^A-Za-z0-9._-]+", "_");
   // }

    /** 统一截断，避免极端长文件名导致 S3 Key/HTTP 头过长 */
    private static String truncate(String s, int max) {
        return (s != null && s.length() > max) ? s.substring(0, max) : s;
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
