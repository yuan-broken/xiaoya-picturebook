package com.picturebook.ai.service;

import cn.hutool.core.util.IdUtil;
import com.picturebook.ai.domain.InteractionRecord;
import com.picturebook.ai.domain.ReadingTask;
import com.picturebook.ai.dto.InteractionResponseRequest;
import com.picturebook.ai.dto.ReadingSession;
import com.picturebook.ai.dto.ReadingTaskCreateRequest;
import com.picturebook.ai.mapper.InteractionRecordMapper;
import com.picturebook.ai.mapper.ReadingTaskMapper;
import com.picturebook.book.domain.Book;
import com.picturebook.book.domain.BookPage;
import com.picturebook.book.domain.InteractionPoint;
import com.picturebook.book.service.BookService;
import com.picturebook.common.constant.CacheConstants;
import com.picturebook.common.enums.ReadingStateEnum;
import com.picturebook.common.exception.BusinessException;
import com.picturebook.common.utils.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 讲读编排服务（核心枢纽）
 * 维护会话状态机，编排AI能力调用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingOrchestratorService {

    private final ReadingTaskMapper readingTaskMapper;
    private final InteractionRecordMapper interactionRecordMapper;
    private final BookService bookService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${picturebook.ai.enabled:false}")
    private boolean aiEnabled;

    @Value("${picturebook.ai.default-reading-style:gentle}")
    private String defaultReadingStyle;

    @Value("${picturebook.ai.default-tts-voice:gentle-female}")
    private String defaultTtsVoice;

    @Value("${picturebook.ai.interaction-timeout:15}")
    private int interactionTimeout;

    @Value("${picturebook.ai.min-wait-seconds:5}")
    private int minWaitSeconds;

    @Value("${picturebook.ai.max-wait-seconds:15}")
    private int maxWaitSeconds;

    /**
     * 创建讲读任务，初始化会话
     */
    public ReadingSession createTask(ReadingTaskCreateRequest request) {
        Long familyId = SecurityUtil.getUserId();
        Book book = bookService.getBookDetail(request.getBookId());

        // 创建任务记录
        ReadingTask task = new ReadingTask();
        task.setTaskNo("RT" + System.currentTimeMillis() + IdUtil.fastSimpleUUID().substring(0, 6));
        task.setFamilyId(familyId);
        task.setChildId(request.getChildId());
        task.setBookId(book.getBookId());
        task.setBookTitle(book.getTitle());
        task.setCurrentPage(1);
        task.setTotalPages(book.getTotalPages());
        task.setReadingState(ReadingStateEnum.LOADING.getCode());
        task.setReadingStyle(request.getReadingStyle() != null ? request.getReadingStyle() : defaultReadingStyle);
        task.setVoiceConfig(request.getVoiceConfig() != null ? request.getVoiceConfig() : defaultTtsVoice);
        task.setInteractionEnabled(request.getInteractionEnabled() != null ? request.getInteractionEnabled() : "Y");
        task.setSubtitleEnabled(request.getSubtitleEnabled() != null ? request.getSubtitleEnabled() : "Y");
        task.setTotalDuration(0);
        task.setStartTime(new Date());
        task.setCreateBy(SecurityUtil.getUsername());
        task.setCreateTime(new Date());
        readingTaskMapper.insert(task);

        // 创建会话上下文并缓存
        ReadingSession session = buildSession(task);
        cacheSession(session);

        log.info("讲读任务创建成功：taskId={}, bookId={}", task.getTaskId(), book.getBookId());
        return session;
    }

    /**
     * 启动讲读（加载第一页，进入讲读状态）
     */
    public ReadingSession startReading(Long taskId) {
        ReadingSession session = getSession(taskId);
        if (session == null) {
            throw new BusinessException("讲读会话不存在或已过期");
        }

        // 加载当前页内容
        List<BookPage> pages = bookService.getBookPages(session.getBookId());
        if (pages.isEmpty()) {
            session.setState(ReadingStateEnum.ERROR.getCode());
            updateTaskState(taskId, ReadingStateEnum.ERROR.getCode(), "绘本页面为空");
            cacheSession(session);
            throw new BusinessException("绘本页面为空");
        }

        session.setTotalPages(pages.size());
        session.setState(ReadingStateEnum.READING.getCode());
        session.setIsPlaying(true);
        session.setStartTimestamp(System.currentTimeMillis());
        session.setCurrentPageInteractionsDone(false);
        session.setCompletedPointIds(new ArrayList<>());

        updateTaskState(taskId, ReadingStateEnum.READING.getCode(), null);
        cacheSession(session);

        log.info("讲读启动：taskId={}, currentPage={}", taskId, session.getCurrentPage());
        return session;
    }

    /**
     * 暂停讲读
     */
    public ReadingSession pause(Long taskId) {
        ReadingSession session = getSession(taskId);
        if (session == null) {
            throw new BusinessException("讲读会话不存在");
        }
        session.setIsPlaying(false);
        session.setState(ReadingStateEnum.PAUSED.getCode());
        updateTaskState(taskId, ReadingStateEnum.PAUSED.getCode(), null);
        cacheSession(session);
        return session;
    }

    /**
     * 继续讲读
     */
    public ReadingSession resume(Long taskId) {
        ReadingSession session = getSession(taskId);
        if (session == null) {
            throw new BusinessException("讲读会话不存在");
        }
        session.setIsPlaying(true);
        session.setState(ReadingStateEnum.READING.getCode());
        updateTaskState(taskId, ReadingStateEnum.READING.getCode(), null);
        cacheSession(session);
        return session;
    }

    /**
     * 下一页
     */
    public ReadingSession nextPage(Long taskId) {
        ReadingSession session = getSession(taskId);
        if (session == null) {
            throw new BusinessException("讲读会话不存在");
        }
        if (session.getCurrentPage() >= session.getTotalPages()) {
            // 已到最后一页，讲读完成
            session.setState(ReadingStateEnum.FINISHED.getCode());
            session.setIsPlaying(false);
            updateTaskState(taskId, ReadingStateEnum.FINISHED.getCode(), null);
            updateTaskEndTime(taskId);
        } else {
            session.setCurrentPage(session.getCurrentPage() + 1);
            session.setCurrentPageInteractionsDone(false);
            session.setCompletedPointIds(new ArrayList<>());
            session.setState(ReadingStateEnum.READING.getCode());
            updateTaskPage(taskId, session.getCurrentPage(), ReadingStateEnum.READING.getCode());
        }
        cacheSession(session);
        return session;
    }

    /**
     * 上一页
     */
    public ReadingSession prevPage(Long taskId) {
        ReadingSession session = getSession(taskId);
        if (session == null) {
            throw new BusinessException("讲读会话不存在");
        }
        if (session.getCurrentPage() <= 1) {
            throw new BusinessException("已是第一页");
        }
        session.setCurrentPage(session.getCurrentPage() - 1);
        session.setCurrentPageInteractionsDone(false);
        session.setCompletedPointIds(new ArrayList<>());
        session.setState(ReadingStateEnum.READING.getCode());
        updateTaskPage(taskId, session.getCurrentPage(), ReadingStateEnum.READING.getCode());
        cacheSession(session);
        return session;
    }

    /**
     * 跳转到指定页
     */
    public ReadingSession gotoPage(Long taskId, Integer pageNum) {
        ReadingSession session = getSession(taskId);
        if (session == null) {
            throw new BusinessException("讲读会话不存在");
        }
        if (pageNum < 1 || pageNum > session.getTotalPages()) {
            throw new BusinessException("页码超出范围");
        }
        session.setCurrentPage(pageNum);
        session.setCurrentPageInteractionsDone(false);
        session.setCompletedPointIds(new ArrayList<>());
        session.setState(ReadingStateEnum.READING.getCode());
        updateTaskPage(taskId, pageNum, ReadingStateEnum.READING.getCode());
        cacheSession(session);
        return session;
    }

    /**
     * 触发互动点（根据当前页配置）
     * 返回互动提问内容
     */
    public List<InteractionPoint> triggerInteractions(Long taskId) {
        ReadingSession session = getSession(taskId);
        if (session == null) {
            throw new BusinessException("讲读会话不存在");
        }

        // 获取当前页的互动点
        List<BookPage> pages = bookService.getBookPages(session.getBookId());
        BookPage currentPage = pages.stream()
                .filter(p -> p.getPageNum().equals(session.getCurrentPage()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("当前页不存在"));

        List<InteractionPoint> points = bookService.getPageInteractionPoints(currentPage.getPageId());

        // 过滤已完成的互动点
        List<InteractionPoint> pending = points.stream()
                .filter(p -> !session.getCompletedPointIds().contains(p.getPointId()))
                .toList();

        if (!pending.isEmpty()) {
            session.setState(ReadingStateEnum.INTERACTIVE.getCode());
            updateTaskState(taskId, ReadingStateEnum.INTERACTIVE.getCode(), null);
            cacheSession(session);
        }

        return pending;
    }

    /**
     * 提交互动回应
     * 返回AI反馈（鼓励或引导重答）
     */
    public Map<String, Object> submitInteraction(InteractionResponseRequest request) {
        ReadingSession session = getSession(request.getTaskId());
        if (session == null) {
            throw new BusinessException("讲读会话不存在");
        }

        InteractionPoint point = bookService.getBookInteractionPoints(session.getBookId()).stream()
                .filter(p -> p.getPointId().equals(request.getPointId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("互动点不存在"));

        // 生成反馈（第一期使用Mock：永远正向鼓励）
        String feedbackType = "encourage";
        String feedbackText = point.getEncourageText() != null && !point.getEncourageText().isBlank()
                ? point.getEncourageText()
                : "小芽听见啦！你的想法很有趣，继续吧！";

        // 记录互动
        InteractionRecord record = new InteractionRecord();
        record.setTaskId(request.getTaskId());
        record.setBookId(session.getBookId());
        record.setPageNum(request.getPageNum());
        record.setPointId(request.getPointId());
        record.setQuestion(point.getQuestion());
        record.setResponseType(request.getResponseType() != null ? request.getResponseType() : "click");
        record.setChildResponse(request.getChildResponse());
        record.setFeedbackType(feedbackType);
        record.setFeedbackText(feedbackText);
        record.setResponseTime(calculateResponseTime(session));
        record.setCreateTime(new Date());
        interactionRecordMapper.insert(record);

        // 更新会话：标记该互动点已完成
        if (session.getCompletedPointIds() == null) {
            session.setCompletedPointIds(new ArrayList<>());
        }
        session.getCompletedPointIds().add(request.getPointId());

        // 检查当前页互动点是否全部完成
        List<InteractionPoint> allPoints = bookService.getPageInteractionPoints(
                session.getBookId(), request.getPageNum());
        boolean allDone = allPoints.stream()
                .allMatch(p -> session.getCompletedPointIds().contains(p.getPointId()));
        session.setCurrentPageInteractionsDone(allDone);

        // 互动完成后回到讲读状态
        if (allDone) {
            session.setState(ReadingStateEnum.READING.getCode());
            updateTaskState(request.getTaskId(), ReadingStateEnum.READING.getCode(), null);
        }
        cacheSession(session);

        Map<String, Object> result = new HashMap<>();
        result.put("feedbackType", feedbackType);
        result.put("feedbackText", feedbackText);
        result.put("allInteractionsDone", allDone);
        result.put("recordId", record.getRecordId());
        return result;
    }

    /**
     * 获取会话状态
     */
    public ReadingSession getSession(Long taskId) {
        String key = CacheConstants.READING_SESSION_KEY + taskId;
        Object obj = redisTemplate.opsForValue().get(key);
        if (obj instanceof ReadingSession) {
            return (ReadingSession) obj;
        }
        // 从数据库恢复
        ReadingTask task = readingTaskMapper.selectById(taskId);
        if (task == null) {
            return null;
        }
        ReadingSession session = buildSession(task);
        cacheSession(session);
        return session;
    }

    // ============ 私有方法 ============

    private ReadingSession buildSession(ReadingTask task) {
        ReadingSession session = new ReadingSession();
        session.setTaskId(task.getTaskId());
        session.setTaskNo(task.getTaskNo());
        session.setFamilyId(task.getFamilyId());
        session.setChildId(task.getChildId());
        session.setBookId(task.getBookId());
        session.setBookTitle(task.getBookTitle());
        session.setTotalPages(task.getTotalPages());
        session.setCurrentPage(task.getCurrentPage());
        session.setState(task.getReadingState());
        session.setReadingStyle(task.getReadingStyle());
        session.setVoiceConfig(task.getVoiceConfig());
        session.setInteractionEnabled("Y".equals(task.getInteractionEnabled()));
        session.setSubtitleEnabled("Y".equals(task.getSubtitleEnabled()));
        session.setCurrentPageInteractionsDone(false);
        session.setCompletedPointIds(new ArrayList<>());
        session.setIsPlaying(false);
        session.setTotalDuration(task.getTotalDuration());
        return session;
    }

    private void cacheSession(ReadingSession session) {
        String key = CacheConstants.READING_SESSION_KEY + session.getTaskId();
        redisTemplate.opsForValue().set(key, session, 24, TimeUnit.HOURS);
    }

    private void updateTaskState(Long taskId, String state, String errorMsg) {
        ReadingTask update = new ReadingTask();
        update.setTaskId(taskId);
        update.setReadingState(state);
        if (errorMsg != null) {
            update.setErrorMsg(errorMsg);
        }
        update.setUpdateBy(SecurityUtil.getUsername());
        update.setUpdateTime(new Date());
        readingTaskMapper.updateById(update);
    }

    private void updateTaskPage(Long taskId, Integer pageNum, String state) {
        ReadingTask update = new ReadingTask();
        update.setTaskId(taskId);
        update.setCurrentPage(pageNum);
        update.setReadingState(state);
        update.setUpdateBy(SecurityUtil.getUsername());
        update.setUpdateTime(new Date());
        readingTaskMapper.updateById(update);
    }

    private void updateTaskEndTime(Long taskId) {
        ReadingTask update = new ReadingTask();
        update.setTaskId(taskId);
        update.setEndTime(new Date());
        update.setUpdateBy(SecurityUtil.getUsername());
        update.setUpdateTime(new Date());
        readingTaskMapper.updateById(update);
    }

    private int calculateResponseTime(ReadingSession session) {
        if (session.getStartTimestamp() != null) {
            return Math.toIntExact((System.currentTimeMillis() - session.getStartTimestamp()) / 1000);
        }
        return 0;
    }

    /**
     * 节奏自适应：根据孩子响应时长动态调整等待时间
     */
    public int getAdaptiveWaitTime(int responseTimeSeconds) {
        if (responseTimeSeconds < 5) {
            // 响应快，节奏更紧凑
            return minWaitSeconds;
        } else if (responseTimeSeconds > 12) {
            // 响应慢，给更多思考时间
            return maxWaitSeconds;
        }
        return (minWaitSeconds + maxWaitSeconds) / 2;
    }
}
