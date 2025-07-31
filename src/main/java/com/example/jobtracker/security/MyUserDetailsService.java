
package com.example.jobtracker.security;

import com.example.jobtracker.domain.User;
import com.example.jobtracker.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;

import org.springframework.stereotype.Service;

@Service
public class MyUserDetailsService implements UserDetailsService {
    private final UserRepository repo;
    public MyUserDetailsService(UserRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {//用提交的用户名在数据库中查找用户
        User user = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户名不存在"));

        return org.springframework.security.core.userdetails.User//构建并返回含用户信息的userdetails对象
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("USER")
                .build();
    }
}