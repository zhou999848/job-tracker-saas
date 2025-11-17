// src/test/java/com/example/jobtracker/support/TestTenantFilter.java
package com.example.jobtracker.support;

import com.example.jobtracker.tenant.TenantContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 仅用于「测试环境」的过滤器：
 * - 从请求头 X-Debug-Tenant 读取 tenantId（UUID）
 * - 主通道：写入 TenantContext（单一真相 / ThreadLocal）
 * - 兜底：写入 MDC["tenantId"] 和 Authentication#details["tenantId"]
 * - 在 finally 中清理，避免测试间串租户
 *
 * 使用方式（测试用例中）：
 * mvc.perform(get("/api/xxx").header("X-Debug-Tenant", tenantId.toString()))
 *    .andExpect(status().isOk());
 */
public class TestTenantFilter implements Filter {

    public static final String HEADER_TENANT = "X-Debug-Tenant";
    public static final String MDC_KEY = "tenantId";
    public static final String DETAILS_KEY = "tenantId";

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest http = (HttpServletRequest) req;

        String raw = http.getHeader(HEADER_TENANT);
        UUID tenantId = null;
        boolean contextSet = false;
        boolean mdcSet = false;

        try {
            if (raw != null && !raw.isBlank()) {
                try {
                    tenantId = UUID.fromString(raw.trim());
                    // ① 主通道：TenantContext（你的 TenantContext 若方法名为 setId/clear，请替换这里）
                    TenantContext.set(tenantId);
                    contextSet = true;

                    // ② 兜底：MDC，方便日志与 CurrentTenant.optionalTenantId() 兼容
                    MDC.put(MDC_KEY, tenantId.toString());
                    mdcSet = true;

                    // ③ 兜底：Authentication#details
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null) {
                        Object details = auth.getDetails();
                        if (details instanceof Map<?, ?> m) {
                            // 直接写入已有 Map
                            @SuppressWarnings("unchecked")
                            Map<Object, Object> map = (Map<Object, Object>) m;
                            map.put(DETAILS_KEY, tenantId.toString());
                        } else {
                            // 构造一个可变 Map；尽量通过 setDetails 设置回去
                            Map<String, Object> map = new HashMap<>();
                            map.put(DETAILS_KEY, tenantId.toString());
                            if (details != null) {
                                map.put("_orig", details); // 可选：保留原 details
                            }
                            if (auth instanceof AbstractAuthenticationToken token) {
                                token.setDetails(map);
                            } else {
                                // 有些实现不是 AbstractAuthenticationToken，但暴露 setDetails(Object)
                                try {
                                    auth.getClass().getMethod("setDetails", Object.class).invoke(auth, map);
                                } catch (Exception ignored) {
                                    // 某些实现不允许/不暴露 setDetails：忽略即可
                                }
                            }
                        }
                    }
                } catch (IllegalArgumentException ignored) {
                    // 无效 UUID，忽略，不注入任何租户信息
                }
            }

            chain.doFilter(req, res);
        } finally {
            // 清理，避免测试用例之间串上下文
            if (contextSet) {
                try { TenantContext.clear(); } catch (Exception ignored) {}
            }
            if (mdcSet) {
                MDC.remove(MDC_KEY);
            }
        }
    }

    /**
     * 测试专用配置：把过滤器注册进 Spring 容器（仅 test scope 生效）
     * 将本类文件放在 src/test/java 下即可保证不会进入生产环境。
     */
    @TestConfiguration
    public static class Cfg {
        @Bean
        public Filter testTenantFilter() {
            return new TestTenantFilter();
        }
    }
}
