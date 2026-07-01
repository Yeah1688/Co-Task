package com.cotask.service.impl;

import com.cotask.entity.User;
import com.cotask.entity.Workspace;
import com.cotask.repository.UserRepository;
import com.cotask.security.JwtTokenProvider;
import com.cotask.service.AuthService;
import com.cotask.service.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private WorkspaceService workspaceService;

    @Override
    @Transactional
    public User register(String email, String name, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("该邮箱已被注册");
        }

        User user = User.builder()
                .email(email)
                .name(name)
                .passwordHash(passwordEncoder.encode(password))
                .build();

        User savedUser = userRepository.save(user);

        // 自动为新用户创建默认工作区
        try {
            Workspace defaultWorkspace = workspaceService.createWorkspace(
                    name + "的工作区",
                    "这是 " + name + " 的默认工作区",
                    savedUser.getId()
            );
            System.out.println("为用户 " + name + " 自动创建默认工作区: " + defaultWorkspace.getName());
        } catch (Exception e) {
            System.err.println("创建默认工作区失败: " + e.getMessage());
        }

        return savedUser;
    }

    @Override
    public Map<String, Object> login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("邮箱或密码错误"));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("邮箱或密码错误");
        }

        // 生成真实的 JWT Token
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail());

        Map<String, Object> result = new HashMap<>();
        result.put("token", accessToken);
        result.put("user", Map.of("id", user.getId(), "name", user.getName(), "email", user.getEmail()));

        return result;
    }
}
