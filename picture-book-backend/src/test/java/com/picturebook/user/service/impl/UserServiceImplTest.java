package com.picturebook.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.picturebook.common.exception.BusinessException;
import com.picturebook.user.domain.ChildProfile;
import com.picturebook.user.domain.Family;
import com.picturebook.user.mapper.ChildProfileMapper;
import com.picturebook.user.mapper.FamilyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserServiceImpl 单元测试
 * 覆盖账户系统核心方法：login / childLogin / addChild / register / changePassword
 *
 * @author Agent-test
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private FamilyMapper familyMapper;

    @Mock
    private ChildProfileMapper childProfileMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private static final String JWT_SECRET = "picturebook-secret-key-for-jwt-signing-2026";

    @BeforeEach
    void setUp() {
        // 注入 @Value 字段
        ReflectionTestUtils.setField(userService, "jwtSecret", JWT_SECRET);
        ReflectionTestUtils.setField(userService, "jwtExpireSeconds", 86400L);
    }

    // ==================== 家长登录 login() ====================

    @Test
    void login_correctCredentials_shouldReturnTokenAndFamilyId() {
        Family family = new Family();
        family.setFamilyId(2L);
        family.setUsername("demo");
        family.setPassword("$2a$10$Ui6vDsSTgRYtnBzGvgAVsOVZGPIXTJyQjJnZiFmcRtCdIsTqxpPsC"); // 123456
        family.setNickname("测试家长");
        family.setMemberLevel("family");
        family.setStatus(1);

        when(familyMapper.selectOne(any(Wrapper.class))).thenReturn(family);

        Map<String, Object> result = userService.login("demo", "123456");

        assertNotNull(result);
        assertEquals(2L, result.get("familyId"));
        assertEquals("测试家长", result.get("nickname"));
        assertNotNull(result.get("token"));
        assertTrue(((String) result.get("token")).length() > 20);
    }

    @Test
    void login_accountNotExist_shouldThrowBusinessException() {
        when(familyMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login("ghost", "any"));
        assertTrue(ex.getMessage().contains("账号不存在"));
    }

    @Test
    void login_wrongPassword_shouldThrowBusinessException() {
        Family family = new Family();
        family.setFamilyId(1L);
        family.setUsername("demo");
        family.setPassword("$2a$10$Ui6vDsSTgRYtnBzGvgAVsOVZGPIXTJyQjJnZiFmcRtCdIsTqxpPsC"); // 123456
        family.setStatus(1);

        when(familyMapper.selectOne(any(Wrapper.class))).thenReturn(family);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login("demo", "wrongPwd"));
        assertTrue(ex.getMessage().contains("密码错误"));
    }

    @Test
    void login_disabledAccount_shouldThrowBusinessException() {
        Family family = new Family();
        family.setStatus(0); // 停用
        when(familyMapper.selectOne(any(Wrapper.class))).thenReturn(family);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.login("demo", "any"));
        assertTrue(ex.getMessage().contains("已停用"));
    }

    // ==================== 孩子登录 childLogin() ====================

    @Test
    void childLogin_correctCredentials_shouldReturnChildInfoAndToken() {
        ChildProfile child = new ChildProfile();
        child.setChildId(3L);
        child.setFamilyId(2L);
        child.setUsername("kid001");
        child.setPassword("$2a$10$Ui6vDsSTgRYtnBzGvgAVsOVZGPIXTJyQjJnZiFmcRtCdIsTqxpPsC"); // 123456
        child.setNickname("林小满");
        child.setAgeGroup("preschool");
        child.setBindStatus(1); // 已绑定

        when(childProfileMapper.selectOne(any(Wrapper.class))).thenReturn(child);

        Map<String, Object> result = userService.childLogin("kid001", "123456");

        assertNotNull(result);
        assertEquals(3L, result.get("childId"));
        assertEquals(2L, result.get("familyId"));
        assertEquals("kid001", result.get("username"));
        assertEquals("child", result.get("role"));
        assertNotNull(result.get("token"));
        verify(childProfileMapper).updateById(any(ChildProfile.class)); // 应更新 lastLoginAt
    }

    @Test
    void childLogin_accountNotExist_shouldThrowBusinessException() {
        when(childProfileMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.childLogin("ghost", "any"));
        assertTrue(ex.getMessage().contains("孩子账号不存在"));
    }

    @Test
    void childLogin_wrongPassword_shouldThrowBusinessException() {
        ChildProfile child = new ChildProfile();
        child.setChildId(1L);
        child.setPassword("$2a$10$Ui6vDsSTgRYtnBzGvgAVsOVZGPIXTJyQjJnZiFmcRtCdIsTqxpPsC"); // 123456
        child.setBindStatus(1);

        when(childProfileMapper.selectOne(any(Wrapper.class))).thenReturn(child);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.childLogin("kid001", "wrongPwd"));
        assertTrue(ex.getMessage().contains("密码错误"));
    }

    @Test
    void childLogin_notBound_shouldThrowBusinessException() {
        ChildProfile child = new ChildProfile();
        child.setChildId(1L);
        child.setPassword("$2a$10$Ui6vDsSTgRYtnBzGvgAVsOVZGPIXTJyQjJnZiFmcRtCdIsTqxpPsC"); // 123456
        child.setBindStatus(0); // 未绑定

        when(childProfileMapper.selectOne(any(Wrapper.class))).thenReturn(child);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.childLogin("kid001", "123456"));
        assertTrue(ex.getMessage().contains("尚未绑定"));
    }

    @Test
    void childLogin_emptyPasswordInDb_shouldThrowBusinessException() {
        ChildProfile child = new ChildProfile();
        child.setChildId(1L);
        child.setPassword(null); // 未设密码
        child.setBindStatus(1);

        when(childProfileMapper.selectOne(any(Wrapper.class))).thenReturn(child);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.childLogin("kid001", "any"));
        assertTrue(ex.getMessage().contains("密码错误"));
    }

    // ==================== 添加孩子 addChild() ====================

    @Test
    void addChild_success_shouldEncryptPasswordAndInsert() {
        Family family = new Family();
        family.setFamilyId(2L);
        family.setUsername("demo");
        family.setMemberLevel("family");

        when(familyMapper.selectById(2L)).thenReturn(family);
        when(childProfileMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        when(childProfileMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        ChildProfile child = new ChildProfile();
        child.setFamilyId(2L);
        child.setUsername("new_kid");
        child.setPassword("123456");
        child.setNickname("新孩子");
        child.setBirthday(new Date());

        Long childId = userService.addChild(child);

        // insert 后 MyBatis 会回填 ID，这里 mock 不会实际回填
        // 但应验证调用了 insert 且密码被加密
        verify(childProfileMapper).insert(any(ChildProfile.class));
        // 密码应该被加密（BCrypt 以 $2a$ 开头）
        assertTrue(child.getPassword().startsWith("$2a$"));
        assertEquals(1, child.getBindStatus());
    }

    @Test
    void addChild_familyNotExist_shouldThrowBusinessException() {
        when(familyMapper.selectById(999L)).thenReturn(null);

        ChildProfile child = new ChildProfile();
        child.setFamilyId(999L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.addChild(child));
        assertTrue(ex.getMessage().contains("家庭不存在"));
    }

    @Test
    void addChild_exceedFreeLimit_shouldThrowBusinessException() {
        Family family = new Family();
        family.setFamilyId(1L);
        family.setUsername("freeUser");
        family.setMemberLevel("free"); // 免费版上限 1

        // 已有 1 个孩子
        ChildProfile existing = new ChildProfile();
        existing.setChildId(10L);
        when(familyMapper.selectById(1L)).thenReturn(family);
        when(childProfileMapper.selectList(any(Wrapper.class))).thenReturn(Collections.singletonList(existing));

        ChildProfile child = new ChildProfile();
        child.setFamilyId(1L);
        child.setUsername("kid_new");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.addChild(child));
        assertTrue(ex.getMessage().contains("上限"));
    }

    @Test
    void addChild_duplicateUsername_shouldThrowBusinessException() {
        Family family = new Family();
        family.setFamilyId(2L);
        family.setUsername("demo");
        family.setMemberLevel("family");

        when(familyMapper.selectById(2L)).thenReturn(family);
        when(childProfileMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());
        // 已存在同名账号
        when(childProfileMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        ChildProfile child = new ChildProfile();
        child.setFamilyId(2L);
        child.setUsername("kid001"); // 重复
        child.setPassword("123456");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.addChild(child));
        assertTrue(ex.getMessage().contains("已被使用"));
    }

    // ==================== 家长注册 register() ====================

    @Test
    void register_success_shouldEncryptPasswordAndReturnId() {
        when(familyMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        Family family = new Family();
        family.setUsername("new_parent");
        family.setPassword("myPass123");

        Long id = userService.register(family);

        verify(familyMapper).insert(any(Family.class));
        assertTrue(family.getPassword().startsWith("$2a$"));
        assertEquals("free", family.getMemberLevel());
        assertEquals(1, family.getStatus());
    }

    @Test
    void register_duplicateUsername_shouldThrowBusinessException() {
        when(familyMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        Family family = new Family();
        family.setUsername("demo");
        family.setPassword("any");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.register(family));
        assertTrue(ex.getMessage().contains("已存在"));
    }

    // ==================== 修改密码 changePassword() ====================

    @Test
    void changePassword_correctOldAndValidNew_shouldUpdate() {
        Family family = new Family();
        family.setFamilyId(1L);
        family.setUsername("demo");
        family.setPassword("$2a$10$Ui6vDsSTgRYtnBzGvgAVsOVZGPIXTJyQjJnZiFmcRtCdIsTqxpPsC"); // 123456

        when(familyMapper.selectById(1L)).thenReturn(family);

        userService.changePassword(1L, "123456", "newPass789");

        verify(familyMapper).updateById(argThat(f -> {
            String pwd = f.getPassword();
            return pwd != null && pwd.startsWith("$2a$");
        }));
    }

    @Test
    void changePassword_wrongOld_shouldThrowBusinessException() {
        Family family = new Family();
        family.setFamilyId(1L);
        family.setPassword("$2a$10$Ui6vDsSTgRYtnBzGvgAVsOVZGPIXTJyQjJnZiFmcRtCdIsTqxpPsC"); // 123456

        when(familyMapper.selectById(1L)).thenReturn(family);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changePassword(1L, "wrongOld", "newPass789"));
        assertTrue(ex.getMessage().contains("原密码错误"));
    }

    @Test
    void changePassword_newPasswordTooShort_shouldThrowBusinessException() {
        Family family = new Family();
        family.setFamilyId(1L);
        family.setPassword("$2a$10$Ui6vDsSTgRYtnBzGvgAVsOVZGPIXTJyQjJnZiFmcRtCdIsTqxpPsC"); // 123456

        when(familyMapper.selectById(1L)).thenReturn(family);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changePassword(1L, "123456", "123")); // 新密码仅 3 位
        assertTrue(ex.getMessage().contains("至少 6 位"));
    }

    @Test
    void changePassword_familyNotExist_shouldThrowBusinessException() {
        when(familyMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changePassword(999L, "any", "newPass789"));
        assertTrue(ex.getMessage().contains("账号不存在"));
    }

    // ==================== 内容筛选设置 updateSettings() ====================

    @Test
    void updateSettings_validRange_shouldUpdate() {
        Family family = new Family();
        family.setFamilyId(1L);
        family.setUsername("demo");
        family.setDailyLimitMin(30);
        family.setSingleLimitMin(15);

        when(familyMapper.selectById(1L)).thenReturn(family);

        userService.updateSettings(1L, "music,art", "", 60, 30, "1");

        verify(familyMapper).updateById(any(Family.class));
    }

    @Test
    void updateSettings_dailyExceedMax_shouldThrowBusinessException() {
        Family family = new Family();
        family.setFamilyId(1L);
        when(familyMapper.selectById(1L)).thenReturn(family);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateSettings(1L, "", "", 300, 30, "0")); // 300 > 240
        assertTrue(ex.getMessage().contains("每日时长上限"));
    }

    @Test
    void updateSettings_singleExceedDaily_shouldThrowBusinessException() {
        Family family = new Family();
        family.setFamilyId(1L);
        when(familyMapper.selectById(1L)).thenReturn(family);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.updateSettings(1L, "", "", 30, 60, "0")); // 60 > 30
        assertTrue(ex.getMessage().contains("单次上限不能大于每日上限"));
    }
}
