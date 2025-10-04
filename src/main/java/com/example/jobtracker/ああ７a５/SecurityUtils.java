
package com.example.jobtracker.ああ７a５;

import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.security.LoginUser;
import com.example.jobtracker.tenant.TenantContext;
import com.example.jobtracker.domain.User;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** 当前认证对象 */
    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /** 当前用户名（若有） */
    public String getCurrentUsername() {
        Authentication auth = getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        Object p = auth.getPrincipal();
        if (p instanceof UserDetails ud) return ud.getUsername();
        return p.toString();
    }

    /** 解析当前登录用户ID；优先从 LoginUser，退化到 (tenantId, username) 查库 */
    public UUID currentUserId() {
        Authentication a = getAuthentication();
        if (a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("未登录");
        }

        Object principal = a.getPrincipal();

        // 1) 你已有的自定义 principal（包含 userId）
        if (principal instanceof LoginUser lu && lu.getUserId() != null) {
            return lu.getUserId();
        }

        // 2) 退化：只有 username，则 (tenantId, username) 查一次库拿 id
        String username = null;
        if (principal instanceof UserDetails ud) {
            username = ud.getUsername();
        } else if (principal instanceof String s) {
            username = s;
        }

        if (username != null) {
            UUID tenantId = TenantContext.requireTenantIdFromRequest();
            return userRepository.findByTenantIdAndUsername(tenantId, username)
                    .map(User::getId)
                    .orElseThrow(() -> new IllegalStateException("未登录：无法解析用户ID"));
        }

        // 3) 其它意外类型
        throw new IllegalStateException("未登录：不支持的 principal 类型=" + principal.getClass().getName());
    }

    /** 是否系统管理员 */
    public boolean isSystemAdmin() {
        Authentication a = getAuthentication();
        return a != null && a.getAuthorities().stream()
                .anyMatch(ga -> "ROLE_SYSTEM_ADMIN".equals(ga.getAuthority()));
    }
}
