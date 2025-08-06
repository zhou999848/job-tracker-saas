package com.example.jobtracker.web;

import com.example.jobtracker.domain.User;
import com.example.jobtracker.dto.JobApplicationDto;
import com.example.jobtracker.repository.JobApplicationRepository;
import com.example.jobtracker.repository.NoteRepository;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.domain.Note;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
public class PageController {
    private static final Logger logger = LoggerFactory.getLogger(PageController.class);
    private final JobApplicationRepository jobRepo;
    private final NoteRepository noteRepo;
    private final UserRepository userRepo;

    public PageController(JobApplicationRepository jobRepo, NoteRepository noteRepo,UserRepository userRepo) {
        this.jobRepo = jobRepo;
        this.noteRepo = noteRepo;
        this.userRepo = userRepo;
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




    public void addJob(@RequestBody @Valid JobApplicationDto dto) {
        logger.info("【EN】Creating job: company={}, position={} / 【中文】创建职位：公司={}，职位={} / 【日本語】職務作成：会社={}、職種={}", dto.getCompany(), dto.getPosition(), dto.getCompany(), dto.getPosition(), dto.getCompany(), dto.getPosition());

           // ① 获取当前登录的用户名
           String username = SecurityContextHolder.getContext().getAuthentication().getName();

           // ② 查出 User 实体
           User user =userRepo.findByUsername(username).orElseThrow();

           // ③ 创建 Job 实体并填充数据
           JobApplication job = new JobApplication();
           job.setCompany(dto.getCompany());
           job.setPosition(dto.getPosition());
           job.setStatus(dto.getStatus());
           job.setAppliedDate(dto.getAppliedDate());

           // ④ 设置所属用户
           job.setUser(user);

           // ⑤ 保存
           jobRepo.save(job);
       }
       @GetMapping("/jobs/add")
    public String showAddForm(Model model) {
        model.addAttribute("job", new JobApplicationDto()); // 用于表单绑定
        return "add-job";
    }

    @PostMapping("/jobs/add")
    public String saveJob(@ModelAttribute JobApplicationDto jobDto) {
        String username = getCurrentUsername();
        logger.info("[Add Job] User={} 添加职位：公司={}，职位={} / Adding job: company={}, position={}", username, jobDto.getCompany(), jobDto.getPosition(), jobDto.getCompany(), jobDto.getPosition());
        addJob(jobDto); // 调用上面的 addJob 方法保存职位

        return "redirect:/jobs"; // 添加成功后跳转回职位列表
    }




    /**
     * ✅ 显示某个职位的笔记页面 / Display notes for a job application
     * [GET] /notes/{jobId}
     */
    @GetMapping("/notes/{jobId}")
    public String showNotes(@PathVariable UUID jobId, Model model, Pageable request) {
        String username = getCurrentUsername();
        logger.info("[Show Notes] User={} 查看 jobId={} 的笔记 / Viewing notes for jobId={}", username, jobId, jobId);

        List<Note> notes = (List<Note>) noteRepo.findByJobId(jobId, request);
        model.addAttribute("notes", notes);
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


    /**
     * 处理登录请求（POST /login）
     * Process login form submission (POST /login)
     *
     * @param username 用户输入的用户名 Username from form
     * @param password 用户输入的密码 Password from for
     * @return 重定向到/jobs 或返回登录页面 Redirect to /jobs or return login page
     */
    @PostMapping("/doLogin")
    public String processLogin(@RequestParam String username,
                               @RequestParam String password) {
        username = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("收到登录请求 (POST /login)，用户名: {}", username); // Log username for tracking

        // 简单判断用户名和密码（实际项目中应使用数据库或JWT认证）
        // Simple credential check (replace with DB or JWT in real projects)
        if ("user1".equals(username) && "123456".equals(password)) {
            username = SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("✅ 登录成功 - 用户名: {}", username); // Log success
            return "redirect:/home"; // 登录成功跳转 Redirect on success
        } else {
            return "/ddddddddddLogin";//原本这里的错误信息都已经转到("login")，现在这里只要有1个return语句就行，内容的随便写

        }
    }
}







