package com.picturebook.user.service;

import com.picturebook.common.exception.BusinessException;
import com.picturebook.user.domain.Family;
import com.picturebook.user.mapper.FamilyMapper;
import com.picturebook.user.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 用户与家长服务单元测试
 * 覆盖：注册、登录、退出登录、密码修改
 *
 * @author Test-Agent
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务测试")
class UserServiceTest {

    @Mock
    private FamilyMapper familyMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // 注入 @Value 属性（JWT key 必须 >= 256 bits = 32 bytes）
        ReflectionTestUtils.setField(userService, "jwtSecret", "TestJwtSecret2026ForUnitTestsAesKey256");
        ReflectionTestUtils.setField(userService, "jwtExpireSeconds", 86400L);
    }

    // ==================== 注册测试 ====================

    @Test
    @DisplayName("注册 - 成功")
    void register_success() {
        // given
        Family family = new Family();
        family.setUsername("test_parent_001");
        family.setPassword("test123456");
        family.setNickname("测试家长");
        family.setPhone("13800000001");
        when(familyMapper.selectCount(any())).thenReturn(0L);
        // mock insert 回填 ID
        doAnswer(invocation -> {
            Family f = invocation.getArgument(0);
            f.setFamilyId(5001L);
            return 1;
        }).when(familyMapper).insert(any(Family.class));

        // when
        Long familyId = userService.register(family);

        // then
        assertNotNull(familyId);
        assertEquals(5001L, familyId);
        assertEquals("test_parent_001", family.getUsername());
        assertNotNull(family.getPassword());
        assertEquals("free", family.getMemberLevel());
        assertEquals(1, family.getStatus());
        assertEquals("0", family.getDelFlag());
        verify(familyMapper).insert(family);
    }

    @Test
    @DisplayName("注册 - 账号重复应抛异常")
    void register_duplicateThrows() {
        // given
        Family family = new Family();
        family.setUsername("demo");
        family.setPassword("test123456");
        when(familyMapper.selectCount(any())).thenReturn(1L);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.register(family));
        assertTrue(ex.getMessage().contains("账号已存在"));
        verify(familyMapper, never()).insert(any());
    }

    @Test
    @DisplayName("注册 - 密码应加密存储")
    void register_passwordEncrypted() {
        // given
        Family family = new Family();
        family.setUsername("test_pwd_enc");
        family.setPassword("plaintext123");
        family.setNickname("加密测试");
        when(familyMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            Family f = invocation.getArgument(0);
            f.setFamilyId(5002L);
            return 1;
        }).when(familyMapper).insert(any(Family.class));

        // when
        userService.register(family);

        // then
        assertNotEquals("plaintext123", family.getPassword());
        assertTrue(family.getPassword().startsWith("$2a$"));
    }

    // ==================== 登录测试 ====================

    @Test
    @DisplayName("登录 - 成功返回 token")
    void login_success() {
        // given
        Family family = new Family();
        family.setFamilyId(1001L);
        family.setUsername("demo");
        family.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("123456"));
        family.setNickname("演示家长");
        family.setMemberLevel("free");
        family.setStatus(1);
        when(familyMapper.selectOne(any())).thenReturn(family);

        // when
        Map<String, Object> result = userService.login("demo", "123456");

        // then
        assertNotNull(result);
        assertEquals(1001L, result.get("familyId"));
        assertEquals("演示家长", result.get("nickname"));
        assertEquals("free", result.get("memberLevel"));
        assertNotNull(result.get("token"));
        assertTrue(result.get("token").toString().length() > 20);
    }

    @Test
    @DisplayName("登录 - 账号不存在")
    void login_userNotFound() {
        when(familyMapper.selectOne(any())).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login("nouser", "123456"));
        assertTrue(ex.getMessage().contains("账号不存在"));
    }

    @Test
    @DisplayName("登录 - 密码错误")
    void login_wrongPassword() {
        Family family = new Family();
        family.setFamilyId(1001L);
        family.setUsername("demo");
        family.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("correct_pwd"));
        family.setStatus(1);
        when(familyMapper.selectOne(any())).thenReturn(family);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login("demo", "wrong_pwd"));
        assertTrue(ex.getMessage().contains("密码错误"));
    }

    @Test
    @DisplayName("登录 - 账号已停用")
    void login_accountDisabled() {
        Family family = new Family();
        family.setFamilyId(1001L);
        family.setUsername("demo");
        family.setStatus(0);
        when(familyMapper.selectOne(any())).thenReturn(family);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login("demo", "123456"));
        assertTrue(ex.getMessage().contains("已停用"));
    }

    // ==================== 退出登录测试 ====================

    @Test
    @DisplayName("退出登录 - 成功更新时间")
    void logout_success() {
        // given
        Long familyId = 1001L;

        // when
        userService.logout(familyId);

        // then
        verify(familyMapper).updateById(any(Family.class));
    }

    @Test
    @DisplayName("退出登录 - familyId 为 null 应跳过")
    void logout_nullFamilyId() {
        userService.logout(null);
        verify(familyMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("退出登录 - 数据库异常应容错不抛")
    void logout_dbExceptionSwallowed() {
        when(familyMapper.updateById(any())).thenThrow(new RuntimeException("DB down"));
        assertDoesNotThrow(() -> userService.logout(1001L));
    }

    // ==================== 修改密码测试 ====================

    @Test
    @DisplayName("修改密码 - 成功")
    void changePassword_success() {
        Family family = new Family();
        family.setFamilyId(1001L);
        family.setUsername("demo");
        family.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("old123"));
        when(familyMapper.selectById(1001L)).thenReturn(family);

        userService.changePassword(1001L, "old123", "new123456");
        verify(familyMapper).updateById(any(Family.class));
    }

    @Test
    @DisplayName("修改密码 - 旧密码错误")
    void changePassword_wrongOld() {
        Family family = new Family();
        family.setFamilyId(1001L);
        family.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("correct_old"));
        when(familyMapper.selectById(1001L)).thenReturn(family);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changePassword(1001L, "wrong_old", "new123456"));
        assertTrue(ex.getMessage().contains("原密码错误"));
    }

    @Test
    @DisplayName("修改密码 - 新密码太短")
    void changePassword_tooShort() {
        Family family = new Family();
        family.setFamilyId(1001L);
        family.setPassword(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("old123"));
        when(familyMapper.selectById(1001L)).thenReturn(family);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changePassword(1001L, "old123", "abc"));
        assertTrue(ex.getMessage().contains("至少 6 位"));
    }
}
