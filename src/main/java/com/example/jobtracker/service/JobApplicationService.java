package com.example.jobtracker.service;

import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.domain.Note;
import com.example.jobtracker.domain.User;
import com.example.jobtracker.dto.JobApplicationDto;
import com.example.jobtracker.repository.JobApplicationRepository;
import com.example.jobtracker.repository.UserRepository;

import com.example.jobtracker.security.LoginUser;
import com.example.jobtracker.tenant.TenantContext;
import com.example.jobtracker.ああ７a５.SecurityUtils;
import com.example.jobtracker.ああ７a５.TenantGuard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;


@Service
public class JobApplicationService {

    private static final Logger biz = LoggerFactory.getLogger("BIZ_AUDIT");

    private final JobApplicationRepository jobRepository;
    private final UserRepository userRepository;
    private final CurrentTenant currentTenant;
    private final TenantGuard guard;

    public JobApplicationService(JobApplicationRepository jobRepository,
                                 UserRepository userRepository,
                                 CurrentTenant currentTenant,
                                 TenantGuard guard) {
        this.guard = guard;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.currentTenant = currentTenant;
    }

    @Transactional
    public void save(JobApplicationDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("JobApplicationDto is null");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID tenantId = TenantContext.requireTenantIdFromRequest();

        guard.requireWritableTenant(tenantId);

// 优先用 userId 精确查，失败再兜底用 username
        User user = null;
        if (auth != null) {
            Object principal = auth.getPrincipal();
            if (principal instanceof LoginUser lu && lu.getUserId() != null) {
                user = userRepository.findByIdAndTenant_Id(lu.getUserId(), tenantId)
                        .orElse(null);
            } else if (principal instanceof UserDetails ud) {
                user = userRepository.findByTenant_IdAndUsername(tenantId, ud.getUsername())
                        .orElse(null);
            } else if (principal instanceof java.security.Principal p) {
                user = userRepository.findByTenant_IdAndUsername(tenantId, p.getName())
                        .orElse(null);
            } else if (auth.getName() != null) {
                user = userRepository.findByTenant_IdAndUsername(tenantId, auth.getName())
                        .orElse(null);
            }
        }

        if (user == null) {
            throw new IllegalStateException(
                    "User not found under tenant. principal=" + auth.getPrincipal() + ", tenantId=" + tenantId);
        }

        // 组装实体
        JobApplication job = new JobApplication();
       job.setId(dto.getId());              // 新建可不设，由 DB 生成
        job.setCompany(dto.getCompany());
        job.setPosition(dto.getPosition());
        job.setStatus(dto.getStatus());
        job.setAppliedDate(dto.getAppliedDate());

        job.setUser(user);                   // 归属用户
        job.setTenant(user.getTenant());     // 归属租户（确保与用户一致；或用 tenantRepo.getReferenceById(tenantId)）

        jobRepository.save(job);
    }



    @Transactional(readOnly = true)
    public Page<JobApplicationDto> findAll(int page, int size, String sortBy, String direction) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID tenantId = currentTenant.requireTenantId();

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        PageRequest request = PageRequest.of(page, size, sort);

        return jobRepository.findByTenantIdAndUser_Username(tenantId, username, request)
                .map(job -> {
                    JobApplicationDto dto = new JobApplicationDto();
                    dto.setId(job.getId());
                    dto.setCompany(job.getCompany());
                    dto.setPosition(job.getPosition());
                    dto.setStatus(job.getStatus());
                    dto.setAppliedDate(job.getAppliedDate());
                    return dto;
                });
    }

    @Transactional(readOnly = true)
    public Page<JobApplicationDto> searchByCompany(String keyword, int page, int size) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID tenantId = currentTenant.requireTenantId();

        PageRequest req = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appliedDate"));
        return jobRepository
                .findByTenantIdAndCompanyContainingIgnoreCaseAndUser_Username(tenantId, keyword, username, req)
                .map(job -> {
                    JobApplicationDto dto = new JobApplicationDto();
                    dto.setId(job.getId());
                    dto.setCompany(job.getCompany());
                    dto.setPosition(job.getPosition());
                    dto.setStatus(job.getStatus());
                    dto.setAppliedDate(job.getAppliedDate());
                    return dto;
                });
    }

    @Transactional
    public void save(JobApplication job) {
        // 保证租户与用户正确
        if (job.getUser() == null) {
            UUID tenantId = currentTenant.requireTenantId();
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            job.setUser(userRepository.findByTenantIdAndUsername(tenantId,username).orElseThrow());
        }
        if (job.getTenant() == null) {
            job.setTenant(job.getUser().getTenant());
        }
        jobRepository.save(job);
    }

    @Transactional
    public void checkOwner(UUID id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID tenantId = currentTenant.requireTenantId();

        JobApplication job = jobRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!job.getUser().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        jobRepository.delete(job);
        biz.info("JOB_DELETE user={} jobId={}", username, id);
    }

    @Transactional(readOnly = true)
    public JobApplication findById(UUID id) {
        UUID tenantId = currentTenant.requireTenantId();
        return jobRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到该职位"));
    }

    @Transactional(readOnly = true)
    public JobApplication getMine(UUID id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID tenantId = currentTenant.requireTenantId();

        JobApplication job = jobRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!job.getUser().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return job;
    }

    @Transactional
    public void updateJob(UUID id, JobApplicationDto req) {
        JobApplication job = getMine(id); // 已做租户&归属校验
        job.setCompany(req.getCompany());
        job.setPosition(req.getPosition());
        job.setStatus(req.getStatus());
        job.setAppliedDate(req.getAppliedDate());
        jobRepository.save(job);
    }
    @Transactional(readOnly = true)

    public Page<JobApplication> getStudentJobs(UUID tenantId, UUID studentUserId, Pageable pageable) {

        // ① student 必须属于当前 tenant
        if (!userRepository.existsByIdAndTenant_Id(studentUserId, tenantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        // ② 查询该学生在本 tenant 下的 Job
        return jobRepository.findByTenant_IdAndUser_Id(tenantId, studentUserId, pageable);
    }
}





