package com.example.jobtracker.web;

import com.example.jobtracker.dto.NoteDto;
import com.example.jobtracker.service.NoteService;
import com.example.jobtracker.domain.Note;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/notes")
public class NoteController {
    // 创建一个“日志记录器(logger)”，以后可以用它来打印信息到控制台（或日志文件），帮助你调试程序
    private static final Logger logger = LoggerFactory.getLogger(NoteController.class);
    private final NoteService service;

    public NoteController(NoteService service) {
        this.service = service;
    }

    private String getCurrentUsername() {//获取“当前登录的用户名”，用于后续日志记录或权限判断。
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * ✅ 创建笔记 / メモを作成 / Create Note
     * [POST] /api/notes
     */
    @PostMapping
    public void create(@RequestBody NoteDto dto) {
        String username = getCurrentUsername();

        logger.info("【EN】User={} Creating note / 【中文】用户={} 创建笔记 / 【日本語】ユーザー={} がメモ作成: jobId={}", username, username, username, dto.getJobId());
        service.save(dto);
    }



    /**
     * ✅ 分页获取笔记 / メモをページングで取得 / Get Paged Notes
     * [GET] /api/notes/paged
     */
    @GetMapping("/paged")
    public Page<NoteDto> getPagedNotes(@RequestParam UUID jobId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "3") int size) {
        String username = getCurrentUsername();
        logger.info("【EN】User={} Fetching paged notes / 【中文】用户={} 分页获取笔记 / 【日本語】ユーザー={} がページ取得: jobId={}, page={}", username, username, username, jobId, page);
        return service.findByJobIdPaged(jobId, page, size);
    }

    /**
     * ✅ 批量创建笔记 / メモを一括作成 / Batch Create Notes
     * [POST] /api/notes/batch
     */
    @PostMapping("/batch")
    public void batchCreate(@RequestBody NoteDto dto) {

        service.save(dto);
    }

    /**
     * ✅ 上传多个附件并保存笔记 / ファイルを複数アップロードしてメモ保存 / Upload Files with Note
     * [POST] /api/notes/uploadMulti
     */
    @PostMapping("/uploadMulti")
    public String uploadMulti(@RequestParam("files") List<MultipartFile> files,
                              @RequestParam("jobId") UUID jobId,
                              @RequestParam("content") String content) throws IOException {

        String username = getCurrentUsername();
        List<String> savedPaths = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String contentType = file.getContentType();
            if (!contentType.startsWith("image/") && !contentType.startsWith("audio/") && !contentType.equals("application/pdf")) {
                continue;
            }

            String path = System.getProperty("user.dir") + "/uploads/notes/" + file.getOriginalFilename();
            File dest = new File(path);
            file.transferTo(dest);
            savedPaths.add(path);
        }

        Note note = new Note();
        note.setJobId(jobId);
        note.setContent(content);
        note.setFilePaths(savedPaths);
        note.setCreatedAt(LocalDateTime.now());
        service.save(note);

        logger.info("【EN】User={} Uploaded {} files / 【中文】用户={} 上传 {} 个文件 / 【日本語】ユーザー={} が{} ファイルアップロード: jobId={}", username, savedPaths.size(), username, savedPaths.size(), username, savedPaths.size(), jobId);
        return ("上传成功，共上传 " + savedPaths.size() + " 个文件");
    }

    /**
     * ✅ 下载附件 / 添付ファイルをダウンロード / Download Attachment
     * [GET] /api/notes/download?file=xxx
     */
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("file") String filePath) throws IOException {
        String username = getCurrentUsername();
        File file = new File(filePath);
        if (!file.exists()) {
            logger.warn("【EN】User={} File not found / 【中文】用户={} 文件不存在 / 【日本語】ユーザー={} ファイルが存在しない: {}", username, username, username, filePath);
            return ResponseEntity.notFound().build();
        }

        logger.info("【EN】User={} Downloading file / 【中文】用户={} 下载文件 / 【日本語】ユーザー={} がファイルダウンロード: {}", username, username, username, filePath);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName())
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * ✅ 删除笔记 / メモを削除 / Delete Note
     * [DELETE] /api/notes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteJob(@PathVariable UUID id) {
        String username = getCurrentUsername();
        logger.info("【EN】User={} Deleting note / 【中文】用户={} 删除笔记 / 【日本語】ユーザー={} がメモ削除: id={}", username, username, username, id);
        Note note = service.findById(id);
        service.checkOwner(id);
        return ResponseEntity.ok("删除成功 / 削除成功 / Deleted successfully");
    }
}
