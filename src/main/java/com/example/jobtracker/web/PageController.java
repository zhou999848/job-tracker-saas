package com.example.jobtracker.web;

import com.example.jobtracker.domain.User;
import com.example.jobtracker.dto.JobApplicationDto;
import com.example.jobtracker.dto.NoteDto;
import com.example.jobtracker.repository.JobApplicationRepository;
import com.example.jobtracker.repository.NoteRepository;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.domain.Note;
import com.example.jobtracker.service.NoteService;
import com.example.jobtracker.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.example.jobtracker.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;

@Controller
public class PageController {
    private static final Logger logger = LoggerFactory.getLogger(PageController.class);
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final JobApplicationRepository jobRepo;
    private final NoteRepository noteRepo;
    private final UserRepository userRepo;
    private final NoteService service;
    public PageController(AuthenticationManager authManager, JwtUtil jwtUtil, JobApplicationRepository jobRepo, NoteRepository noteRepo, UserRepository userRepo,NoteService service) {
        this.jobRepo = jobRepo;
        this.noteRepo = noteRepo;
        this.userRepo = userRepo;
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.service = service;
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }


    /**
     * ✅ 显示所有职位页面 / Display all job applications page
     * [GET] /jobs
     */

    public Page<JobApplicationDto> list(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "5") int size,
                                        @RequestParam(defaultValue = "appliedDate") String sortBy,
                                        @RequestParam(defaultValue = "desc") String direction

    ) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("【EN】User {} fetching job list page {} / 【中文】用户 {} 查询职位列表第 {} 页 / 【日本語】ユーザー {} が職務リストのページ {} を取得", username, page, username, page, username, page);
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        PageRequest request = PageRequest.of(page, size, sort);
        return jobRepo.findByUserUsername(username, request)
                .map(job -> {
                    JobApplicationDto dto = new JobApplicationDto();
                    dto.setCompany(job.getCompany());
                    dto.setPosition(job.getPosition());
                    dto.setStatus(job.getStatus());
                    dto.setAppliedDate(job.getAppliedDate());
                    dto.setId(job.getId()); // 添加 ID 字段
                    return dto;
                });
    }

    @GetMapping("/jobs")
    public String showJobs(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "5") int size,
                           @RequestParam(defaultValue = "appliedDate") String sortBy,
                           @RequestParam(defaultValue = "desc") String direction,
                           Model model) {
        String username = getCurrentUsername();
        logger.info("[Show Jobs] User={} 查看职位列表 / Viewing job list", username);
        Page<JobApplicationDto> jobs = list(page, size, sortBy, direction);
        model.addAttribute("jobs", jobs.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", jobs.getTotalPages());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);
        model.addAttribute("pageSize", size);
        return "jobs";  // 指向 templates/jobs.html
    }
///add note is included in the NoteController.java
@GetMapping("/uploadMulti")
public String showAddNoteForm(Model model){
    model.addAttribute("jobs",new JobApplicationDto());
    return "add-note"; // 返回上传页面的视图名
}

    @PostMapping("/uploadMulti")
    public String uploadMulti(@ModelAttribute JobApplicationDto jobDto, @RequestParam("files") List<MultipartFile> files,
                              @RequestParam("jobId") UUID jobId,
                              @RequestParam("content") String content
                             ) throws IOException {

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



        User user = userRepo.findByUsername(username)
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

        return "redirect:/jobs/" + jobId + "/notes"; }// 保存后重定向到该职位的笔记页面



    /**add a new job application
     * ✅ 添加职位申请 / Add a new job application
     * [POST] /jobs/add
     */

    public void addJob(@RequestBody @Valid JobApplicationDto jobDto) {
        logger.info("【EN】Creating job: company={}, position={} / 【中文】创建职位：公司={}，职位={} / 【日本語】職務作成：会社={}、職種={}", jobDto.getCompany(), jobDto.getPosition(), jobDto.getCompany(), jobDto.getPosition(), jobDto.getCompany(), jobDto.getPosition());

        // ① 获取当前登录的用户名
        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // ② 查出 User 实体
        User user = userRepo.findByUsername(username).orElseThrow();

        // ③ 创建 Job 实体并填充数据
        JobApplication job = new JobApplication();
        job.setCompany(jobDto.getCompany());
        job.setPosition(jobDto.getPosition());
        job.setStatus(jobDto.getStatus());
        job.setAppliedDate(jobDto.getAppliedDate());

        // ④ 设置所属用户
        job.setUser(user);

        // ⑤ 保存
        jobRepo.save(job);
    }


    @GetMapping("/jobs/add")
    public String showAddJobForm(Model model) {
        String username = getCurrentUsername();
        logger.info("[Show Add Job Form] User={} 显示添加职位表单 / Display add job form", username);
        model.addAttribute("job", new JobApplicationDto());
        return "add-job";  // 指向 templates/addJob.html
    }

    @PostMapping("/jobs/add")
    public String saveJob(@ModelAttribute JobApplicationDto jobDto) {
        String username = getCurrentUsername();
        logger.info("[Save Job] User={} 保存职位 / Saving job: {}", username, jobDto);
        addJob(jobDto);  // 调用上面的 addJob 方法
        return "redirect:/jobs";  // 保存后重定向到职位列表
    }


    /**
     * ✅ 显示某个职位的笔记页面 / Display notes for a job application
     * [GET] /notes/{jobId}
     */
    public Page<NoteDto> showPagedNotes(@RequestParam UUID jobId,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "3") int size) {
        String username = getCurrentUsername();
        logger.info("【EN】User={} Fetching paged notes / 【中文】用户={} 分页获取笔记 / 【日本語】ユーザー={} がページ取得: jobId={}, page={}", username, username, username, jobId, page);

        PageRequest request = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return noteRepo.findByUserUsernameAndJobId(username, jobId, request)
                .map(note -> {
                    NoteDto dto = new NoteDto();
                    dto.setId(note.getId()); // 添加 ID 字段,delete時用
                    dto.setJobId(note.getJobId());
                    dto.setContent(note.getContent());
                    dto.setCreatedAt(note.getCreatedAt());
                    dto.setFilePaths(note.getFilePaths() == null ? List.of() : new ArrayList<>(note.getFilePaths())); // 设置附件路径
                    logger.info("【EN】Note fetched: id={}, content={}, createdAt={}, filePaths={}", dto.getId(), dto.getContent(), dto.getCreatedAt(), dto.getFilePaths());
                    return dto;
                });
    }

    @GetMapping("/jobs/{jobId}/notes")
    public String showNotes(@PathVariable UUID jobId,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "3") int size,
                            Model model) {
        String username = getCurrentUsername();
        logger.info("[Show Notes] User={} 查看 jobId={} 的笔记 / Viewing notes for jobId={}", username, jobId, jobId);

        Page<NoteDto> notes = showPagedNotes(jobId, page, size);
        model.addAttribute("jobId", jobId);
        model.addAttribute("notes", notes.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notes.getTotalPages());
        model.addAttribute("pageSize", size);
        return "notes";  // 指向 templates/notes.html
    }


    /**
     * 显示登录页面（GET /login）
     * Display login form (GET /login)
     *
     * @param model 用于传递错误信息 Model to pass error messages
     * @return login.html 页面视图 View name for login.html
     */

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "redirect", required = false) String redirect,
                            Model model) {
        if (error != null) model.addAttribute("error", "用户名或密码错误");
        model.addAttribute("redirect", redirect);
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam(required = false) String redirect,
                          HttpServletResponse response,
                          Model model) {
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

            String token = jwtUtil.generateToken(username);
            ResponseCookie cookie = ResponseCookie.from("JWT", token)
                    .httpOnly(true).secure(false)
                    .sameSite("Lax").path("/")
                    .maxAge(24 * 60 * 60)
                    .build();
            response.addHeader("Set-Cookie", cookie.toString());

            // ✅ 兜底：redirect 判空 + 安全检查
            if (redirect != null && !redirect.isBlank()
                    && redirect.startsWith("/")
                    && !redirect.startsWith("/error")
                    && !redirect.startsWith("/login")) {
                return "redirect:" + redirect.trim();
            }
            return "redirect:/jobs"; // ✅ 永远有个默认
        } catch (AuthenticationException e) {
            model.addAttribute("error", "用户名或密码错误");
            model.addAttribute("redirect", redirect);
            return "login";
        } catch (Exception e) {
            model.addAttribute("error", "系统错误，请稍后重试");
            model.addAttribute("redirect", redirect);
            return "login";
        }
    }


    @PostMapping("/logout")
    public String doLogout(HttpServletResponse response) {
        // 本地（http）当前使用：Lax + 非 Secure
        var c1 = org.springframework.http.ResponseCookie.from("JWT", "")
                .httpOnly(true).secure(false).sameSite("Lax").path("/").maxAge(0).build();
        response.addHeader("Set-Cookie", c1.toString());

        // 历史/线上（https）可能使用：None + Secure
        var c2 = org.springframework.http.ResponseCookie.from("JWT", "")
                .httpOnly(true).secure(true).sameSite("None").path("/").maxAge(0).build();
        response.addHeader("Set-Cookie", c2.toString());

        // 保险：Servlet API 再清一次
        var legacy = new jakarta.servlet.http.Cookie("JWT", "");
        legacy.setHttpOnly(true);
        legacy.setPath("/");
        legacy.setMaxAge(0);
        response.addCookie(legacy);

        return "redirect:/login";
    }
}