package com.example.jobtracker.service;

import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.domain.Note;
import com.example.jobtracker.domain.User;
import com.example.jobtracker.dto.JobApplicationDto;
import com.example.jobtracker.repository.JobApplicationRepository;
import com.example.jobtracker.repository.UserRepository;
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
    private final JobApplicationRepository jobRepository;
    private final UserRepository userRepository;


    public JobApplicationService(JobApplicationRepository jobRepository, UserRepository userRepository) {
        this.jobRepository= jobRepository;this.userRepository = userRepository;
    }
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

        // ④ 设置所属用户
        job.setUser(user);

        // ⑤ 保存
        jobRepository.save(job);
    }

    public List<JobApplicationDto> findAll() {
        List<JobApplicationDto> dtoList = new ArrayList<>();
        for (JobApplication job : jobRepository.findAll()) {
            JobApplicationDto dto = new JobApplicationDto();
            dto.setCompany(job.getCompany());
            dto.setPosition(job.getPosition());
            dto.setStatus(job.getStatus());
            dto.setAppliedDate(job.getAppliedDate());
            dtoList.add(dto);
        }
        return dtoList;
    }
    public Page<JobApplicationDto> findAll(int page, int size, String sortBy, String direction) {
        // ?取当前用?名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PageRequest request = PageRequest.of(page, size, sort);

        // 只??属于?个用?的数据
        return jobRepository.findByUserUsername(username, request)
                .map(job -> {
                    JobApplicationDto dto = new JobApplicationDto();
                    dto.setCompany(job.getCompany());
                    dto.setPosition(job.getPosition());
                    dto.setStatus(job.getStatus());
                    dto.setAppliedDate(job.getAppliedDate());
                    return dto;
                });
    }
    public Page<JobApplicationDto> searchByCompany(String keyword, int page, int size) {
        // 创建分页参数（默认不排序）
        PageRequest request = PageRequest.of(page, size);
        // 查询并转换为 DTO
        return jobRepository.findByCompanyContainingIgnoreCase(keyword, request)
                .map(job -> {
                    JobApplicationDto dto = new JobApplicationDto();
                    dto.setCompany(job.getCompany());
                    dto.setPosition(job.getPosition());
                    dto.setStatus(job.getStatus());
                    dto.setAppliedDate(job.getAppliedDate());
                    return dto;
                });
    }

    public void save(JobApplication job) {//对应uploadWithInfo的最后一行的!!!!
        jobRepository.save(job); // 这里的 repository 是 JPA 注入的
    }

    public void checkOwner(UUID id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();//获取当前登录用户

        JobApplication job=jobRepository.findById(id)//加载note，并检查是不是当前用户的
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!job.getUser().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        jobRepository.delete(job);
    }

    public JobApplication findById(UUID id) {//4-5対応controller。findById
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到该职位"));
    }

}

