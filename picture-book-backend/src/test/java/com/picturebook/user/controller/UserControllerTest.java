package com.picturebook.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 集成测试
 * 覆盖账户系统全流程：家长登录 / 孩子登录 / 添加孩子 / 孩子档案 / 权限隔离
 * 使用 H2 内存数据库，预置数据：demo/123456（家长）、kid001/123456（孩子）
 *
 * @author Agent-test
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PARENT_LOGIN_BODY = "{\"username\":\"demo\",\"password\":\"123456\"}";
    private static final String CHILD_LOGIN_BODY = "{\"username\":\"kid001\",\"password\":\"123456\"}";

    // ==================== 家长登录 ====================

    @Test
    void parentLogin_correctCredentials_shouldReturn200AndToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PARENT_LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.familyId").value(2))
                .andExpect(jsonPath("$.data.nickname").isNotEmpty());
    }

    @Test
    void parentLogin_wrongPassword_shouldReturnError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("密码错误")));
    }

    @Test
    void parentLogin_nonExistentAccount_shouldReturnError() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ghost\",\"password\":\"any\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("账号不存在")));
    }

    // ==================== 孩子登录 ====================

    @Test
    void childLogin_correctCredentials_shouldReturn200AndTokenWithChildRole() throws Exception {
        mockMvc.perform(post("/api/auth/child/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CHILD_LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.role").value("child"))
                .andExpect(jsonPath("$.data.childId").isNotEmpty())
                .andExpect(jsonPath("$.data.familyId").value(2));
    }

    @Test
    void childLogin_wrongPassword_shouldReturnError() throws Exception {
        mockMvc.perform(post("/api/auth/child/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"kid001\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("密码错误")));
    }

    @Test
    void childLogin_nonExistentAccount_shouldReturnError() throws Exception {
        mockMvc.perform(post("/api/auth/child/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ghost_kid\",\"password\":\"any\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("孩子账号不存在")));
    }

    // ==================== 添加孩子（需家长 Token） ====================

    @Test
    void addChild_withParentToken_shouldReturnChildId() throws Exception {
        String parentToken = obtainParentToken();
        String addChildBody = objectMapper.writeValueAsString(Map.of(
                "username", "test_kid_" + System.currentTimeMillis(),
                "password", "abc123",
                "nickname", "测试孩子",
                "birthday", "2019-06-15"
        ));

        mockMvc.perform(post("/api/user/child")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addChildBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    void addChild_duplicateUsername_shouldReturnError() throws Exception {
        // 使用新注册家长避免触发孩子数量上限
        String uniqueParent = "parent_" + System.currentTimeMillis();
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "username", uniqueParent,
                "password", "parent123",
                "nickname", "新家长",
                "phone", "13900000000"
        ));
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk());

        String newParentToken = obtainToken(uniqueParent, "parent123");
        // kid001 已存在于预置数据中
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "kid001",
                "password", "abc123",
                "nickname", "重复孩子"
        ));

        mockMvc.perform(post("/api/user/child")
                        .header("Authorization", "Bearer " + newParentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("已被使用")));
    }

    @Test
    void addChild_withoutToken_shouldReturn401or403() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "no_auth_kid",
                "password", "abc123"
        ));

        mockMvc.perform(post("/api/user/child")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    // ==================== 孩子档案（需孩子 Token） ====================

    @Test
    void getChildProfile_withChildToken_shouldReturnProfile() throws Exception {
        String childToken = obtainChildToken();

        mockMvc.perform(get("/api/child/profile")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("kid001"))
                .andExpect(jsonPath("$.data.role").value("child"))
                .andExpect(jsonPath("$.data.ageGroup").isNotEmpty());
    }

    @Test
    void getChildProfile_withParentToken_shouldReturnFail() throws Exception {
        String parentToken = obtainParentToken();

        mockMvc.perform(get("/api/child/profile")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("仅孩子账号")));
    }

    @Test
    void getChildProfile_withoutToken_shouldReturn401or403() throws Exception {
        mockMvc.perform(get("/api/child/profile"))
                .andExpect(status().is4xxClientError());
    }

    // ==================== 孩子获取家长信息 ====================

    @Test
    void getChildFamily_withChildToken_shouldReturnParentInfo() throws Exception {
        String childToken = obtainChildToken();

        mockMvc.perform(get("/api/child/family")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.parentName").isNotEmpty())
                .andExpect(jsonPath("$.data.phone").isNotEmpty());
    }

    // ==================== 权限隔离：孩子访问家长接口应被拒绝 ====================

    @Test
    void childTokenAccessParentEndpoint_shouldReturnBusinessError() throws Exception {
        String childToken = obtainChildToken();

        // 孩子用 child token 访问家长首页接口
        // 安全配置要求 authenticated，child token 已认证但 familyId 为孩子 ID
        // getHomeData 会用 childId 当 familyId 查询，导致"家庭不存在"业务错误
        mockMvc.perform(get("/api/user/home")
                        .header("Authorization", "Bearer " + childToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    void parentTokenAccessChildEndpoint_shouldReturnFail() throws Exception {
        String parentToken = obtainParentToken();

        mockMvc.perform(get("/api/child/profile")
                        .header("Authorization", "Bearer " + parentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    // ==================== 完整流程：家长添加孩子→孩子登录 ====================

    @Test
    void fullFlow_parentAddChild_thenChildLogin_shouldSucceed() throws Exception {
        String parentToken = obtainParentToken();
        String uniqueUsername = "flow_kid_" + System.currentTimeMillis();

        // 1. 家长添加孩子
        String addChildBody = objectMapper.writeValueAsString(Map.of(
                "username", uniqueUsername,
                "password", "test123",
                "nickname", "流程测试孩子",
                "birthday", "2018-03-20"
        ));
        MvcResult addResult = mockMvc.perform(post("/api/user/child")
                        .header("Authorization", "Bearer " + parentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addChildBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();

        // 2. 孩子用新账号登录
        String childLoginBody = objectMapper.writeValueAsString(Map.of(
                "username", uniqueUsername,
                "password", "test123"
        ));
        mockMvc.perform(post("/api/auth/child/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(childLoginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value(uniqueUsername))
                .andExpect(jsonPath("$.data.role").value("child"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    // ==================== 工具方法 ====================

    private String obtainParentToken() throws Exception {
        return obtainToken("demo", "123456");
    }

    private String obtainChildToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/child/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CHILD_LOGIN_BODY))
                .andReturn();
        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).at("/data/token").asText();
    }

    private String obtainToken(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("username", username, "password", password));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
        String response = result.getResponse().getContentAsString();
        return objectMapper.readTree(response).at("/data/token").asText();
    }
}
