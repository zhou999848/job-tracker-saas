
package com.example.jobtracker.service.storage;

import java.util.UUID;
public class FileUploadResponse {
        private UUID id;
        private UUID tenantId;
        private String filename;
        private long size;
        private String s3Key;

    public FileUploadResponse(UUID id, String filename, long size, UUID tenantId, String s3Key) {
    }

    public UUID getId() { return id; }
        public UUID getTenantId() { return tenantId; }
        public String getFilename() { return filename; }
        public long getSize() { return size; }
        public String getS3Key() { return s3Key; }

        // 可选：构造函数、builder 或 setter（视你的实现而定）
    }
