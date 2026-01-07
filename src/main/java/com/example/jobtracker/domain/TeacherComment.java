package com.example.jobtracker.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "teacher_comments",
        indexes = {
                @Index(name = "idx_tc_tenant_student", columnList = "tenant_id, student_id"),
                @Index(name = "idx_tc_student_created", columnList = "student_id, created_at")
        })
public class TeacherComment {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "content", nullable = false, length = 2000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TeacherComment() {}

    public TeacherComment(UUID tenantId, UUID studentId, UUID teacherId, String content) {
        this.tenantId = tenantId;
        this.studentId = studentId;
        this.teacherId = teacherId;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getStudentId() { return studentId; }
    public UUID getTeacherId() { return teacherId; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
