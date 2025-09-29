package com.example.jobtracker.ああ７a５;

import com.example.jobtracker.domain.Tenant;
import com.example.jobtracker.repository.TenantRepository;
import com.example.jobtracker.tenant.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class TenantGuard {
    private final TenantRepository tenantRepository;

    public TenantGuard(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /** ★★★ 统一从 ThreadLocal 取，取不到就报错 */
    public static UUID currentTenant() {
        return TenantContext.requireTenantIdFromRequest(); // ← 请确保 TenantContext 有 require() 走 ThreadLocal
    }

    /** 仅做“同租户”校验（读/写通用的场景可用） */
    public static void requireSameTenant(UUID targetTenantId) {
        UUID cur = TenantContext.get(); // ThreadLocal
        if (cur == null) {
            throw new IllegalStateException("Tenant not resolved");
        }
        if (!cur.equals(targetTenantId) && !SecurityUtils.isSystemAdmin()) {
            throw new AccessDeniedException("跨租户访问被拒绝");
        }
    }

    /**
     * 写操作前必须调用：同租户 + ACTIVE。
     * 是否豁免 SYS_ADMIN：看你的策略。这里给两套写法：
     *   - 更安全：不豁免（如下）
     *   - 需要豁免：见注释代码
     */
    public void requireWritableTenant(UUID targetTenantId) {
        UUID cur = currentTenant(); // ThreadLocal
        if (!cur.equals(targetTenantId)) {
            // 允许 SYS_ADMIN 跨租户写的话，改为：
            // if (!cur.equals(targetTenantId) && !SecurityUtils.isSystemAdmin()) { ... }
            throw new AccessDeniedException("跨租户写入被拒绝");
        }

        Tenant t = tenantRepository.findById(targetTenantId)
                .orElseThrow(() -> new NoSuchElementException("Tenant not found"));

        if (t.getStatus() == TenantStatus.SUSPENDED) {
            // 若要让 SYS_ADMIN 仍可写，改为：
            // if (t.getStatus() == TenantStatus.SUSPENDED && !SecurityUtils.isSystemAdmin()) { ... }
            throw new AccessDeniedException("租户已被暂停，禁止写操作");
        }
    }
}

