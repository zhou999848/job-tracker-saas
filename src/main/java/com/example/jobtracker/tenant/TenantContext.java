package com.example.jobtracker.tenant;

import java.util.UUID;

public class TenantContext {
    private static final ThreadLocal<UUID> HOLDER = new ThreadLocal<>();

    public static void set(UUID tenantId) { HOLDER.set(tenantId); }
    public static UUID get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }
    public static final String REQUEST_ATTR = "tenantId";
    // 你代码里调用的就是这个
    public static UUID requireTenantIdFromRequest() {
        UUID id = HOLDER.get();
        if (id == null) throw new IllegalStateException("Tenant not resolved");
        return id;
    }
}

