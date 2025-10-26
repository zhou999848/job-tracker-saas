package com.example.jobtracker.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * 只存“元数据”，不存文件二进制。
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(
        name = "file_object",
        indexes = {
                @Index(name = "idx_file_object_tenant", columnList = "tenant_id"),
                @Index(name = "idx_file_object_created_at", columnList = "created_at")
        }
)
public class FileObject {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "s3_key", nullable = false, unique = true, length = 512)
    private String key;                 // S3/MinIO 的对象 Key（包含租户前缀）

    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long size;

    @Column(name = "uploader_user_id")
    private UUID uploaderUserId;        // 没有就先允许 null

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
