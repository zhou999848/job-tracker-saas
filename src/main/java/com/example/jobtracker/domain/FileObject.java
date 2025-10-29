package com.example.jobtracker.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.domain.Persistable;

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

public class FileObject implements Persistable<UUID> {
    @Id
   // @GeneratedValue
   // @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;



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
    @Version
    private Long version;  // ★これを追加
    // --- Persistable を実装して save() に新規/更新を明示 ---
    @Override
    public UUID getId() { return id; }
    @Override
    public boolean isNew() { return version == null; }

    @Column(name = "s3_key", nullable = false)
    private String s3Key;


    // 其他字段...
// 其他字段...
    private static String sanitize(String name) {
        if (name == null) return "";
        String s = name.trim();
        // 只保留常见安全字符，其他替换为下划线
        s = s.replaceAll("[^a-zA-Z0-9._-]", "_");
        // 限制长度，避免数据库或对象键过长
        int max = 200;
        if (s.length() > max) s = s.substring(0, max);
        // 移除结尾的点/下划线/短横
        s = s.replaceAll("[._-]+$", "");
        if (s.isEmpty()) s = "file";
        return s;
    }
    @PrePersist
    private void prePersist() {
        if (this.s3Key == null) {
           // if (this.s3Key == null) {
            // 根据已有字段拼出 key（比如 tenantId、filename）
            String sanitized = sanitize(this.filename);
            this.s3Key = "tenant/" + this.tenantId + "/" + this.id + "-" + sanitized;
        }
    }



}
