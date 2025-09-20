package com.example.jobtracker.repository;

import com.example.jobtracker.domain.Note;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NoteRepository extends JpaRepository<Note, UUID> {

    // 列表：按租户 + 用户名 + JobId 分页
    @EntityGraph(attributePaths = "filePaths")
    Page<Note> findByTenantIdAndUser_UsernameAndJobId(UUID tenantId, String username, UUID jobId, Pageable pageable);

    // 详情：按租户 + 主键
    Optional<Note> findByIdAndTenantId(UUID id, UUID tenantId);

    // （若需要按文件反查 Note）按租户 + 文件路径
    Optional<Note> findByTenantIdAndFilePathsContains(UUID tenantId, String filePath);
}







