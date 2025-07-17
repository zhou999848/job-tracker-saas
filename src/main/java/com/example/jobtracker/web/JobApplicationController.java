package com.example.jobtracker.web;

import com.example.jobtracker.dto.JobApplicationDto;
import com.example.jobtracker.service.JobApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;


import org.springframework.data.domain.Page;//fenyepaixu

@RestController
@RequestMapping("/api/jobs")
public class JobApplicationController {
    private final JobApplicationService service;

    public JobApplicationController(JobApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public void create(@RequestBody @Valid  JobApplicationDto dto) {
        service.save(dto);
    }

    @GetMapping
    public List<JobApplicationDto> list() {
        return service.findAll();
    }
    public Page<JobApplicationDto> listt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "appliedDate") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return service.findAll(page, size, sortBy, direction);
    }
    public Page<JobApplicationDto> search(@RequestParam String keyword,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "5") int size) {
        return service.searchByCompany(keyword, page, size);
    }
}
