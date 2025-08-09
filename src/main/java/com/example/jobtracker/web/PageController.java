package com.example.jobtracker.web;

import com.example.jobtracker.domain.User;
import com.example.jobtracker.dto.JobApplicationDto;
import com.example.jobtracker.dto.NoteDto;
import com.example.jobtracker.repository.JobApplicationRepository;
import com.example.jobtracker.repository.NoteRepository;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.domain.Note;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

    public PageController(AuthenticationManager authManager,JwtUtil jwtUtil,JobApplicationRepository jobRepo, NoteRepository noteRepo,UserRepository userRepo) {
        this.jobRepo = jobRepo;
        this.noteRepo = noteRepo;
        this.userRepo = userRepo;
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
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
        Page<JobApplicationDto>jobs=list(page, size, sortBy, direction);
        model.addAttribute("jobs", jobs.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", jobs.getTotalPages());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("direction", direction);
        model.addAttribute("pageSize", size);
        return "jobs";  // 指向 templates/jobs.html
    }




    public void addJob(@RequestBody @Valid JobApplicationDto jobDto) {
        logger.info("【EN】Creating job: company={}, position={} / 【中文】创建职位：公司={}，职位={} / 【日本語】職務作成：会社={}、職種={}", jobDto.getCompany(), jobDto.getPosition(), jobDto.getCompany(), jobDto.getPosition(), jobDto.getCompany(), jobDto.getPosition());

           // ① 获取当前登录的用户名
           String username = SecurityContextHolder.getContext().getAuthentication().getName();

           // ② 查出 User 实体
           User user =userRepo.findByUsername(username).orElseThrow();

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
    public Page<NoteDto>showPagedNotes(@RequestParam UUID jobId,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "3" )int size) {
        String username = getCurrentUsername();
        logger.info("【EN】User={} Fetching paged notes / 【中文】用户={} 分页获取笔记 / 【日本語】ユーザー={} がページ取得: jobId={}, page={}", username, username, username, jobId, page);

        PageRequest request = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return noteRepo.findByUserUsernameAndJobId(username,jobId,request)
                .map(note -> {
                    NoteDto dto = new NoteDto();
                    dto.setJobId(note.getJobId());
                    dto.setContent(note.getContent());
                    dto.setCreatedAt(note.getCreatedAt());
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
     *  @param model    用于传递错误信息 Model to pass error messages
     * @return login.html 页面视图 View name for login.html
     */

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("error", "用户名或密码错误"); // 这里添加错误信息

        }
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          HttpServletResponse response,
                          Model model) {
        try {
            // 1) 校验用户名/密码
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            // 2) 生成 JWT
            String token = jwtUtil.generateToken(username);
            // 3) 写入 HttpOnly Cookie（名字叫 JWT）
            ResponseCookie cookie = ResponseCookie.from("JWT", token)
                    .httpOnly(true)
                    .secure(false)        // 本地开发可 false，生产建议 true（https）
                    .path("/")
                    .sameSite("Lax")
                    .maxAge(24 * 60 * 60)
                    .build();
            response.addHeader("Set-Cookie", cookie.toString());

            // 4) 成功后跳到职位页
            return "redirect:/jobs";
        } catch (Exception e) {
            model.addAttribute("error", "用户名或密码错误");
            return "login";
        }
    }

    @PostMapping("/logout")
    public String doLogout(HttpServletResponse response) {
        // 覆盖同名 Cookie 使其过期
        ResponseCookie clear = ResponseCookie.from("JWT", "")
                .httpOnly(true).secure(false).path("/")
                .sameSite("Lax").maxAge(0).build();
        response.addHeader("Set-Cookie", clear.toString());
        return "redirect:/login";
    }
}










