package com.example.jobtracker.service;

import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.domain.Note;
import com.example.jobtracker.domain.User;
import com.example.jobtracker.dto.JobApplicationDto;
import com.example.jobtracker.repository.JobApplicationRepository;
import com.example.jobtracker.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@Service
public class JobApplicationService {
    private static final Logger biz = LoggerFactory.getLogger("BIZ_AUDIT");

    private final JobApplicationRepository jobRepository;
    private final UserRepository userRepository;

    public JobApplicationService(JobApplicationRepository jobRepository, UserRepository userRepository) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
    }

    // 保存功能
    public void save(JobApplicationDto dto) {
        // ① 获取当前登录的用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // ② 查出 User 实体
        User user = userRepository.findByUsername(username).orElseThrow();

        // ③ 创建 Job 实体并填充数据
        JobApplication job = new JobApplication();
        job.setCompany(dto.getCompany());
        job.setPosition(dto.getPosition());
        job.setStatus(dto.getStatus());
        job.setAppliedDate(dto.getAppliedDate());
        job.setId(dto.getId());   // ← 改回 id

        // ④ 设置所属用户
        job.setUser(user);

        // ⑤ 保存
        jobRepository.save(job);
    }

    public List<JobApplicationDto> findAll() {
        List<JobApplicationDto> dtoList = new ArrayList<>();
        for (JobApplication job : jobRepository.findAll()) {
            JobApplicationDto dto = new JobApplicationDto();
            dto.setId(job.getId());   // ← 加上 id
            dto.setCompany(job.getCompany());
            dto.setPosition(job.getPosition());
            dto.setStatus(job.getStatus());
            dto.setAppliedDate(job.getAppliedDate());
            dtoList.add(dto);
        }
        return dtoList;
    }

    // 分页查询功能
    public Page<JobApplicationDto> findAll(int page, int size, String sortBy, String direction) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();
        PageRequest request = PageRequest.of(page, size, sort);

        // 只查属于当前用户的数据
        return jobRepository.findByUserUsername(username, request)
                .map(job -> {
                    JobApplicationDto dto = new JobApplicationDto();
                    dto.setId(job.getId());   // ← 改回 id
                    dto.setCompany(job.getCompany());
                    dto.setPosition(job.getPosition());
                    dto.setStatus(job.getStatus());
                    dto.setAppliedDate(job.getAppliedDate());
                    return dto;
                });
    }

//搜索功能
    public Page<JobApplicationDto> searchByCompany(String keyword, int page, int size) {
        // Service 里取当前用户名再调用 repo
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        PageRequest req = PageRequest.of(page, size);
        return jobRepository.findByCompanyContainingIgnoreCaseAndUser_Username(keyword, username, req)
                .map(job -> {
                    JobApplicationDto dto = new JobApplicationDto();
                    dto.setId(job.getId());   // ← 改回 id
                    dto.setCompany(job.getCompany());
                    dto.setPosition(job.getPosition());
                    dto.setStatus(job.getStatus());
                    dto.setAppliedDate(job.getAppliedDate());
                    return dto;
                });
    }

    public void save(JobApplication job) { // 对应 uploadWithInfo 的最后一行
        jobRepository.save(job);
    }


    // 删除功能
    public void checkOwner(UUID id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        JobApplication job = jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!job.getUser().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        jobRepository.delete(job);
        biz.info("JOB_DELETE user={} jobId={}", username, id);
    }

    public JobApplication findById(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到该职位"));
    }

    // 编辑功能
    public JobApplication getMine(UUID id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        JobApplication job = jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!job.getUser().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return job;
    }

    public void updateJob(UUID id, JobApplicationDto req) {
        JobApplication job = getMine(id); // 已做 owner 校验
        job.setCompany(req.getCompany());
        job.setPosition(req.getPosition());
        job.setStatus(req.getStatus());
        job.setAppliedDate(req.getAppliedDate());

        jobRepository.save(job);
    }
}





