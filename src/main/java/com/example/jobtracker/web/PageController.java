package com.example.jobtracker.web;

import com.example.jobtracker.TenantInvite7a4.*;
import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.domain.User;
import com.example.jobtracker.dto.*;
import com.example.jobtracker.repository.JobApplicationRepository;
import com.example.jobtracker.repository.NoteRepository;
import com.example.jobtracker.repository.TenantRepository;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.domain.Note;
import com.example.jobtracker.security.LoginAttemptService;
import com.example.jobtracker.service.*;
import com.example.jobtracker.ああ７a５.ChangeRoleForm;
import com.example.jobtracker.ああ７a５.MemberRoleService;
import com.example.jobtracker.ああ７a５.RenameTenantForm;
import com.example.jobtracker.ああ７a５.TenantProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.file.Path;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
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
    private final TenantRepository tenantRepo;
    private final TenantAdminService tenantAdminService;
    private final MemberRoleService memberRoleService;
    private final TenantProfileService tenantProfileService;
    private final TenantService tenantService;
    public PageController(AuthenticationManager authManager, JwtUtil jwtUtil, JobApplicationRepository jobRepo, NoteRepository noteRepo, UserRepository userRepo, NoteService noteService,UserService userService, LoginAttemptService loginAttemptService,JobApplicationService jobService, CurrentTenant currentTenant, TenantRepository tenantRepo, TenantAdminService tenantAdminService, MemberRoleService memberRoleService, TenantProfileService tenantProfileService, TenantService tenantService) {
        this.tenantService = tenantService;
this .tenantProfileService = tenantProfileService;
        this.memberRoleService = memberRoleService;
this.tenantAdminService = tenantAdminService;
        this.tenantRepo = tenantRepo;

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
        model.addAttribute("totalJobs", jobs.getTotalElements());


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
        model.addAttribute("totalNotes", notes.getTotalElements());


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



    @GetMapping("/register")
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public String registerPage(Model model) {

        // 当前租户 ID（必要）
        UUID tenantId = currentTenant.requireTenantId();
        model.addAttribute("tenantId", tenantId);

        // 若没有表单对象则初始化
        if (!model.containsAttribute("userDto")) {
            UserDto dto = new UserDto();
            dto.setRole("USER"); // 默认角色（可按需）
            model.addAttribute("userDto", dto);
        }

        return "register";
    }
    @PostMapping("/register")
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public String register(@ModelAttribute("userDto") @Valid UserDto dto,
                           BindingResult br,
                           RedirectAttributes ra) {

        UUID tenantId = currentTenant.requireTenantId();

        if (br.hasErrors()) {
            return "register";  // 回到注册页，显示错误
        }

        logger.info("[Register(Admin)] username={} tenant={}", dto.getUsername(), tenantId);

        // ★★ 关键：走新的安全注册方法 ★★
        userService.registerViaAdminOrInvite(dto, tenantId);

        ra.addFlashAttribute("ok", "注册成功");
        return "redirect:/tenants/" + tenantId + "/members";
    }



    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "redirect", required = false) String redirect,
                            Model model) {
        model.addAttribute("redirect", redirect);
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String tenantName,
                          @RequestParam String username,
                          @RequestParam String password,
                          @RequestParam(required = false) String redirect,
                          HttpServletResponse response,
                          HttpServletRequest request,
                          Model model) {

        String uname = username;

        // —— 1) 基本校验（保留你 API 的规则）
        if (tenantName == null || tenantName.isBlank()
                || username == null || password == null) {
            model.addAttribute("error", "tenantName / username / password 必填");
            model.addAttribute("redirect", redirect);
            return "login";
        }

        // —— 2) 防爆破
        if (loginAttemptService.isBlocked(uname)) {
            model.addAttribute("error", "尝试次数过多，账户已暂时锁定");
            model.addAttribute("redirect", redirect);
            return "login";
        }

        try {
            /*
             * —— 3) 从 tenantName 找 tenantId（你的 API 风格）
             */
            Tenant tenant = tenantRepo.findByName(tenantName.trim())
                    .orElseThrow(() -> new BadCredentialsException("租户不存在"));

            UUID tenantId = tenant.getId();

            // —— 4) 登录前写入 TenantContext（与 API 登录保持一致）
            com.example.jobtracker.tenant.TenantContext.set(tenantId);

            /*
             * —— 5) 调用 Spring Security 做认证
             */
            authManager.authenticate(new UsernamePasswordAuthenticationToken(uname, password));

            // —— 6) 登录成功：清除失败次数
            loginAttemptService.loginSucceeded(uname);

            /*
             * —— 7) 生成 JWT（与 API 保持一致）
             */
            String access = jwtUtil.generateAccessToken(tenantId, uname);
            String refresh = jwtUtil.generateRefreshToken(tenantId, uname);

            boolean secure = request.isSecure(); // 本地开发 = false

            // —— 8) 写 Access/Refresh Cookie（你的旧逻辑）
            ResponseCookie accessCookie = ResponseCookie.from("ACCESS", access)
                    .httpOnly(true).secure(false).sameSite("Lax")
                    .path("/")
                    .maxAge(15 * 60)
                    .build();

            ResponseCookie refreshCookie = ResponseCookie.from("REFRESH", refresh)
                    .httpOnly(true).secure(false).sameSite("Lax")
                    .path("/")
                    .maxAge(7 * 24 * 3600)
                    .build();

            response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
            response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

            // —— 9) 跳转（保留你之前逻辑）
            if (redirect != null && !redirect.isBlank()
                    && redirect.startsWith("/")
                    && !redirect.startsWith("/error")
                    && !redirect.startsWith("/login")) {
                return "redirect:" + redirect.trim();
            }

            return "redirect:/jobs";

        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(uname);
            model.addAttribute("error", "用户名或密码错误");
            model.addAttribute("redirect", redirect);
            return "login";

        } catch (AuthenticationException e) {
            loginAttemptService.loginFailed(uname);
            model.addAttribute("error", "登录失败，请重试");
            model.addAttribute("redirect", redirect);
            return "login";

        } catch (Exception e) {
            model.addAttribute("error", "系统错误，请稍后重试");
            model.addAttribute("redirect", redirect);
            return "login";

        } finally {
            // —— 10) 清理（与 API 登录一致）
            com.example.jobtracker.tenant.TenantContext.clear();
        }
    }
    // 寶?曻嵼摨堦槩 Controller 棦
    private void clearAllAuthCookies(HttpServletResponse response) {
        // 1) ??椷攙丗ACCESS乮楬宎 /乯
        // 杮抧 http乮Secure=false, SameSite=Lax乯
        response.addHeader("Set-Cookie", "ACCESS=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        // ?忋 https乮Secure=true, SameSite=None乯
        response.addHeader("Set-Cookie", "ACCESS=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=None");

        // 2) 嶞怴椷攙丗REFRESH乮楬宎 /api/auth/refresh乯
        // 杮抧 http
        response.addHeader("Set-Cookie", "REFRESH=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax");
        // ?忋 https
        response.addHeader("Set-Cookie", "REFRESH=; Path=/api/auth/refresh; Max-Age=0; HttpOnly; Secure; SameSite=None");

        // 3) 寭梕?巎丗庒慭梡??堦 JWT 柤徧丆?庤惔漿乮楬宎 /乯
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



        // 成员列表页面
        @GetMapping("tenants/{tenantId}/members")
        @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
        public String membersPage(@PathVariable UUID tenantId,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "5") int size,
                                  Model model) {

            Page<MemberDto> memberPage = tenantAdminService.listMembers(tenantId, page, size);

            model.addAttribute("tenantId", tenantId);
            Tenant t = tenantRepo.findById(tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "租户不存在"));
            model.addAttribute("tenantName", t.getName());
            model.addAttribute("members", memberPage.getContent());
            model.addAttribute("totalMembers", memberPage.getTotalElements());
            // ⭐⭐ 新增
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", memberPage.getTotalPages());
            model.addAttribute("pageSize", size);

            model.addAttribute("page", page);
            model.addAttribute("last", memberPage.isLast());
            model.addAttribute("inviteForm", new InviteMemberForm());


            return "tenant-members";
        }
    // 显示邀请成员页面（HTML 表单）
    @GetMapping("tenants/{tenantId}/invite")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
    public String showInvitePage(@PathVariable UUID tenantId, Model model) {

        // 用于表单绑定
        model.addAttribute("inviteForm", new InviteMemberForm());

        // 页面需要 tenantId 用于拼接 action
        model.addAttribute("tenantId", tenantId);

        return "tenant-invite-user";  // 对应 templates/tenant/invite.html
    }

        // 邀请成员（表单）
        @PostMapping("tenants/{tenantId}/invite")
        @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
        public String invite(@PathVariable UUID tenantId,
                             @ModelAttribute InviteMemberForm form
        ,RedirectAttributes ra) {
            CreateInviteReq req = new CreateInviteReq(form.getEmail(), form.getRole(),form.getDaysToExpire());
            tenantAdminService.createInvite(tenantId,req);
            ra.addFlashAttribute("ok", "邀请成功！");
            return "redirect:/tenants/" + tenantId + "/invitesList";
        }



    @GetMapping("/tenants/{tenantId}/members/{userId}/role")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
    public String showChangeRolePage(@PathVariable UUID tenantId,
                                     @PathVariable UUID userId,
                                     Model model) {

        ChangeRoleForm form = new ChangeRoleForm();
        model.addAttribute("form", form);
        model.addAttribute("tenantId", tenantId);
        model.addAttribute("userId", userId);

        return "tenant-change-role";
    }

    @PostMapping("/tenants/{tenantId}/members/{userId}/role")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
    public String changeRole(@PathVariable UUID tenantId,
                             @PathVariable UUID userId,
                             @ModelAttribute("form") ChangeRoleForm form) {

       memberRoleService .changeRole(tenantId, userId, form.getNewRole());

        return "redirect:/tenants/" + tenantId + "/members";
    }





    @GetMapping("/tenants/{tenantId}/rename")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
    public String showRenameTenantPage(@PathVariable UUID tenantId,
                                       Model model) {

        RenameTenantForm form = new RenameTenantForm();

        model.addAttribute("form", form);
        model.addAttribute("tenantId", tenantId);

        return "tenant-rename";
    }

    @PostMapping("/tenants/{tenantId}/rename")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
    public String renameTenant(@PathVariable UUID tenantId,
                               @ModelAttribute("form") RenameTenantForm form) {

   tenantProfileService.rename(tenantId, form.getNewName());

        return "redirect:/system/tenants";
    }

    @GetMapping("/tenants/{tenantId}/invitesList")
    @PreAuthorize("hasRole('TENANT_ADMIN') or hasRole('SYSTEM_ADMIN')")
    public String listInvites(@PathVariable UUID tenantId, Model model) {

        List<InviteInfoResp> invites = tenantAdminService.listInvites(tenantId);

        model.addAttribute("invites", invites);
        model.addAttribute("tenantId", tenantId);

        return "tenant-invite-list";
    }





//    ⑦ 系统管理员查看所有租户页面


    @GetMapping("/system/tenants")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
        public String listTenants(
                @ModelAttribute("searchForm") TenantSearchForm form, // 直接放在这里
                @PageableDefault(size = 10) Pageable pageable,
                Model model
        ) {
            Page<Map<String, Object>> page = tenantService.list(form.getKeyword(), pageable);

            model.addAttribute("page", page);
            model.addAttribute("tenants", page.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", page.getTotalPages());
        model.addAttribute("pageSize", pageable.getPageSize());
        model.addAttribute("keyword", form.getKeyword());
        model.addAttribute("totalTenants",page.getTotalElements()); // 回传表单数据



        return "system-tenant-list";
        }


    /** GET — 显示创建表单 */
    @GetMapping("/system/tenants/create")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public String showCreateForm(Model model) {
        model.addAttribute("form", new TenantCreateForm());
        return "system-tenants-create";
    }

    /** POST — 提交创建 */
    @PostMapping("system/tenant/create")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public String createTenant(
            @ModelAttribute("form") TenantCreateForm form
    ) {
        tenantService.createTenant(form.getName());
        return "redirect:/system/tenants";  // 创建成功后跳回租户列表
    }


}





