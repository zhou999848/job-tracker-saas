package com.example.jobtracker.ああ７a５;



import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.security.LoginUser;
import com.example.jobtracker.tenant.TenantContext;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

public class SecurityUtils {

    // 获取当前登录用户的 Authentication
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    // 获取当前用户名（JWT 登录时就是 username）
    public static String getCurrentUsername() {
        Authentication auth = getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return principal.toString();
    }


    private static UserRepository userRepository; // 静态注入一次即可

    public SecurityUtils(UserRepository repo) {
        SecurityUtils.userRepository = repo;
    }
    public static UUID currentUserId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("未登录");
        }

        Object principal = a.getPrincipal();

        // 1) 你的自定义 principal（例如包含 userId 的 LoginUser 或 UserDetailsImpl）
        if (principal instanceof LoginUser lu) {
            return lu.getUserId();
        }
        // 如果你项目里是 UserDetailsImpl，放开下面这个分支
        if (principal instanceof UserDetailsImpl udi) {
            return udi.getId();
        }

        // 2) 退化：只有 username（UserDetails 或 String），就 (tenantId, username) 查一次库拿 id
        String username = null;
        if (principal instanceof UserDetails ud) {
            username = ud.getUsername();
        } else if (principal instanceof String s) {
            username = s;
        }

        if (username != null) {
            UUID tenantId = TenantContext.requireTenantIdFromRequest();
            return userRepository.findByTenantIdAndUsername(tenantId, username)
                    .map(user -> user.getId())
                    .orElseThrow(() -> new IllegalStateException("未登录：无法解析用户ID"));
        }

        // 3) 其它意外类型
        throw new IllegalStateException("未登录：不支持的 principal 类型=" + principal.getClass().getName());
    }

    public static boolean isSystemAdmin() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getAuthorities().stream()
                .anyMatch(ga -> "ROLE_SYSTEM_ADMIN".equals(ga.getAuthority()));
    }
}
