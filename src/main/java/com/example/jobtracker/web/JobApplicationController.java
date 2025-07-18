package com.example.jobtracker.web;

import com.example.jobtracker.dto.JobApplicationDto;
import com.example.jobtracker.service.JobApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;


import org.springframework.data.domain.Page;//fenyepaixu

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
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
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return "文件不能为空";
            }

            String contentType = file.getContentType();
            if (!contentType.equals("application/pdf")
                    && !contentType.equals("image/jpeg")
                    && !contentType.equals("image/png")) {
                return "只支持 PDF/JPG/PNG 文件";
            }

            String path = "uploads/" + file.getOriginalFilename();
            File dest = new File(path);
            file.transferTo(dest);

            return "上传成功: " + path;
        } catch (Exception e) {
            e.printStackTrace(); // 打印错误堆栈
            return "上传失败: " + e.getMessage();
        }
    }


    @GetMapping("/list")
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
   @GetMapping("/search")
    public Page<JobApplicationDto> search(@RequestParam String keyword,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "5") int size) {
        return service.searchByCompany(keyword, page, size);
    }
}
