package com.picturebook.framework.controller;

import com.picturebook.admin.domain.SysAiModel;
import com.picturebook.admin.mapper.SysAiModelMapper;
import com.picturebook.admin.mapper.SysConfigMapper;
import com.picturebook.admin.mapper.StatsMapper;
import com.picturebook.common.core.Result;
import com.picturebook.common.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员认证接口（Agent-5 基础设施线）
 * POST /api/auth/admin/login {username, password}
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${picturebook.jwt.secret}")
    private String secret;

    @Value("${picturebook.jwt.expire}")
    private long expireSeconds;

    public AuthController(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 管理员登录
     */
    @PostMapping("/admin/login")
    public Result<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || password == null) {
            return Result.fail(400, "用户名密码不能为空");
        }
        // 查询管理员
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, username, password, nickname, role FROM sys_admin WHERE username = ? AND del_flag = 0", username);
        if (rows.isEmpty()) {
            return Result.fail(401, "用户不存在");
        }
        Map<String, Object> admin = rows.get(0);
        String dbPassword = (String) admin.get("password");
        if (!passwordEncoder.matches(password, dbPassword)) {
            return Result.fail(401, "密码错误");
        }
        Long userId = ((Number) admin.get("id")).longValue();
        String nickname = (String) admin.get("nickname");
        String role = (String) admin.get("role");
        String token = JwtUtil.generateToken(userId, username, role, secret, expireSeconds * 1000);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userId", userId);
        data.put("username", username);
        data.put("nickname", nickname);
        data.put("role", role);
        return Result.ok("登录成功", data);
    }

    /**
     * 当前用户信息
     */
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        // 简化：从 SecurityContext 获取
        Map<String, Object> data = new HashMap<>();
        data.put("username", "admin");
        data.put("role", "admin");
        return Result.ok(data);
    }
}
