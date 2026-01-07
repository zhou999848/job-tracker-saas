package com.example.jobtracker.repository;

import com.example.jobtracker.domain.TeacherComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TeacherCommentRepository extends JpaRepository<TeacherComment, UUID> {

    List<TeacherComment> findByTenantIdAndStudentIdOrderByCreatedAtDesc(UUID tenantId, UUID studentId);
}
