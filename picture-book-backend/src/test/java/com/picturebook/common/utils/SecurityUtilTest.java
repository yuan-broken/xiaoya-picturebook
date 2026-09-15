package com.picturebook.common.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SecurityUtil 单元测试
 * 覆盖：密码加密、密码匹配校验
 *
 * @author Agent-test
 */
class SecurityUtilTest {

    @Test
    void encryptPassword_shouldReturnBCryptHash() {
        String raw = "123456";
        String encoded = SecurityUtil.encryptPassword(raw);

        assertNotNull(encoded);
        assertNotEquals(raw, encoded);
        // BCrypt 哈希以 $2a$ 开头，长度 60
        assertTrue(encoded.startsWith("$2a$"), "BCrypt hash should start with $2a$");
        assertEquals(60, encoded.length());
    }

    @Test
    void encryptPassword_sameInputShouldProduceDifferentHash() {
        // BCrypt 每次加密使用不同 salt，结果不同
        String h1 = SecurityUtil.encryptPassword("abc123");
        String h2 = SecurityUtil.encryptPassword("abc123");
        assertNotEquals(h1, h2);
    }

    @Test
    void matchesPassword_correctPassword_shouldReturnTrue() {
        String raw = "mySecret123";
        String encoded = SecurityUtil.encryptPassword(raw);
        assertTrue(SecurityUtil.matchesPassword(raw, encoded));
    }

    @Test
    void matchesPassword_wrongPassword_shouldReturnFalse() {
        String encoded = SecurityUtil.encryptPassword("correctPwd");
        assertFalse(SecurityUtil.matchesPassword("wrongPwd", encoded));
    }

    @Test
    void matchesPassword_emptyPassword_shouldReturnFalse() {
        String encoded = SecurityUtil.encryptPassword("anyPwd");
        assertFalse(SecurityUtil.matchesPassword("", encoded));
    }

    @Test
    void matchesPassword_nullEncoded_shouldReturnFalse() {
        // 传 null 加密哈希，不应抛异常
        assertFalse(SecurityUtil.matchesPassword("any", null));
    }

    @Test
    void encryptPassword_andMatchesFullRound_shouldWork() {
        // 端到端：加密 → 校验
        String[] testCases = {"123456", "abc!@#", "中文密码", "very-long-password-1234567890"};
        for (String pwd : testCases) {
            String encoded = SecurityUtil.encryptPassword(pwd);
            assertTrue(SecurityUtil.matchesPassword(pwd, encoded), "Failed for password: " + pwd);
            assertFalse(SecurityUtil.matchesPassword(pwd + "x", encoded));
        }
    }
}
