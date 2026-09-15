package com.picturebook.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试
 * 覆盖：Token 生成/解析、角色/用户名提取、过期/篡改异常
 *
 * @author Agent-test
 */
class JwtUtilTest {

    private static final String SECRET = "picturebook-secret-key-for-jwt-signing-2026";

    @Test
    void generateToken_shouldContainUserIdUsernameAndRole() {
        String token = JwtUtil.generateToken(100L, "demo", "parent", SECRET, 60000L);

        Claims claims = JwtUtil.parseToken(token, SECRET);
        assertNotNull(claims);
        assertEquals(100L, JwtUtil.getUserId(claims));
        assertEquals("demo", JwtUtil.getUsername(claims));
        assertEquals("parent", JwtUtil.getRole(claims));
    }

    @Test
    void generateToken_childRole_shouldPreserveChildRole() {
        String token = JwtUtil.generateToken(5L, "kid001", "child", SECRET, 60000L);

        Claims claims = JwtUtil.parseToken(token, SECRET);
        assertEquals("child", JwtUtil.getRole(claims));
        assertEquals(5L, JwtUtil.getUserId(claims));
    }

    @Test
    void parseToken_expiredToken_shouldThrowExpiredJwtException() throws InterruptedException {
        // 过期时间 1ms
        String token = JwtUtil.generateToken(1L, "u", "parent", SECRET, 1L);
        Thread.sleep(20L); // 等待 token 过期

        assertThrows(ExpiredJwtException.class, () -> JwtUtil.parseToken(token, SECRET));
    }

    @Test
    void parseToken_wrongSecret_shouldThrowJwtException() {
        String token = JwtUtil.generateToken(1L, "demo", "parent", SECRET, 60000L);
        assertThrows(JwtException.class, () -> JwtUtil.parseToken(token, "another-wrong-secret-key-for-test-purposes-xxxx"));
    }

    @Test
    void parseToken_tamperedToken_shouldThrowJwtException() {
        String token = JwtUtil.generateToken(1L, "demo", "parent", SECRET, 60000L);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThrows(JwtException.class, () -> JwtUtil.parseToken(tampered, SECRET));
    }

    @Test
    void getUserId_integerValue_shouldConvertToLong() {
        // 当 userId 较小时，JSON 反序列化可能得到 Integer
        String token = JwtUtil.generateToken(3L, "kid", "child", SECRET, 60000L);
        Claims claims = JwtUtil.parseToken(token, SECRET);
        // 应该返回 Long 类型
        assertEquals(3L, JwtUtil.getUserId(claims));
    }

    @Test
    void generateToken_twoTokensShouldDifferByIat() throws InterruptedException {
        String t1 = JwtUtil.generateToken(1L, "u", "parent", SECRET, 60000L);
        Thread.sleep(1100L); // JWT iat 精度为秒，需等待超过 1 秒
        String t2 = JwtUtil.generateToken(1L, "u", "parent", SECRET, 60000L);
        assertNotEquals(t1, t2);
    }
}
