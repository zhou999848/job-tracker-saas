package com.example.jobtracker.service;

import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.dto.JobApplicationDto;
import com.example.jobtracker.reposiroty.JobApplicationRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@Service
public class JobApplicationService {
    private final JobApplicationRepository repository;

    public JobApplicationService(JobApplicationRepository repository) {
        this.repository = repository;
    }

    public void save(JobApplicationDto dto) {
        JobApplication job = new JobApplication();
        job.setCompany(dto.getCompany());
        job.setPosition(dto.getPosition());
        job.setStatus(dto.getStatus());
        job.setAppliedDate(dto.getAppliedDate());

        repository.save(job);
    }

    public List<JobApplicationDto> findAll() {
        List<JobApplicationDto> dtoList = new ArrayList<>();
        for (JobApplication job : repository.findAll()) {
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
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PageRequest request = PageRequest.of(page, size, sort);

        return repository.findAll(request).map(job -> {
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
        return repository.findByCompanyContainingIgnoreCase(keyword, request)
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
        repository.save(job); // 这里的 repository 是 JPA 注入的
    }

}
