package com.example.jobtracker.web;

import com.example.jobtracker.domain.User;
import com.example.jobtracker.dto.ChangePasswordRequest;
import com.example.jobtracker.dto.JobApplicationDto;
import com.example.jobtracker.dto.NoteDto;
import com.example.jobtracker.dto.UpdateProfileRequest;
import com.example.jobtracker.repository.JobApplicationRepository;
import com.example.jobtracker.repository.NoteRepository;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.domain.Note;
import com.example.jobtracker.security.LoginAttemptService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PageController {
    private static final Logger logger = LoggerFactory.getLogger(PageController.class);
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final JobApplicationRepository jobRepo;
    private final NoteRepository noteRepo;
    private final UserRepository userRepo;
    private final NoteService service;
    private final UserService userService;
private final LoginAttemptService loginAttemptService;
    public PageController(AuthenticationManager authManager, JwtUtil jwtUtil, JobApplicationRepository jobRepo, NoteRepository noteRepo, UserRepository userRepo, NoteService service,UserService userService, LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
        this.jobRepo = jobRepo;
        this.noteRepo = noteRepo;
        this.userRepo = userRepo;
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.service = service;
        this.userService = userService;
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

    /// add note is included in the NoteController.java
    @GetMapping("/uploadMulti")
    public String showAddNoteForm(Model model) {
        model.addAttribute("jobs", new JobApplicationDto());
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

        return "redirect:/jobs/" + jobId + "/notes";
    }// 保存后重定向到该职位的笔记页面


    /**
     * add a new job application
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

    // 假设这是你的 LoginController.java 里的方法
    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam(required = false) String redirect,
                          HttpServletResponse response,
                          HttpServletRequest request,
                          Model model) {

        // —— 1) 先做空白/格式处理（可选）
        String uname = (username == null) ? "" : username.trim();

        // —— 2) 防爆破：检查是否已被锁定
        if (loginAttemptService.isBlocked(uname)) {  // ← 需要注入 LoginAttemptService
            model.addAttribute("error", "尝试次数过多，账户已暂时锁定");
            model.addAttribute("redirect", redirect);
            return "login";
        }

        try {
            // —— 3) 调用认证
            authManager.authenticate(new UsernamePasswordAuthenticationToken(uname, password));

            // —— 4) 登录成功：清除失败计数
            loginAttemptService.loginSucceeded(uname);

            // ✅ 生成两种 token（你已有 JwtUtil，直接用）
            String access  = jwtUtil.generateAccessToken(uname);   // 短期，比如 15 分钟
            String refresh = jwtUtil.generateRefreshToken(uname);  // 长期，比如 7 天

            // ✅ 本地 http 调试 secure(false)；部署到 HTTPS 再改 true
            boolean secure = request.isSecure(); // 本地一般是 false
            // 你也可以开发期强制：secure = false;

            // ✅ 写两枚 Cookie（注意 addHeader 调两次，不要用 setHeader）
            ResponseCookie accessCookie = ResponseCookie.from("ACCESS", access)
                    .httpOnly(true).secure(secure).sameSite("Lax")
                    .path("/")                 // 业务请求都会带
                    .maxAge(15 * 60)          // 示例：15 分钟
                    .build();

            ResponseCookie refreshCookie = ResponseCookie.from("REFRESH", refresh)
                    .httpOnly(true).secure(secure).sameSite("Lax")
                    .path("/api/auth/refresh") // 仅刷新接口会带
                    .maxAge(7 * 24 * 3600)     // 示例：7 天
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            // ✅ 原有跳转逻辑保持
            String requestUri = request.getRequestURI();
            model.addAttribute("basePath", requestUri);
            model.addAttribute("redirect", redirect);
            if (redirect != null && !redirect.isBlank()
                    && redirect.startsWith("/")
                    && !redirect.startsWith("/error")
                    && !redirect.startsWith("/login")) {
                return "redirect:" + redirect.trim();
            }
            return "redirect:/jobs";

        } catch (BadCredentialsException e) {
            // —— 5) 认证失败：累计失败次数
            loginAttemptService.loginFailed(uname);
            model.addAttribute("error", "用户名或密码错误");
            model.addAttribute("redirect", redirect);
            return "login";

        } catch (LockedException e) {
            // （可选分支）如果你的 AuthenticationProvider 内部也会抛 LockedException，可单独处理
            model.addAttribute("error", "尝试次数过多，账户已暂时锁定");
            model.addAttribute("redirect", redirect);
            return "login";

        } catch (AuthenticationException e) {
            // 其他认证类异常：也算一次失败
            loginAttemptService.loginFailed(uname);
            model.addAttribute("error", "登录失败，请重试");
            model.addAttribute("redirect", redirect);
            return "login";

        } catch (Exception e) {
            model.addAttribute("error", "系统错误，请稍后重试");
            model.addAttribute("redirect", redirect);
            return "login";
        }
    }


    // 建议放在同一个 Controller 里
    private void clearAllAuthCookies(HttpServletResponse response) {
        // 1) 访问令牌：ACCESS（路径 /）
        // 本地 http（Secure=false, SameSite=Lax）
        response.addHeader("Set-Cookie", "ACCESS=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        // 线上 https（Secure=true, SameSite=None）
        response.addHeader("Set-Cookie", "ACCESS=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=None");

        // 2) 刷新令牌：REFRESH（路径 /api/auth/refresh）
        // 本地 http
        response.addHeader("Set-Cookie", "REFRESH=; Path=/api/auth/refresh; Max-Age=0; HttpOnly; SameSite=Lax");
        // 线上 https
        response.addHeader("Set-Cookie", "REFRESH=; Path=/api/auth/refresh; Max-Age=0; HttpOnly; Secure; SameSite=None");

        // 3) 兼容历史：若曾用过单一 JWT 名称，顺手清掉（路径 /）
        response.addHeader("Set-Cookie", "JWT=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        response.addHeader("Set-Cookie", "JWT=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=None");
    }

    @PostMapping("/logout")
    public String doLogout(HttpServletResponse response, HttpServletRequest req) {
        // 1. 让 Spring Session 失效（如果没开 session，这步也安全无害）
        try {
            req.getSession(false); // 若无会话不创建
            if (req.getSession(false) != null) {
                req.getSession(false).invalidate();
            }
        } catch (IllegalStateException ignore) {}

        // 2. 主动清 JSESSIONID（覆盖 http/https 两种可能）
        response.addHeader("Set-Cookie", "JSESSIONID=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        response.addHeader("Set-Cookie", "JSESSIONID=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=None");

        // 3. 清除认证相关 Cookie（ACCESS / REFRESH / 兼容旧 JWT）
        clearAllAuthCookies(response);

        // 4. 回登录页
        return "redirect:/login";
    }


    @GetMapping("/profile")
        public String profile(Model model) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepo.findByUsername(username).orElseThrow();
            model.addAttribute("me", user);
            return "profile";
        }

        @PostMapping("/profile/update")
        public String updateProfile(@Valid UpdateProfileRequest req, RedirectAttributes ra) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            userService.updateProfile(username, req);
            ra.addFlashAttribute("ok", "资料已更新");
            return "redirect:/profile";
        }

    @PostMapping("/profile/change-password")
    public String changePassword(@Valid ChangePasswordRequest req,
                                 HttpServletResponse response,
                                 RedirectAttributes ra) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        userService.changePassword(username, req); // 这里内部已更新 passwordChangedAt

        // 修改密码后：清除所有认证 Cookie（强制所有端重新登录）
        clearAllAuthCookies(response);

        ra.addFlashAttribute("ok", "密码已修改，请重新登录");
        return "redirect:/login";
    }
    @GetMapping("/error-test")
    public String errorTest() {
        // 故意抛异常
        throw new RuntimeException("手动触发异常");
    }@GetMapping("/encoding-test") @ResponseBody
    public String encoding() {
        return "中文OK 日本語OK ひらがなカタカナ";
    }

}





