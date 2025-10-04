package com.example.jobtracker.tenant;
import java.util.UUID;

public class TenantContext {
    private static final ThreadLocal<UUID> ID_HOLDER = new ThreadLocal<>();

    public static void set(UUID tenantId) { ID_HOLDER.set(tenantId); }
    public static UUID getId() { return ID_HOLDER.get(); }
    public static void clear() { ID_HOLDER.remove(); }

    public static UUID requireTenantIdFromRequest() {
        UUID id = ID_HOLDER.get();
        if (id == null) throw new IllegalStateException("Tenant ID not resolved");
        return id;
    }

    /** ↓↓↓ 如果以后确实需要 name，可单独提供查库工具方法按需取；不再放 ThreadLocal。 */
}
