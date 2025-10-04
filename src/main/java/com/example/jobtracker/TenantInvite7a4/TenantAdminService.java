package com.example.jobtracker.TenantInvite7a4;



import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.domain.User;
import com.example.jobtracker.repository.TenantRepository;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.security.LoginUser;
import com.example.jobtracker.service.CurrentTenant;
import com.example.jobtracker.ああ７a５.TenantGuard;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.Base64.Encoder;

/**
 * 完成版 TenantAdminService
 * 关键特性：
 * - 邀请创建/预览/接受（一次性 token，过期控制）
 * - 成员列表与删除（最后管理员保护）
 * - 方法级权限 + 服务层租户一致性校验
 * - 强随机 token（Base64URL），线程安全 Clock 兜底
 */
@Service
public class TenantAdminService {

    private static final Logger log = LoggerFactory.getLogger(TenantAdminService.class);

    private final TenantInviteRepo invites;
    private final TenantRepository tenants;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final Clock clock;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Encoder b64url = Base64.getUrlEncoder().withoutPadding();
    private final CurrentTenant currentTenant;


    public TenantAdminService(TenantInviteRepo invites,
                              TenantRepository tenants,
                              UserRepository users,
                              PasswordEncoder encoder,
                              Optional<Clock> clock,CurrentTenant currentTenant) {
        this.currentTenant = currentTenant;
        this.invites = Objects.requireNonNull(invites);
        this.tenants = Objects.requireNonNull(tenants);
        this.users = Objects.requireNonNull(users);
        this.encoder = Objects.requireNonNull(encoder);
        // 若项目未声明 Clock Bean，这里用系统 UTC 兜底，避免 NPE
        this.clock = clock.orElse(Clock.systemUTC());
    }

    @Transactional
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
    public InviteInfoResp createInvite(UUID tenantId, CreateInviteReq req) {
        // 0) 参数校验…
        // 1) ensureSameTenantOrSysAdmin(tenantId);
        // 2) 幂等检查…
        Instant now = Instant.now(clock);

        Tenant tenant = tenants.findById(tenantId)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found"));

        TenantInvite invite = new TenantInvite();
        invite.setId(UUID.randomUUID());
        invite.setTenant(tenant);
        invite.setEmail(normalizeEmail(req.email()));
        Role role = (req.role() == null) ? Role.USER : req.role();
        invite.setRole(role);
        invite.setToken(generateStrongToken(48));
        invite.setExpiresAt(now.plus(Duration.ofDays(
                (req.daysToExpire() == null || req.daysToExpire() <= 0) ? 7 : req.daysToExpire()
        )));
        invite.setCreatedAt(now);

        // ✅ 关键改动：一律从认证主体拿 userId，保证非空，避免跨租户取不到的问题
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) throw new IllegalStateException("No authentication");
        Object details = auth.getDetails();
        if (!(details instanceof LoginUser lu) || lu.userId() == null) {
            throw new IllegalStateException("Missing userId in authentication details");
        }
        // 如果实体是 UUID 列：
        invite.setCreatedBy(lu.userId());
        // 如果实体是字符串列：
        // invite.setCreatedBy(lu.userId().toString());

        invites.save(invite);

        log.info("[InviteCreated] tenant={}, email={}, role={}, createdBy={}, token={}",
                tenant.getName(), invite.getEmail(), role, lu.userId(), invite.getToken());

   //final String frontBaseUrl = "https://localhost:8080";
        String inviteToken =   invite.getToken();

