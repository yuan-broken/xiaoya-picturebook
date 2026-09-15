package com.picturebook.framework.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器
 * 启动时确保管理员密码哈希正确
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 确保 admin 密码为 admin123（用 BCrypt 重新编码）
        String hash = passwordEncoder.encode("admin123");
        jdbcTemplate.update("UPDATE sys_admin SET password = ? WHERE username = 'admin'", hash);
        // 确保 family 密码为 family123
        String familyHash = passwordEncoder.encode("family123");
        jdbcTemplate.update("UPDATE biz_family SET password = ? WHERE username = 'family'", familyHash);
    }
}
