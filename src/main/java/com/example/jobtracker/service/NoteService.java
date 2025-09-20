package com.example.jobtracker.service;

import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.domain.Note;
import com.example.jobtracker.domain.User;
import com.example.jobtracker.dto.NoteDto;
import com.example.jobtracker.repository.JobApplicationRepository;
import com.example.jobtracker.repository.NoteRepository;
import com.example.jobtracker.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;


@Service
public class NoteService {

    private final NoteRepository noteRepo;
    private final UserRepository userRepo;
    private final JobApplicationRepository jobRepo;
    private final CurrentTenant currentTenant;

    public NoteService(NoteRepository noteRepo,
                       UserRepository userRepo,
                       JobApplicationRepository jobRepo,
                       CurrentTenant currentTenant) {
        this.noteRepo = noteRepo;
        this.userRepo = userRepo;
        this.jobRepo = jobRepo;
        this.currentTenant = currentTenant;
    }

    @Transactional
    public void save(NoteDto dto) {
        // 当前用户 & 租户
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID tenantId = currentTenant.requireTenantId();

        User user = userRepo.findByTenantIdAndUsername(tenantId,username).orElseThrow();

        // 先按“租户 + 主键”加载 Job，防越权
        JobApplication job = jobRepo.findByIdAndTenantId(dto.getJobId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));

        Note note = new Note();
        note.setTenant(job.getTenant());     // 绑定租户（与 job 一致）
        note.setUser(user);                  // 创建者
        note.setJobId(dto.getJobId());                    // 关联职位
        // 如果你的 Note 还是 jobId 基本字段，请用： note.setJobId(dto.getJobId());

        note.setContent(dto.getContent());
        note.setCreatedAt(dto.getCreatedAt());
        note.setFilePaths(dto.getFilePaths());

        noteRepo.save(note);
    }

    @Transactional
    public void save(Note note) {
        // 保底：若外部没设租户/用户/职位，这里补齐并做校验
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID tenantId = currentTenant.requireTenantId();

        if (note.getUser() == null) {
            note.setUser(userRepo.findByTenantIdAndUsername(tenantId,username).orElseThrow());
        }
        if (note.getJobId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Job is required");
        }
        // 校验：job 必须属于当前租户
        JobApplication job = jobRepo.findByIdAndTenantId(note.getJobId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Cross-tenant job"));
        note.setJobId(note.getJobId());

        if (note.getTenant() == null) {
            note.setTenant(job.getTenant());
        }
        noteRepo.save(note);
    }

    @Transactional(readOnly = true)
    public Page<NoteDto> findByJobIdPaged(UUID jobId, int page, int size) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID tenantId = currentTenant.requireTenantId();

        PageRequest request = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 租户 + 用户名 + Job 限定
        Page<Note> pg = noteRepo.findByTenantIdAndUser_UsernameAndJobId(tenantId, username, jobId, request);
        // 若你还是旧方法：noteRepo.findByTenantIdAndUser_UsernameAndJobId(tenantId, username, jobId, request);

        return pg.map(n -> {
            NoteDto dto = new NoteDto();
            dto.setId(n.getId());
           dto.setJobId(n.getJobId());
            dto.setContent(n.getContent());
            dto.setCreatedAt(n.getCreatedAt());
            dto.setFilePaths(n.getFilePaths() == null ? List.of() : new ArrayList<>(n.getFilePaths()));
            return dto;
        });
    }

    @Transactional
    public void delete(UUID id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID tenantId = currentTenant.requireTenantId();

        Note note = noteRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!note.getUser().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        noteRepo.delete(note);
    }

    @Transactional
    public void checkOwner(UUID id) {
        // 与 delete 合并同逻辑，保留兼容
        delete(id);
    }

    @Transactional(readOnly = true)
    public Note findById(UUID id) {
        UUID tenantId = currentTenant.requireTenantId();
        return noteRepo.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到该笔记"));
    }
}