        return new InviteInfoResp(invite.getEmail(), tenant.getName(), invite.getRole(),
                invite.getExpiresAt(), false,inviteToken);
    }



    // ======================
    // 招待预览
    // ======================

    @Transactional(readOnly = true)
    public InviteInfoResp previewInvite(String token) {
        TenantInvite i = invites.findByToken(requireToken(token))
                .orElseThrow(() -> new NoSuchElementException("Invalid token"));
        return new InviteInfoResp(i.getEmail(), i.getTenant().getName(), i.getRole(), i.getExpiresAt(), i.isUsed(),
                 i.getToken());
      //  "https://localhost:8080/signup?token=" +
    }

    // ======================
    // 接受邀请 → 创建用户并绑定租户
    // ======================

    @Transactional
    public void acceptInvite(String token, AcceptInviteReq req) {
        // 0) 参数与 token 校验
        String t = requireToken(token);
        TenantInvite invite = invites.findByToken(t)
                .orElseThrow(() -> new NoSuchElementException("Invalid token"));

        Instant now = Instant.now(clock);
        if (invite.getUsedAt() != null) {
            throw new IllegalStateException("邀请已被使用");
        }
        if (now.isAfter(invite.getExpiresAt())) {
            throw new IllegalStateException("邀请已过期");
        }

        // 1) 表单校验
        String username = trimToNull(req.username());
        String displayName = trimToNull(req.displayName());
        String rawPassword = trimToNull(req.password());
        if (username == null || rawPassword == null) {
            throw new IllegalArgumentException("用户名与密码为必填项");
        }

        // 2) 邮箱冲突校验：邮箱若存在且属于其他租户 → 拒绝；属于当前租户 → 也拒绝（受邀注册只允许新建）
        users.findByEmail(invite.getEmail()).ifPresent(u -> {
            UUID inviteTid = invite.getTenant().getId();
            UUID userTid = u.getTenant().getId();
            if (!inviteTid.equals(userTid)) {
                throw new IllegalStateException("该邮箱已在其他租户注册");
            }
            throw new IllegalStateException("该邮箱在本租户已存在");
        });

        // 3) 用户名冲突
        users.findByUsername(username).ifPresent(u -> {
            throw new IllegalStateException("该用户名已被占用");
        });

        // 4) 创建用户 —— 关键点：不要手动 setId，交给 JPA 走 persist 路径（避免 merge 引发的乐观锁/脱管冲突）
        User user = new User();
        // user.setId(null); // 确保为 null，依赖 @GeneratedValue(strategy = …) 生成
        user.setUsername(username);
        user.setDisplayName(displayName != null ? displayName : username);
        user.setEmail(invite.getEmail());
        user.setPassword(encoder.encode(rawPassword));
        user.setTenant(invite.getTenant());
        user.setRole(invite.getRole());
        // 如需审计：user.setCreatedAt(now);

        users.save(user);          // persist（非 merge）
        // users.flush();           // 可选：需要立即捕获唯一约束等异常时打开

        // 5) 标记邀请已使用 —— invite 为托管实体，更新后保存（或依赖脏检查亦可）
        invite.setUsedAt(now);
        invites.save(invite);      // 明确保存，便于日志与一致性

        log.info("[InviteAccepted] tenant={}, email={}, username={}",
                invite.getTenant().getName(), invite.getEmail(), username);
    }

    // ======================
    // 成员列表
    // ======================

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
    public Page<MemberDto> listMembers(UUID tenantId, int page, int size) {
        Objects.requireNonNull(tenantId, "tenantId is required");
        ensureSameTenantOrSysAdmin(tenantId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> pageData = users.findAllByTenantId(tenantId, pageable);
        return pageData.map(u -> new MemberDto(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getRole(),
                u.getCreatedAt()));
    }

    // ======================
// 删除成员（最后管理员保护）
// ======================
    @PersistenceContext
    private EntityManager em;

    @Transactional
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SYSTEM_ADMIN')")
    public void removeMember(UUID tenantId, UUID userId) {
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(userId, "userId is required");

        // 1) 系统管理员放行；普通管理员必须同租户
        ensureSameTenantOrSysAdmin(tenantId);

        // 2) 目标用户（再做一次租户断言）
        User target = users.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found"));
        assertSameTenant(target.getTenant().getId(), tenantId);

        // 3) 解析当前操作者（不要再从 principal 强转 LoginUser）
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        LoginUser actor = (auth != null && auth.getDetails() instanceof LoginUser lu) ? lu : null;

        // 用户名用通用方法拿（兼容 UserDetails/JWT/Principal）
        String operatorUsername = currentTenant.currentUsername();

        // 优先用 details 里的 userId；若为空（如 SYSTEM_ADMIN 跨租户），回退用 (tenantId, username) 查
        UUID operatorId = (actor != null) ? actor.userId() : null;
        if (operatorId == null) {
            operatorId = users.findByTenantIdAndUsername(tenantId, operatorUsername)
                    .map(User::getId)
                    .orElse(null); // 跨租户时可能为空，后面判空处理
        }

        // 4) 禁止自删（系统管理员除外）。跨租户时 operatorId 可能为 null，跳过该限制。
        if (operatorId != null && operatorId.equals(target.getId()) && !isSystemAdmin()) {
            throw new IllegalStateException("不允许删除自己");
        }

        // 5) 禁止删除最后一个租户管理员
        if (target.getRole() == Role.TENANT_ADMIN && countAdmins(tenantId) <= 1) {
            throw new IllegalStateException("禁止删除最后一个租户管理员");
        }

        // 6) 清理子表外键（字段名按你的实体调整）
        em.createQuery("""
        update JobApplication ja
           set ja.user = null
         where ja.user.id = :uid
    """).setParameter("uid", target.getId()).executeUpdate();

        // 7) 物理删除用户；不要再对 target 做任何 save/merge
        users.delete(target);
        users.flush(); // 可留，便于尽早暴露约束问题

        log.info("[MemberRemoved] tenantId={}, userId={}, operator={}", tenantId, userId, operatorUsername);
    }




    // ======================
    // —— 私有辅助方法 ——
    // ======================
// 示例：TenantAdminService.java
    private void ensureSameTenantOrSysAdmin(UUID pathTenantId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSysAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SYSTEM_ADMIN".equals(a.getAuthority()));

        if (isSysAdmin) {
            // 系统管理员允许跨租户操作，无需依赖 TenantContext
            return;
        }

        // 非系统管理员：必须有当前租户，并且与 pathTenantId 一致
        UUID currentTenantId = currentTenant.requireTenantId();

        if (!currentTenantId.equals(pathTenantId)) {
            throw new AccessDeniedException("Cross-tenant access denied");
        }
    }



    private void assertSameTenant(UUID a, UUID b) {
        if (!Objects.equals(a, b)) {
            throw new AccessDeniedException("跨租户成员");
        }
    }

    private long countAdmins(UUID tenantId) {
        return users.countByTenantIdAndRole(tenantId, Role.TENANT_ADMIN);
    }

    private String requireToken(String token) {
        String t = trimToNull(token);
        if (t == null) throw new IllegalArgumentException("token 为空");
        return t;
    }

    private String normalizeEmail(String email) {
        String v = trimToNull(email);
        return v == null ? null : v.toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    private String generateStrongToken(int numBytes) {
        byte[] buf = new byte[numBytes <= 0 ? 48 : numBytes];
        secureRandom.nextBytes(buf);
        return b64url.encodeToString(buf); // URL-safe，无需再替换
    }

    /**
     * 当前登录用户实体（用于 createdBy 等场景）
     */
    private User requireCurrentUserEntity() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser p)) {
            throw new IllegalStateException("未登录");
        }

        // 首选按 userId 精确命中
        if (p.userId() != null) {
            return users.findById(p.userId())
                    .orElseThrow(() -> new IllegalStateException("当前用户不存在：" + p));
        }

        // 兜底：严格使用 tenantId + username
        return users.findByTenantIdAndUsername(p.tenantId(), p.username())
                .orElseThrow(() -> new IllegalStateException("当前用户不存在：" + p));
    }
    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new IllegalStateException("未登录或无身份信息");
        }
        return auth.getName();
    }

    private boolean isSystemAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String a = ga.getAuthority();
            if ("ROLE_SYSTEM_ADMIN".equals(a) || "SYSTEM_ADMIN".equals(a)) {
                return true;
            }
        }
        return false;
    }



}

