package com.picturebook.ai.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.picturebook.ai.domain.AiRole;
import com.picturebook.ai.domain.ChatMessage;
import com.picturebook.ai.domain.ChatSession;
import com.picturebook.ai.domain.StoryCreation;
import com.picturebook.ai.domain.StoryTheme;
import com.picturebook.ai.dto.SendMessageRequest;
import com.picturebook.ai.dto.StoryCreateRequest;
import com.picturebook.ai.mapper.ChatMessageMapper;
import com.picturebook.ai.mapper.ChatSessionMapper;
import com.picturebook.ai.mapper.StoryCreationMapper;
import com.picturebook.common.constant.CacheConstants;
import com.picturebook.common.exception.BusinessException;
import com.picturebook.common.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * AI故事创作与角色对话服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final StoryCreationMapper storyCreationMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final AiRoleService aiRoleService;
    private final StoryThemeService storyThemeService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${picturebook.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${picturebook.ai.default-reading-model:qwen2.5}")
    private String defaultModel;

    @Value("${picturebook.ai.llm.provider:mock}")
    private String llmProvider;

    @Value("${picturebook.ai.llm.model:qwen-turbo}")
    private String llmModel;

    @Value("${picturebook.ai.llm.api-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String llmApiUrl;

    @Value("${picturebook.ai.llm.api-key:}")
    private String llmApiKey;

    @Value("${picturebook.ai.llm.temperature:0.7}")
    private double llmTemperature;

    @Value("${picturebook.ai.llm.max-token:2048}")
    private int llmMaxToken;

    // ============ 故事创作 ============

    /**
     * 创建AI故事
     */
    public StoryCreation createStory(StoryCreateRequest request) {
        Long familyId = SecurityUtil.getUserId();

        StoryCreation creation = new StoryCreation();
        creation.setFamilyId(familyId);
        creation.setRoleId(request.getRoleId());
        creation.setRoleName(request.getRoleName());
        creation.setThemeId(request.getThemeId());
        creation.setStoryInput(request.getStoryInput());
        creation.setGenerationState("generating");
        creation.setAuditStatus("0");
        creation.setGenerationModel(defaultModel);
        creation.setCreateBy(SecurityUtil.getUsername());
        creation.setCreateTime(new Date());
        storyCreationMapper.insert(creation);

        // 生成故事内容（第一期Mock）
        long startMs = System.currentTimeMillis();
        try {
            String storyContent = generateStoryContent(request);
            creation.setStoryContent(storyContent);
            creation.setStoryPages(6);
            creation.setGenerationState("success");
            creation.setAuditStatus("1"); // Mock自动通过审核
        } catch (Exception e) {
            creation.setGenerationState("failed");
            creation.setErrorMsg(e.getMessage());
        }
        creation.setDurationMs(System.currentTimeMillis() - startMs);
        creation.setUpdateBy(SecurityUtil.getUsername());
        creation.setUpdateTime(new Date());
        storyCreationMapper.updateById(creation);

        log.info("故事创作完成：creationId={}, state={}", creation.getCreationId(), creation.getGenerationState());
        return creation;
    }

    /**
     * 查询故事创作状态
     */
    public StoryCreation getStoryStatus(Long creationId) {
        StoryCreation creation = storyCreationMapper.selectById(creationId);
        if (creation == null) {
            throw new BusinessException("故事创作不存在");
        }
        return creation;
    }

    /**
     * 获取用户的故事创作列表
     */
    public List<StoryCreation> myStories() {
        Long familyId = SecurityUtil.getUserId();
        return storyCreationMapper.selectList(
                new LambdaQueryWrapper<StoryCreation>()
                        .eq(StoryCreation::getFamilyId, familyId)
                        .orderByDesc(StoryCreation::getCreateTime));
    }

    // ============ 角色对话 ============

    /**
     * 创建或恢复对话会话
     */
    public ChatSession createOrResumeSession(Long roleId) {
        Long familyId = SecurityUtil.getUserId();

        // 查找是否有未结束的会话
        ChatSession existing = chatSessionMapper.selectOne(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getFamilyId, familyId)
                        .eq(ChatSession::getRoleId, roleId)
                        .eq(ChatSession::getStatus, "0")
                        .last("LIMIT 1"));

        if (existing != null) {
            return existing;
        }

        // 获取角色信息
        AiRole role = aiRoleService.getById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        // 创建新会话
        ChatSession session = new ChatSession();
        session.setSessionNo("CS" + System.currentTimeMillis() + IdUtil.fastSimpleUUID().substring(0, 6));
        session.setFamilyId(familyId);
        session.setRoleId(roleId);
        session.setRoleName(role.getRoleName());
        session.setMessageCount(0);
        session.setStatus("0");
        session.setCreateBy(SecurityUtil.getUsername());
        session.setCreateTime(new Date());
        chatSessionMapper.insert(session);

        // 插入AI欢迎消息
        ChatMessage welcome = new ChatMessage();
        welcome.setSessionId(session.getSessionId());
        welcome.setSenderType("ai");
        welcome.setMessageType("text");
        welcome.setContent(role.getRoleIntro() != null && !role.getRoleIntro().isBlank()
                ? role.getRoleIntro() : "嗨！很高兴认识你，我们聊聊天吧！");
        welcome.setExceptionStatus("0");
        welcome.setCreateTime(new Date());
        chatMessageMapper.insert(welcome);

        session.setMessageCount(1);
        session.setLastMessage(welcome.getContent());
        chatSessionMapper.updateById(session);

        return session;
    }

    /**
     * 发送消息并获取AI回复
     */
    public Map<String, Object> sendMessage(SendMessageRequest request) {
        Long familyId = SecurityUtil.getUserId();
        ChatSession session;

        if (request.getSessionId() != null) {
            session = chatSessionMapper.selectById(request.getSessionId());
            if (session == null) {
                throw new BusinessException("会话不存在");
            }
        } else {
            session = createOrResumeSession(request.getRoleId());
        }

        // 保存用户消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(session.getSessionId());
        userMsg.setSenderType("user");
        userMsg.setMessageType(request.getMessageType() != null ? request.getMessageType() : "text");
        userMsg.setContent(request.getContent());
        userMsg.setMediaUrl(request.getMediaUrl());
        userMsg.setExceptionStatus("0");
        userMsg.setCreateTime(new Date());
        chatMessageMapper.insert(userMsg);

        // 生成AI回复（第一期Mock）
        String aiReply = generateAiReply(request.getContent(), session.getRoleId());

        // 保存AI消息
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(session.getSessionId());
        aiMsg.setSenderType("ai");
        aiMsg.setMessageType("text");
        aiMsg.setContent(aiReply);
        aiMsg.setModelName(defaultModel);
        aiMsg.setExceptionStatus("0");
        aiMsg.setCreateTime(new Date());
        chatMessageMapper.insert(aiMsg);

        // 更新会话
        session.setMessageCount(session.getMessageCount() + 2);
        session.setLastMessage(aiReply.length() > 100 ? aiReply.substring(0, 100) : aiReply);
        session.setUpdateBy(SecurityUtil.getUsername());
        session.setUpdateTime(new Date());
        chatSessionMapper.updateById(session);

        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getSessionId());
        result.put("userMessage", userMsg);
        result.put("aiMessage", aiMsg);
        return result;
    }

    /**
     * 获取会话的消息列表
     */
    public List<ChatMessage> getMessages(Long sessionId) {
        return chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreateTime));
    }

    /**
     * 获取用户的会话列表
     */
    public List<ChatSession> mySessions() {
        Long familyId = SecurityUtil.getUserId();
        return chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getFamilyId, familyId)
                        .orderByDesc(ChatSession::getCreateTime));
    }

    // ============ AI 生成方法（阿里云百炼 DashScope） ============

    /**
     * 生成故事内容
     * provider=dashscope 时调用真实 API，否则降级为 Mock
     */
    private String generateStoryContent(StoryCreateRequest request) {
        String role = request.getRoleName() != null ? request.getRoleName() : "小狐狸";
        String input = request.getStoryInput();

        if ("dashscope".equals(llmProvider) && llmApiKey != null && !llmApiKey.isEmpty()) {
            try {
                String systemPrompt = "你是一个儿童绘本故事作家。请根据用户输入创作一个适合2-12岁儿童的故事，"
                    + "分为6页，每页一句话，语言简洁温暖有童趣。格式：第1页：...\\n第2页：... 以此类推。";
                String userPrompt = "角色：" + role + "，主题：" + input;
                String reply = callDashScopeLlm(systemPrompt, userPrompt);
                if (reply != null && !reply.isEmpty()) {
                    return reply;
                }
            } catch (Exception e) {
                log.error("调用 DashScope 生成故事失败，降级为 Mock", e);
            }
        }

        // Mock fallback
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(role).append("的故事冒险】\n\n");
        sb.append("第1页：在一个阳光明媚的早晨，").append(role).append("遇到了一个有趣的事情……\n");
        sb.append("第2页：").append(role).append("决定踏上一段新的旅程，关于").append(input).append("。\n");
        sb.append("第3页：路上，").append(role).append("遇到了需要帮助的朋友。\n");
        sb.append("第4页：他们一起想了一个好办法来解决问题。\n");
        sb.append("第5页：经过努力，他们终于达成了目标！\n");
        sb.append("第6页：").append(role).append("明白了一个道理：勇敢和善良是最棒的礼物。\n");
        sb.append("\n（AI生成的故事，适合2-12岁儿童，共6页）");
        return sb.toString();
    }

    /**
     * 生成角色对话回复
     * provider=dashscope 时调用真实 API，否则降级为 Mock
     */
    private String generateAiReply(String userMessage, Long roleId) {
        AiRole role = aiRoleService.getById(roleId);
        String roleName = role != null ? role.getRoleName() : "小芽";

        if ("dashscope".equals(llmProvider) && llmApiKey != null && !llmApiKey.isEmpty()) {
            try {
                String systemPrompt = "你是" + roleName + "，一个陪伴2-12岁儿童的绘本角色。"
                    + "回复要简短（不超过50字）、温暖、有童趣，用第一人称说话。"
                    + "如果孩子说的内容不明确，可以引导孩子继续聊。"
                    + "绝对不要出现任何不适宜儿童的内容。";
                String reply = callDashScopeLlm(systemPrompt, userMessage);
                if (reply != null && !reply.isEmpty()) {
                    return reply;
                }
            } catch (Exception e) {
                log.error("调用 DashScope 生成回复失败，降级为 Mock", e);
            }
        }

        // Mock fallback
        return mockAiReply(userMessage, roleName);
    }

    /**
     * 调用阿里云百炼 DashScope LLM API（OpenAI 兼容格式）
     */
    private String callDashScopeLlm(String systemPrompt, String userMessage) {
        try {
            URL url = new URL(llmApiUrl + "/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + llmApiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setDoOutput(true);

            // 构造请求 JSON（手动拼接，避免引入额外 JSON 依赖）
            String body = "{\"model\":\"" + llmModel + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + escapeJson(systemPrompt) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + escapeJson(userMessage) + "\"}"
                + "],"
                + "\"temperature\":" + llmTemperature + ","
                + "\"max_tokens\":" + llmMaxToken + "}";

            conn.getOutputStream().write(body.getBytes("UTF-8"));

            int code = conn.getResponseCode();
            String responseStr;
            if (code == 200) {
                responseStr = new String(conn.getInputStream().readAllBytes(), "UTF-8");
            } else {
                responseStr = new String(conn.getErrorStream().readAllBytes(), "UTF-8");
                log.error("DashScope API 错误: {} - {}", code, responseStr);
                return null;
            }

            // 解析响应 JSON，提取 choices[0].message.content
            return parseContentFromResponse(responseStr);
        } catch (Exception e) {
            log.error("DashScope LLM 调用异常", e);
            return null;
        }
    }

    /**
     * 从 DashScope 响应中提取回复内容
     */
    private String parseContentFromResponse(String json) {
        // 简单 JSON 解析，提取 "content":"xxx" 的值
        // 支持 \n 等转义
        int idx = json.indexOf("\"content\":\"");
        if (idx < 0) return null;
        int start = idx + "\"content\":\"".length();
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    default: sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    /**
     * JSON 字符串转义
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Mock 回复（降级方案）
     */
    private String mockAiReply(String userMessage, String roleName) {
        if (userMessage.contains("你好") || userMessage.contains("hi") || userMessage.contains("嗨")) {
            return "嗨！我是" + roleName + "，很高兴认识你！你想和我聊些什么呢？";
        }
        if (userMessage.contains("故事") || userMessage.contains("冒险")) {
            return "听起来真有趣！我们一起出发吧，你想去森林、海底还是云朵上呢？";
        }
        if (userMessage.contains("害怕") || userMessage.contains("不敢")) {
            return "没关系的，勇敢不是不害怕，而是害怕了也愿意试一试。我陪着你呢！";
        }
        if (userMessage.contains("开心") || userMessage.contains("高兴")) {
            return "太棒了！我也跟着你一起开心！把你开心的事讲给我听听吧。";
        }
        if (userMessage.contains("?") || userMessage.contains("？") || userMessage.contains("为什么")) {
            return "这是一个好问题！让我想想……你觉得答案会是什么呢？";
        }
        String[] defaultReplies = {
            "听起来真有趣！我们一起出发吧。",
            "嗯嗯，我懂你的意思，继续说说看！",
            "好的呀，那我们一起想想办法吧！",
            "哇，这个主意不错呢，我也想试试！",
            "你的想法很棒，让我们一起去看看吧！"
        };
        return defaultReplies[new Random().nextInt(defaultReplies.length)];
    }
}
