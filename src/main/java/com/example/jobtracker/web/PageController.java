package com.example.jobtracker.web;

import com.example.jobtracker.domain.User;
import com.example.jobtracker.dto.*;
import com.example.jobtracker.repository.JobApplicationRepository;
import com.example.jobtracker.repository.NoteRepository;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.domain.Note;
import com.example.jobtracker.security.LoginAttemptService;
import com.example.jobtracker.service.CurrentTenant;
import com.example.jobtracker.service.JobApplicationService;
import com.example.jobtracker.service.NoteService;
import com.example.jobtracker.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.file.Path;
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final JobApplicationService jobService;
    private final NoteService noteService;
    private final UserService userService;
private final LoginAttemptService loginAttemptService;
    private final CurrentTenant currentTenant;
    public PageController(AuthenticationManager authManager, JwtUtil jwtUtil, JobApplicationRepository jobRepo, NoteRepository noteRepo, UserRepository userRepo, NoteService noteService,UserService userService, LoginAttemptService loginAttemptService,JobApplicationService jobService, CurrentTenant currentTenant) {

        this.loginAttemptService = loginAttemptService;
        this.jobRepo = jobRepo;
        this.noteRepo = noteRepo;
        this.userRepo = userRepo;
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.noteService = noteService;
        this.userService = userService;
        this.jobService = jobService;
        this.currentTenant = currentTenant;
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
    /**
     * ✅ 显示所有职位页面 / Display all job applications page
     * [GET] /jobs
     */

    @GetMapping("/jobs")
    public String showJobs(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "5") int size,
                           @RequestParam(defaultValue = "appliedDate") String sortBy,
                           @RequestParam(defaultValue = "desc") String direction,
                          Model model) {
        String username = getCurrentUsername();
        logger.info("[Show Jobs] User={} 查看职位列表 / Viewing job list", username);
        Page<JobApplicationDto> jobs = jobService.findAll(page, size, sortBy, direction);
        model.addAttribute("jobs", jobs.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", jobs.getTotalPages());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);
        model.addAttribute("pageSize", size);

        return "jobs";  // 指向 templates/jobs.html
    }
    /**
     * add a new job application
     * ✅ 添加职位申请 / Add a new job application
     * [POST] /jobs/add
     */

    @GetMapping("/jobs/add")
    public String showAddJobForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new JobApplicationDto());
        }
        return "add-job";
    }

    @PostMapping("/jobs/add")
    public String saveJob(@ModelAttribute("form") @Valid JobApplicationDto jobDto,
                          BindingResult br,
                          RedirectAttributes ra) {
        if (br.hasErrors()) {
            // 直接返回 add-job 模板，让 Thymeleaf 读取 BindingResult 渲染错误
            return "add-job";
        }
        jobService.save(jobDto);
        ra.addFlashAttribute("ok", "职位已创建");
        return "redirect:/jobs";
    }



    // 打开编辑页
    @GetMapping("/jobs/edit/{id}")
    public String editPage(@PathVariable UUID id, Model model, Principal principal) {
        // 可选：校验归属
        JobApplication job = jobService.getMine(id);

        // 如果 model 里没有现成的 form（比如从 POST 校验失败回显过来），则创建一个
        if (!model.containsAttribute("form")) {
            JobApplicationDto form = new JobApplicationDto();
            form.setId(job.getId());                 // 注意携带 id
            form.setCompany(job.getCompany());
            form.setPosition(job.getPosition());
            form.setStatus(job.getStatus());
            form.setAppliedDate(job.getAppliedDate());
            model.addAttribute("form", form);
        }
        model.addAttribute("jobId", id);
        return "jobs-edit";
    }

    // 提交更新（POST）
    @PostMapping("/jobs/edit/{id}")
    public String doEdit(@PathVariable UUID id,
                         @ModelAttribute("form") @Valid JobApplicationDto form,
                         BindingResult br,
                         Model model,
                         RedirectAttributes ra,
                         Principal principal) {

        // 确保 DTO 中的 id 与路径参数一致（防止篡改）
        form.setId(id);

        if (br.hasErrors()) {
            // 直接返回视图，这样 Thymeleaf 才能读取 BindingResult 并展示各字段 message
            model.addAttribute("jobId", id);
            return "jobs-edit";
        }

        // 可选：再次确认权限/归属
        jobService.updateJob(id, form);

        ra.addFlashAttribute("ok", "职位已更新");
        return "redirect:/jobs";
    }

    /** 首次进入搜索页 */
    @GetMapping("/jobs/search")
    public String searchPage(Model model,
                             @RequestParam(value = "keyword", required = false) String keyword,
                             @RequestParam(value = "page", defaultValue = "0") int page,
                             @RequestParam(value = "size", defaultValue = "5") int size) {

        // 允许直接 GET 携带参数进行搜索；没带就显示空页面
        if (keyword != null && !keyword.isBlank()) {
            Page<JobApplicationDto> result = jobService.searchByCompany(keyword.trim(),
                    Math.max(page, 0), Math.max(size, 1));
            model.addAttribute("result", result);
        }

        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("page", Math.max(page, 0));
        model.addAttribute("size", Math.max(size, 1));
        return "jobs-search";
    }

    /** 表单提交（POST），仍然只用 @RequestParam，不用表单类 */
    @PostMapping("/jobs/search")
    public String doSearch(@RequestParam String keyword,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "5") int size,
                           Model model) {

        Page<JobApplicationDto> result =
                jobService.searchByCompany(keyword.trim(), Math.max(page, 0), Math.max(size, 1));

        model.addAttribute("result", result);
        model.addAttribute("keyword", keyword);
        model.addAttribute("page", Math.max(page, 0));
        model.addAttribute("size", Math.max(size, 1));
        return "jobs-search";
    }










    /**
     * ✅ 显示某个职位的笔记页面 / Display notes for a job application
     * [GET] /notes/{jobId}
     */

    @GetMapping("/jobs/{jobId}/notes")
    public String showNotes(@PathVariable UUID jobId,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "3") int size,

                            Model model) {
        String username = getCurrentUsername();
        logger.info("[Show Notes] User={} 查看 jobId={} 的笔记 / Viewing notes for jobId={}", username, jobId, jobId);
        Page<NoteDto> notes = noteService.findByJobIdPaged(jobId, page, size); // 使用

        model.addAttribute("jobId", jobId);
        model.addAttribute("notes", notes.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", notes.getTotalPages());
        model.addAttribute("pageSize", size);

        return "notes";  // 指向 templates/notes.html
    }

    /// add note is included in the NoteController.java
    @GetMapping("/uploadMulti")
    public String showAddNoteForm(@RequestParam UUID jobId,Model model) {

        model.addAttribute("jobs", new JobApplicationDto());
        model.addAttribute("jobId", jobId);
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
            String fileName = file.getOriginalFilename();
            String path = System.getProperty("user.dir") + "/uploads/notes/" + file.getOriginalFilename();
            File dest = new File(path);
            file.transferTo(dest);
            logger.info("即将保存路径: {}", path);   // 新增日志
            // 保存到数据库的不要存 path，而是存 fileName
            savedPaths.add(fileName);
        }
        logger.info("全部待保存附件路径: {}", savedPaths); // 新增日志

        UUID tenantId = currentTenant.requireTenantId();
        User user = userRepo.findByTenantIdAndUsername(tenantId,username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用?不存在/未登?"));
        Note note = new Note();
        note.setUser(user);
        note.setJobId(jobId);
        note.setContent(content);
        note.setFilePaths(savedPaths);
        logger.info("Note?象 filePaths: {}", note.getFilePaths()); // 新增日志
        note.setCreatedAt(LocalDateTime.now());
        noteService.save(note);

        logger.info("【EN】User={} Uploaded {} files / 【中文】用?={} 上? {} 个文件 / 【日本語】ユーザー={} が{} ファイルアップロード: jobId={}", username, savedPaths.size(), username, savedPaths.size(), username, savedPaths.size(), jobId);
        // return ("上?成功，共上? " + savedPaths.size() + " 个文件");

        return "redirect:/jobs/" + jobId + "/notes";
    }// 保存后重定向到该职位的笔记页面
    /// add note is included in the NoteController.java

    // 打开编辑页（带上 jobId 便于保存后回跳）
    @GetMapping("/notes/edit/{id}")
    public String editNotePage(@PathVariable UUID id,
                               @RequestParam("jobId") UUID jobId,
                               Model model,
                               Principal principal) {

        // 1. 获取当前登录用户
        String username = principal.getName();
        UUID tenantId = currentTenant.requireTenantId();
        User user = userRepo.findByTenantIdAndUsername(tenantId,username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在/未登录"));

        // 2. 查找笔记
        Note note = noteService.findById(id);
        if (note == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "笔记不存在");
        }

        // 3. 权限检查：只能本人编辑自己的笔记
        if (!note.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "没有权限编辑此笔记");
        }

        // 4. 如果数据库里存的是文件名，就可以直接显示，不用截取路径
        List<String> fileNames = note.getFilePaths();

        // 5. 将数据放入 model，传给 Thymeleaf 模板
        model.addAttribute("note", note);
        model.addAttribute("jobId", jobId);
        model.addAttribute("fileNames", fileNames);

        return "notes-edit";  // 对应 templates/edit-note.html
    }

    // 提交保存
    @PostMapping("/notes/edit/{id}")
    public String updateNote(
            @PathVariable UUID id,
            @RequestParam("jobId") UUID jobId,
            @RequestParam("content") String content,
            @RequestParam(name = "newFiles", required = false) List<MultipartFile> newFiles,
            @RequestParam(name = "removeFiles", required = false) List<String> removeFiles
    ) throws IOException {

        String username = getCurrentUsername();

        // 1) 取原笔记
        Note note = noteService.findById(id);
        if (note == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "笔记不存在");
        }

        // （可选）安全：确保此笔记属于当前职位/当前用户
        if (!jobId.equals(note.getJobId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "jobId 不匹配");
        }

        // 2) 更新内容
        note.setContent(content);

        // 3) 处理删除：从磁盘删、从列表移除
        if (removeFiles != null && !removeFiles.isEmpty()) {
            for (String p : removeFiles) {
                try {
                    if (p != null && !p.isBlank()) {
                        File f = new File(p);
                        if (f.exists()) f.delete();
                        logger.info("已删除旧附件: {}", p);
                    }
                } catch (Exception ex) {
                    logger.warn("删除旧附件失败: {}", p, ex);
                }
            }
            if (note.getFilePaths() != null) {
                note.getFilePaths().removeIf(p -> p != null && removeFiles.contains(p));
            }
        }

        // 4) 处理新增：与 uploadMulti 保持一致
        List<String> filePaths = note.getFilePaths();
        if (filePaths == null) filePaths = new ArrayList<>();

        if (newFiles != null) {
            Path uploadRoot = Paths.get(System.getProperty("user.dir"), "uploads", "notes");
            Files.createDirectories(uploadRoot);

            for (MultipartFile file : newFiles) {
                if (file == null || file.isEmpty()) continue;

                String contentType = file.getContentType();
                if (contentType == null) continue;
                if (!contentType.startsWith("image/") &&
                        !contentType.startsWith("audio/") &&
                        !contentType.equals("application/pdf")) {
                    // 跳过不支持类型
                    continue;
                }

                String fileName = file.getOriginalFilename();
                // 防覆盖：文件名加 UUID 前缀
                String uniqueName = UUID.randomUUID() + "_" +
                        (fileName == null ? "unnamed" : org.springframework.util.StringUtils.cleanPath(fileName));

                Path dest = uploadRoot.resolve(uniqueName);
                file.transferTo(dest.toFile());

                String savedPath = dest.toString();
                logger.info("编辑页新增附件已保存: {}", savedPath);
                // 保存到数据库的不要存 path，而是存 fileName
                filePaths.add(fileName);
            }
        }

        // 5) 收尾 & 保存
        // 清理空路径
        filePaths.removeIf(p -> p == null || p.isBlank());
        note.setFilePaths(filePaths);
        note.setCreatedAt(note.getCreatedAt() == null ? LocalDateTime.now() : note.getCreatedAt());
        // （可选）可加 updatedAt
        // note.setUpdatedAt(LocalDateTime.now());

        noteService.save(note);

        logger.info("【Edit Note】User={} 更新笔记 id={}, 新增附件 {} 个, 现有总数 {} 个, jobId={}",
                username,
                id,
                (newFiles == null ? 0 : newFiles.stream().filter(f -> f != null && !f.isEmpty()).count()),
                note.getFilePaths().size(),
                jobId
        );

        return "redirect:/jobs/" + jobId + "/notes";
    }




    // 显示注册页面
    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("userDto")) {
            model.addAttribute("userDto", new UserDto()); // 你的表单类
        }

        return "register";  // 对应 templates/register.html
    }

    // 接收表单提交
    @PostMapping("/register")
    public String register(@ModelAttribute("userDto") @Valid UserDto dto,
                           BindingResult br,
                           RedirectAttributes ra) {
            if (br.hasErrors()) {
                // 直接返回 add-job 模板，让 Thymeleaf 读取 BindingResult 渲染错误
                return "register";
            }
        logger.info("[Register] username={} - 用户注册", dto.getUsername());
        userService.register(dto);
        ra.addFlashAttribute("ok", "注册成功！");
        return "redirect:/login";   // 注册完成后跳到登录页
    }
    /**
     * 显示登录页面（GET /login）
     * Display login form (GET /login)
     *
     * @param model 用于传递错误信息 Model to pass error messages
     * @return login.html 页面视图 View name for login.html
     */

    @GetMapping("/login")
    public String loginPage(//@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "redirect", required = false) String redirect,
                            Model model) {
       // if (error != null) model.addAttribute("error", "用户名或密码错误");
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
                          Model model,
                          LoginRequest req) {

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
            String access = jwtUtil.generateAccessToken(req.getTenantId(),uname);   // 短期，比如 15 分钟
            String refresh = jwtUtil.generateRefreshToken(req.getTenantId(),uname);  // 长期，比如 7 天

            // ✅ 本地 http 调试 secure(false)；部署到 HTTPS 再改 true
            boolean secure = request.isSecure(); // 本地一般是 false
            // 你也可以开发期强制：secure = false;

            // ✅ 写两枚 Cookie（注意 addHeader 调两次，不要用 setHeader）
            ResponseCookie accessCookie = ResponseCookie.from("ACCESS", access)
                    .httpOnly(true).secure(false).sameSite("Lax")
                    .path("/")                 // 业务请求都会带
                    .maxAge(15 * 60)          // 示例：15 分钟
                    .build();

            ResponseCookie refreshCookie = ResponseCookie.from("REFRESH", refresh)
                    .httpOnly(true).secure(false).sameSite("Lax")
                    .path("/") // 仅刷新接口会带
                    .maxAge(7 * 24 * 3600)     // 示例：7 天
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
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
            // 5) 认证失败：累计失败次数
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
        response.addHeader("Set-Cookie", "REFRESH=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
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
    public String profile(Model model, Principal principal) {
        if (principal != null) {
            var meOpt = userService.findByUsername(principal.getName());
            model.addAttribute("me", meOpt.orElse(null));
        }
        return "profile";
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





