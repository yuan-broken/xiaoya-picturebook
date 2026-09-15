package com.picturebook.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.picturebook.user.domain.Family;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 账户系统全流程集成测试
 * 覆盖：注册 → 登录 → 退出 → 试用 → 重复试用拦截
 *
 * @author Test-Agent
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DisplayName("账户系统全流程集成测试")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper om = new ObjectMapper();

    // ==================== 注册测试 ====================

    @Test
    @DisplayName("注册 - 成功注册新家长")
    void register_success() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", "itest_reg_" + System.currentTimeMillis());
        body.put("password", "test123456");
        body.put("nickname", "集成测试家长");
        body.put("phone", "13900000000");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    @DisplayName("注册 - 重复注册应失败")
    void register_duplicate_fails() throws Exception {
        String username = "itest_dup_" + System.currentTimeMillis();
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", "test123456");
        body.put("nickname", "重复测试");
        body.put("phone", "13900000001");

        // 第一次注册成功
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(jsonPath("$.code").value(200));

        // 第二次注册失败
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("已存在")));
    }

    // ==================== 登录测试 ====================

    @Test
    @DisplayName("登录 - 正确账号密码")
    void login_success() throws Exception {
        // 先注册
        String username = "itest_login_" + System.currentTimeMillis();
        registerUser(username, "test123456");

        // 再登录
        Map<String, String> loginBody = new HashMap<>();
        loginBody.put("username", username);
        loginBody.put("password", "test123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.familyId").isNumber())
                .andExpect(jsonPath("$.data.memberLevel").value("free"));
    }

    @Test
    @DisplayName("登录 - 密码错误应失败")
    void login_wrongPassword() throws Exception {
        String username = "itest_wrong_" + System.currentTimeMillis();
        registerUser(username, "correct_pwd_123");

        Map<String, String> loginBody = new HashMap<>();
        loginBody.put("username", username);
        loginBody.put("password", "wrong_password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(loginBody)))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("密码错误")));
    }

    @Test
    @DisplayName("登录 - 账号不存在")
    void login_userNotFound() throws Exception {
        Map<String, String> loginBody = new HashMap<>();
        loginBody.put("username", "no_such_user_xyz");
        loginBody.put("password", "whatever");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(loginBody)))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("账号不存在")));
    }

    // ==================== 退出登录测试 ====================

    @Test
    @DisplayName("退出登录 - 成功返回")
    void logout_success() throws Exception {
        // 注册+登录拿 token
        String username = "itest_logout_" + System.currentTimeMillis();
        registerUser(username, "test123456");
        String token = loginAndGetToken(username, "test123456");

        // 调退出接口
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // logout 在白名单，即使不带 token 也能调用成功
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 试用 7 天测试 ====================

    @Test
    @DisplayName("试用 - 首次开通成功")
    void trial_firstTime_success() throws Exception {
        String username = "itest_trial1_" + System.currentTimeMillis();
        registerUser(username, "test123456");
        String token = loginAndGetToken(username, "test123456");

        mockMvc.perform(post("/api/user/trial")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("试用 - 同一家长第二次试用应被拒绝")
    void trial_secondTime_rejected() throws Exception {
        String username = "itest_trial2_" + System.currentTimeMillis();
        registerUser(username, "test123456");
        String token = loginAndGetToken(username, "test123456");

        // 第一次试用成功
        mockMvc.perform(post("/api/user/trial")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(200));

        // 第二次试用失败
        mockMvc.perform(post("/api/user/trial")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("已开通过")));
    }

    @Test
    @DisplayName("试用 - 开通后会员等级应为 growth")
    void trial_memberLevelUpgraded() throws Exception {
        String username = "itest_trial3_" + System.currentTimeMillis();
        registerUser(username, "test123456");
        String token = loginAndGetToken(username, "test123456");

        // 开通前
        mockMvc.perform(get("/api/user/member")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.level").value("free"));

        // 开通试用
        mockMvc.perform(post("/api/user/trial")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(200));

        // 开通后
        mockMvc.perform(get("/api/user/member")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.data.level").value("growth"))
                .andExpect(jsonPath("$.data.daysRemaining").value(org.hamcrest.Matchers.lessThanOrEqualTo(7)))
                .andExpect(jsonPath("$.data.daysRemaining").value(org.hamcrest.Matchers.greaterThanOrEqualTo(6)));
    }

    // ==================== 认证保护测试 ====================

    @Test
    @DisplayName("未认证访问受保护接口应返回401")
    void protectedApi_noAuth_401() throws Exception {
        mockMvc.perform(get("/api/user/member"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("无效Token访问受保护接口应返回401")
    void protectedApi_invalidToken_401() throws Exception {
        mockMvc.perform(get("/api/user/member")
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== 辅助方法 ====================

    /** 注册用户 */
    private void registerUser(String username, String password) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("username", username);
        body.put("password", password);
        body.put("nickname", username);
        body.put("phone", "13900000000");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(jsonPath("$.code").value(200));
    }

    /** 登录并返回 token */
    private String loginAndGetToken(String username, String password) throws Exception {
        Map<String, String> loginBody = new HashMap<>();
        loginBody.put("username", username);
        loginBody.put("password", password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(loginBody)))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        Map<String, Object> resp = om.readValue(body, Map.class);
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        return (String) data.get("token");
    }
}
