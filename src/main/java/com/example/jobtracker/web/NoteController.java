package com.example.jobtracker.web;

import org.springframework.core.io.UrlResource;
import org.springframework.ui.Model;
import com.example.jobtracker.domain.User;
import com.example.jobtracker.dto.JobApplicationDto;
import com.example.jobtracker.dto.NoteDto;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.service.NoteService;
import com.example.jobtracker.domain.Note;

import org.springframework.http.HttpStatus;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notes")
public class NoteController {
    // 创建一个“日志记录器(logger)”，以后可以用它来打印信息到控制台（或日志文件），帮助你调试程序
    private static final Logger logger = LoggerFactory.getLogger(NoteController.class);

    private final NoteService service;
    private final UserRepository userRepository;
    public NoteController(NoteService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
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
    @GetMapping("/uploadMulti")
    public String showAddNoteForm(Model model){
        model.addAttribute("jobs",new JobApplicationDto());
        return "add-note"; // 返回上传页面的视图名
    }

    @PostMapping("/uploadMulti")
    public String uploadMulti(@ModelAttribute JobApplicationDto jobDto, @RequestParam("files") List<MultipartFile> files,
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
            logger.info("即将保存路径: {}", path);   // 新增日志
            savedPaths.add(path);
        }
        logger.info("全部待保存附件路径: {}", savedPaths); // 新增日志



        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用?不存在/未登?"));
        Note note = new Note();
        note.setUser(user);
        note.setJobId(jobId);
        note.setContent(content);
        note.setFilePaths(savedPaths);
        logger.info("Note?象 filePaths: {}", note.getFilePaths()); // 新增日志
        note.setCreatedAt(LocalDateTime.now());
        service.save(note);

        logger.info("【EN】User={} Uploaded {} files / 【中文】用?={} 上? {} 个文件 / 【日本語】ユーザー={} が{} ファイルアップロード: jobId={}", username, savedPaths.size(), username, savedPaths.size(), username, savedPaths.size(), jobId);
        // return ("上?成功，共上? " + savedPaths.size() + " 个文件");
        return "redirect:/jobs";}


        /**
         * ✅ 下载附件 / 添付ファイルをダウンロード / Download Attachment
         * [GET] /api/notes/download?file=xxx
         */
        @GetMapping("/download")
        public ResponseEntity<Resource> downloadFile(@RequestParam("filename") String filename) throws IOException {
            String username = getCurrentUsername();

            // 基础目录：限制只能下载 /uploads/notes 下的文件
            Path baseDir = Paths.get(System.getProperty("user.dir"), "uploads", "notes").normalize();
            Path targetFile = baseDir.resolve(filename).normalize();

            // 安全校验：防止路径穿越（../）
            if (!targetFile.startsWith(baseDir)) {
                logger.warn("【EN】User={} Invalid path / 【中文】用户={} 非法路径 / 【日本語】ユーザー={} 不正パス: {}",
                        username, username, username, filename);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (!Files.exists(targetFile)) {
                logger.warn("【EN】User={} File not found / 【中文】用户={} 文件不存在 / 【日本語】ユーザー={} ファイル不存在: {}",
                        username, username, username, targetFile);
                return ResponseEntity.notFound().build();
            }

            logger.info("【EN】User={} Downloading file={} / 【中文】用户={} 正在下载文件={} / 【日本語】ユーザー={} がファイルをダウンロード: {}",
                    username, filename, username, filename, username, filename);

            UrlResource resource = new UrlResource(targetFile.toUri());

            String contentType = Files.probeContentType(targetFile);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(Files.size(targetFile))
                    .body(resource);
        }


    /**
     * ✅ 删除笔记 / メモを削除 / Delete Note
     * [DELETE] /api/notes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNote(@PathVariable UUID id) {
        String username = getCurrentUsername();
        logger.info("【EN】User={} Deleting note / 【中文】用户={} 删除笔记 / 【日本語】ユーザー={} がメモ削除: id={}", username, username, username, id);
        Note note = service.findById(id);
        service.checkOwner(id);
        return ResponseEntity.ok("删除成功 / 削除成功 / Deleted successfully");
    }

    public class FaviconController {
        @RequestMapping("favicon.ico")
        @ResponseBody
        void returnNoFavicon() {}
    }
}
