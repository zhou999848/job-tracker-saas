// src/main/java/com/example/jobtracker/security/SessionKickoutService.java
package com.example.jobtracker.security;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class SessionKickoutService {

    private final SessionRegistry sessionRegistry;

    public SessionKickoutService(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    /** 踢掉该用户名的所有会话，返回踢掉数量 */
    public int kickout(String username) {
        int count = 0;
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            String name = (principal instanceof UserDetails ud) ? ud.getUsername() : principal.toString();
            if (!username.equals(name)) continue;
            for (SessionInformation s : sessionRegistry.getAllSessions(principal, false)) {
                s.expireNow(); // 标记会话为过期（下一次请求即失效）
                count++;
            }
        }
        return count;
    }
}
