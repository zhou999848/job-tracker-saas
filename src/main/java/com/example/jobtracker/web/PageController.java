package com.example.jobtracker.web;

import com.example.jobtracker.repository.JobApplicationRepository;
import com.example.jobtracker.repository.NoteRepository;
import com.example.jobtracker.domain.JobApplication;
import com.example.jobtracker.domain.Note;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Controller
public class PageController {
    private static final Logger logger = LoggerFactory.getLogger(PageController.class);
    private final JobApplicationRepository jobRepo;
    private final NoteRepository noteRepo;

    public PageController(JobApplicationRepository jobRepo, NoteRepository noteRepo) {
        this.jobRepo = jobRepo;
        this.noteRepo = noteRepo;
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    /**
     * ✅ 显示所有职位页面 / Display all job applications page
     * [GET] /jobs
     */
    @GetMapping("/jobs")
    public String showJobs(Model model) {
        String username = getCurrentUsername();
        logger.info("[Show Jobs] User={} 查看职位列表 / Viewing job list", username);

        List<JobApplication> jobs = jobRepo.findAll();
        model.addAttribute("jobs", jobs);
        return "jobs";  // 指向 templates/jobs.html
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
         *
         * @return login.html 页面视图 View name for login.html
         */
        @GetMapping("/login")

        public String showLoginForm() {
            logger.info("访问登录页面 (GET /login)"); // Log page access
            return "login"; // 返回登录页面 return login.html
        }

        /**
         * 处理登录请求（POST /login）
         * Process login form submission (POST /login)
         *
         * @param username 用户输入的用户名 Username from form
         * @param password 用户输入的密码 Password from form
         * @param model    用于传递错误信息 Model to pass error messages
         * @return 重定向到/jobs 或返回登录页面 Redirect to /jobs or return login page
         */
        @PostMapping("/login")
        public String processLogin(@RequestParam String username,
                @RequestParam String password,
                Model model) {
           username=SecurityContextHolder.getContext().getAuthentication().getName();
            logger.info("收到登录请求 (POST /login)，用户名: {}", username); // Log username for tracking

            // 简单判断用户名和密码（实际项目中应使用数据库或JWT认证）
            // Simple credential check (replace with DB or JWT in real projects)
            if ("user1".equals(username) && "123456".equals(password)) {
                username = SecurityContextHolder.getContext().getAuthentication().getName();
                logger.info("✅ 登录成功 - 用户名: {}", username); // Log success
                return "redirect:/jobs"; // 登录成功跳转 Redirect on success
            }

            // 登录失败，添加错误信息并记录日志
            // On login failure, add error message and log

                model.addAttribute("error", "用户名或密码错误"); // 这里添加错误信息

            username=SecurityContextHolder.getContext().getAuthentication().getName();
            logger.warn("❌ 登录失败 - 用户名: {}", username); // Log failure
            return "login"; // 返回登录页面 Return to login
        }


}