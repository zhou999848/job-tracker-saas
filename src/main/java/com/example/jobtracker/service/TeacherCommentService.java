package com.example.jobtracker.service;

import com.example.jobtracker.TenantInvite7a4.Role;
import com.example.jobtracker.domain.TeacherComment;
import com.example.jobtracker.domain.User;
import com.example.jobtracker.repository.TeacherCommentRepository;
import com.example.jobtracker.repository.UserRepository;

import com.example.jobtracker.ああ７a５.SecurityUtils;
import com.example.jobtracker.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TeacherCommentService {

    private final TeacherCommentRepository commentRepo;
    private final UserRepository userRepo;
    private final SecurityUtils securityUtils;

    public TeacherCommentService(TeacherCommentRepository commentRepo,
                                 UserRepository userRepo,
                                 SecurityUtils securityUtils) {
        this.commentRepo = commentRepo;
        this.userRepo = userRepo;
        this.securityUtils = securityUtils;
    }

    /* ========= 教员侧 ========= */

    @Transactional(readOnly = true)
    public List<TeacherComment> listCommentsForStudent(UUID studentId) {
        requireTenantAdmin();
        UUID tenantId = TenantContext.requireTenantIdFromRequest();
        ensureStudentInSameTenant(tenantId, studentId);
        return commentRepo.findByTenantIdAndStudentIdOrderByCreatedAtDesc(tenantId, studentId);
    }

    @Transactional
    public void addCommentForStudent(UUID studentId, String content) {
        requireTenantAdmin();
        UUID tenantId = TenantContext.requireTenantIdFromRequest();
        ensureStudentInSameTenant(tenantId, studentId);

        String c = content == null ? "" : content.trim();
        if (c.isEmpty()) throw new IllegalArgumentException("content is blank");
        if (c.length() > 2000) throw new IllegalArgumentException("content too long");

        UUID teacherId = securityUtils.currentUserId();
        commentRepo.save(new TeacherComment(tenantId, studentId, teacherId, c));
    }

    /* ========= 学生侧 ========= */

    @Transactional(readOnly = true)
    public List<TeacherComment> listMyComments() {
        requireUserOrTenantAdminOrSystemAdmin(); // 你也可以改成只允许 USER
        UUID tenantId = TenantContext.requireTenantIdFromRequest();
        UUID me = securityUtils.currentUserId();
        return commentRepo.findByTenantIdAndStudentIdOrderByCreatedAtDesc(tenantId, me);
    }

    /* ========= 内部校验 ========= */

    private void requireTenantAdmin() {
        Authentication a = securityUtils.getAuthentication();
        if (a == null || !a.isAuthenticated()) throw new SecurityException("forbidden");

        boolean ok = a.getAuthorities().stream().anyMatch(ga ->
                "ROLE_TENANT_ADMIN".equals(ga.getAuthority()) ||
                        "ROLE_SYSTEM_ADMIN".equals(ga.getAuthority()) // 你想系统管理员也能看可保留；不想就删掉
        );

        if (!ok) throw new SecurityException("forbidden");
    }

    private void requireUserOrTenantAdminOrSystemAdmin() {
        Authentication a = securityUtils.getAuthentication();
        if (a == null || !a.isAuthenticated()) throw new SecurityException("forbidden");

        boolean ok = a.getAuthorities().stream().anyMatch(ga ->
                "ROLE_USER".equals(ga.getAuthority()) ||
                        "ROLE_TENANT_ADMIN".equals(ga.getAuthority()) ||
                        "ROLE_SYSTEM_ADMIN".equals(ga.getAuthority())
        );
        if (!ok) throw new SecurityException("forbidden");
    }

    private void ensureStudentInSameTenant(UUID tenantId, UUID studentId) {
        User student = userRepo.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("student not found"));

        // ✅ 关键：同租户
        if (student.getTenant() == null || student.getTenant().getId() == null
                || !tenantId.equals(student.getTenant().getId())) {
            throw new SecurityException("forbidden");
        }

        // ✅ 最小版：限定只能对 USER 留言（避免对 admin/system 乱写）
        Role r = student.getRole();
        if (r == null || r != Role.USER) {
            throw new IllegalArgumentException("target is not a student(USER)");
        }
    }
}
