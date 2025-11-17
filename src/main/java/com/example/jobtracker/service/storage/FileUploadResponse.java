// java
package com.example.jobtracker.service.storage;

import java.util.UUID;

public class FileUploadResponse {
    private UUID id;
    private String filename;
    private long size;
    private UUID tenantId;
    private String s3Key;
    public FileUploadResponse(UUID id, String filename, long size, UUID tenantId, String s3Key) {
        this.id = id;
        this.filename = filename;
        this.size = size;
        this.tenantId = tenantId;
        this.s3Key = s3Key;
    }


    public UUID getId() { return id; }
    public String getFilename() { return filename; }
    public long getSize() { return size; }
    public UUID getTenantId() { return tenantId; }
    public String getS3Key() { return s3Key; }
}
