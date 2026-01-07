package com.example.jobtracker.web;

import com.example.jobtracker.domain.TeacherComment;
import com.example.jobtracker.service.TeacherCommentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
public class TeacherCommentController {

    private final TeacherCommentService service;

    public TeacherCommentController(TeacherCommentService service) {
        this.service = service;
    }

    /* 教员：查看 + 新增某学生留言 */
    @GetMapping("/students/{studentId}/comments")
    public String teacherComments(@PathVariable UUID studentId, Model model) {
        List<TeacherComment> comments = service.listCommentsForStudent(studentId);
        model.addAttribute("studentId", studentId);
        model.addAttribute("comments", comments);
        model.addAttribute("form", new CommentForm());
        return "teacher-comments";
    }

    @PostMapping("/students/{studentId}/comments")
    public String teacherPost(@PathVariable UUID studentId,
                              @ModelAttribute("form") CommentForm form) {
        service.addCommentForStudent(studentId, form.getContent());
        return "redirect:/students/%s/comments".formatted(studentId);
    }

    /* 学生：看自己收到的留言 */
    @GetMapping("/my/comments")
    public String myComments(Model model) {
        model.addAttribute("comments", service.listMyComments());
        return "my-comments";
    }

    public static class CommentForm {
        private String content;
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}

