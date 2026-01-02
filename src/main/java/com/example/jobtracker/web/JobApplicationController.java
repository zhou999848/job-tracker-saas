package com.example.jobtracker.web;

import ch.qos.logback.core.model.Model;
import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.dto.JobApplicationDto;
import com.example.jobtracker.security.JwtUtil;
import com.example.jobtracker.service.JobApplicationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;


import org.springframework.data.domain.Page;//fenyepaixu

import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.io.FileInputStream;



import java.io.File;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestController
@RequestMapping("/api/jobs")
public class JobApplicationController {

    private static final Logger logger = LoggerFactory.getLogger(JobApplicationController.class);
    private JobApplicationService service;
    private JwtUtil jwtUtil;

    public JobApplicationController(JobApplicationService service) {
        this.service = service;
    }

    /**
     * ✅ 创建职位 / 職務を作成する / Create Job
     * [POST] /api/jobs
     */
    @PostMapping
    public void addJob(@RequestBody @Valid JobApplicationDto dto) {
        logger.info("【EN】Creating job: company={}, position={} / 【中文】创建职位：公司={}，职位={} / 【日本語】職務作成：会社={}、職種={}", dto.getCompany(), dto.getPosition(), dto.getCompany(), dto.getPosition(), dto.getCompany(), dto.getPosition());
        service.save(dto);
    }

    /**
     * ✅ 上传职位信息+文件 / 情報とファイルをアップロード / Upload Job Info with File
     * [POST] /api/jobs/uploadWithInfo
     */
    @PostMapping("/uploadWithInfo")
    public String uploadWithInfo(@RequestParam("file") MultipartFile file,
                                 @RequestParam("company") String company,
                                 @RequestParam("status") String status,
                                 @RequestParam("position") String position,
                                 @RequestParam("appliedDate") String appliedDateStr) {
        try {
            if (file.isEmpty()) {
                return "【中文】文件不能为空 / 【日本語】ファイルが空です / 【EN】File cannot be empty";
            }

            String contentType = file.getContentType();
            if (!contentType.equals("application/pdf") &&
                    !contentType.equals("image/jpeg") &&
                    !contentType.equals("image/png")) {
                return "【中文】只支持 PDF/JPG/PNG 文件 / 【日本語】PDF、JPG、PNG のみ対応 / 【EN】Only PDF/JPG/PNG files are allowed";
            }

            String projectPath = System.getProperty("user.dir");
            String path = projectPath + "/uploads/" + file.getOriginalFilename();
            File dest = new File(path);
            file.transferTo(dest);

            JobApplication job = new JobApplication();
            job.setCompany(company);
            job.setStatus(status);
            job.setPosition(position);
            job.setAppliedDate(LocalDate.parse(appliedDateStr));
            job.setFilePath(path);

            service.save(job);

            logger.info("【EN】Uploaded file={} and saved job={} / 【中文】上传文件={}，保存职位={} / 【日本語】ファイル={} をアップロードし、職務={} を保存", file.getOriginalFilename(), company, file.getOriginalFilename(), company, file.getOriginalFilename(), company);
            return "上传成功 / アップロード成功 / Upload success：uploads/" + path;

        } catch (IOException e) {
            logger.error("【EN】Upload failed: {} / 【中文】上传失败：{} / 【日本語】アップロード失敗：{}", e.getMessage(), e.getMessage(), e.getMessage());
            return "上传失败：" + e.getMessage();
        }
    }

    /**
     * ✅ 分页+排序+查询 / ページング・ソート・検索 / List Jobs with Paging & Sorting
     * [GET] /api/jobs
     */
    @GetMapping
    public Page<JobApplicationDto> list(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "5") int size,
                                        @RequestParam(defaultValue = "appliedDate") String sortBy,
                                        @RequestParam(defaultValue = "desc") String direction) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("【EN】User {} fetching job list page {} / 【中文】用户 {} 查询职位列表第 {} 页 / 【日本語】ユーザー {} が職務リストのページ {} を取得", username, page, username, page, username, page);
        return service.findAll(page, size, sortBy, direction);
    }

    /**
     * ✅ 搜索职位 / 職務を検索 / Search Jobs
     * [GET] /api/jobs/search
     */
    @GetMapping("/search")
    public Page<JobApplicationDto> search(@RequestParam String keyword,

                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "5") int size) {
        logger.info("【EN】Searching job with keyword={} / 【中文】搜索职位关键字={} / 【日本語】職務キーワード検索={}", keyword, keyword, keyword);
        return service.searchByCompany(keyword, page, size);
    }

    /**
     * ✅ 下载文件 / 添付ファイルをダウンロード / Download Attachment
     * [GET] /api/jobs/download
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam("file") String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            logger.warn("【EN】File not found: {} / 【中文】文件不存在：{} / 【日本語】ファイルが存在しない：{}", filePath, filePath, filePath);
            return ResponseEntity.notFound().build();
        }

        logger.info("【EN】Downloading file: {} / 【中文】下载文件：{} / 【日本語】ダウンロード中のファイル：{}", filePath, filePath, filePath);

        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName())
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * ✅ 删除职位 / 職務を削除 / Delete Job
     * [DELETE] /api/jobs/{id}
     */
    @DeleteMapping("{id}")
    public ResponseEntity<?> deleteJob(@PathVariable UUID id) {
        logger.info("【EN】Deleting job ID={} / 【中文】删除职位 ID={} / 【日本語】職務 ID={} を削除", id, id, id);
        JobApplication job = service.findById(id);
        service.checkOwner(id);
         return ResponseEntity.ok("删除成功 / 削除成功 / Deleted successfully");

    }


}
