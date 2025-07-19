package com.example.jobtracker.web;

import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.dto.JobApplicationDto;
import com.example.jobtracker.service.JobApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.*;
import java.util.List;


import org.springframework.data.domain.Page;//fenyepaixu

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

import com.example.jobtracker.reposiroty.JobApplicationRepository;//"download"
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.io.FileInputStream;



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
    public void create(@RequestBody @Valid JobApplicationDto dto) {
        service.save(dto);
    }

    @PostMapping("/uploadWithInfo")
    public String uploadWithInfo(@RequestParam("file") MultipartFile file,
                                 @RequestParam("company") String company,
                                 @RequestParam("status") String status,
                                 @RequestParam("position") String position,
                                 @RequestParam("appliedDate") String appliedDateStr) {
        try {
            if (file.isEmpty()) {
                return "文件不能为空";
            }

            // 检查文件类型
            String contentType = file.getContentType();
            if (!contentType.equals("application/pdf")
                    && !contentType.equals("image/jpeg")
                    && !contentType.equals("image/png")) {
                return "只支持 PDF/JPG/PNG 文件";
            }

            // 🔥 获取绝对路径（解决你的问题的关键！）/////！！！！！！
            String basePath = System.getProperty("user.dir");
            String uploadPath = basePath + File.separator + "uploads";


            // 保存文件
            String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
            String fullPath = uploadPath + File.separator + fileName;
            file.transferTo(new File(fullPath));

            // 保存职位信息
            JobApplication job = new JobApplication();
            job.setCompany(company);
            job.setStatus(status);
            job.setPosition(position);
            job.setAppliedDate(LocalDate.parse(appliedDateStr));
            job.setFilePath("uploads/" + fileName); // 相对路径也保存一份

            service.save(job);

            return "上传成功，保存路径为：uploads/" + fileName;

        } catch (IOException e) {
            return "上传失败：" + e.getMessage();
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

    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam("file") String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName())
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
