package com.picturebook.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picturebook.common.exception.BusinessException;
import com.picturebook.user.domain.Family;
import com.picturebook.user.domain.MemberOrder;
import com.picturebook.user.mapper.FamilyMapper;
import com.picturebook.user.mapper.MemberOrderMapper;
import com.picturebook.user.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 订单与试用服务单元测试
 * 覆盖：7天试用一次限制、创建订单、支付、退款
 *
 * @author Test-Agent
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("订单与试用服务测试")
class OrderServiceTrialTest {

    @Mock
    private MemberOrderMapper memberOrderMapper;

    @Mock
    private FamilyMapper familyMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    // ==================== 试用限制测试 ====================

    @Test
    @DisplayName("试用 - 首次开通成功")
    void startTrial_firstTime_success() {
        // given
        Long familyId = 2001L;
        Family family = new Family();
        family.setFamilyId(familyId);
        family.setUsername("trial_user");
        family.setMemberLevel("free");
        when(familyMapper.selectById(familyId)).thenReturn(family);
        when(memberOrderMapper.selectCount(any())).thenReturn(0L);

        // when
        MemberOrder order = orderService.startTrial(familyId);

        // then
        assertNotNull(order);
        assertEquals("trial", order.getPlanType());
        assertEquals(BigDecimal.ZERO, order.getAmount());
        assertEquals("1", order.getStatus());
        assertNotNull(order.getStartAt());
        assertNotNull(order.getEndAt());
        // 验证 endAt 是 7 天后
        long diffDays = (order.getEndAt().getTime() - order.getStartAt().getTime()) / (1000 * 60 * 60 * 24);
        assertEquals(7, diffDays, 1);
        verify(memberOrderMapper).insert(any(MemberOrder.class));
        verify(familyMapper).updateById(any(Family.class));
    }

    @Test
    @DisplayName("试用 - 第二次应被拒绝")
    void startTrial_secondTime_rejected() {
        // given
        Long familyId = 2001L;
        Family family = new Family();
        family.setFamilyId(familyId);
        family.setUsername("trial_user");
        when(familyMapper.selectById(familyId)).thenReturn(family);
        when(memberOrderMapper.selectCount(any())).thenReturn(1L);

        // when & then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.startTrial(familyId));
        assertTrue(ex.getMessage().contains("已开通过"));
        verify(memberOrderMapper, never()).insert(any());
    }

    @Test
    @DisplayName("试用 - 家庭不存在应抛异常")
    void startTrial_familyNotFound() {
        when(familyMapper.selectById(9999L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.startTrial(9999L));
        assertTrue(ex.getMessage().contains("家庭不存在"));
    }

    // ==================== 创建订单测试 ====================

    @Test
    @DisplayName("创建订单 - 季卡金额应为79")
    void createOrder_quarterlyAmount() {
        Family family = new Family();
        family.setFamilyId(2001L);
        family.setUsername("buyer");
        when(familyMapper.selectById(2001L)).thenReturn(family);

        MemberOrder order = orderService.createOrder(2001L, "quarterly", "alipay");
        assertEquals("quarterly", order.getPlanType());
        assertEquals(new BigDecimal("79.00"), order.getAmount());
        assertEquals("alipay", order.getPaymentMethod());
        assertEquals("0", order.getStatus());
    }

    @Test
    @DisplayName("创建订单 - 月卡金额应为29")
    void createOrder_monthlyAmount() {
        Family family = new Family();
        family.setFamilyId(2001L);
        family.setUsername("buyer");
        when(familyMapper.selectById(2001L)).thenReturn(family);

        MemberOrder order = orderService.createOrder(2001L, "monthly", "wxpay");
        assertEquals(new BigDecimal("29.00"), order.getAmount());
    }

    @Test
    @DisplayName("创建订单 - 年卡金额应为199")
    void createOrder_yearlyAmount() {
        Family family = new Family();
        family.setFamilyId(2001L);
        family.setUsername("buyer");
        when(familyMapper.selectById(2001L)).thenReturn(family);

        MemberOrder order = orderService.createOrder(2001L, "yearly", "manual");
        assertEquals(new BigDecimal("199.00"), order.getAmount());
    }

    @Test
    @DisplayName("创建订单 - 不支持的套餐应抛异常")
    void createOrder_unsupportedPlan() {
        Family family = new Family();
        family.setFamilyId(2001L);
        when(familyMapper.selectById(2001L)).thenReturn(family);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.createOrder(2001L, "lifetime", "alipay"));
        assertTrue(ex.getMessage().contains("套餐类型不支持"));
    }

    // ==================== 支付测试 ====================

    @Test
    @DisplayName("支付 - 成功后状态应为1")
    void payOrder_success() {
        MemberOrder order = new MemberOrder();
        order.setOrderId(3001L);
        order.setFamilyId(2001L);
        order.setPlanType("quarterly");
        order.setStatus("0");
        when(memberOrderMapper.selectById(3001L)).thenReturn(order);

        Family family = new Family();
        family.setFamilyId(2001L);
        family.setUsername("buyer");
        family.setMemberEndAt(null);
        when(familyMapper.selectById(2001L)).thenReturn(family);

        orderService.payOrder(3001L, "alipay");

        verify(memberOrderMapper).updateById(argThat(u -> "1".equals(u.getStatus())));
        verify(familyMapper).updateById(argThat(f -> "growth".equals(f.getMemberLevel())));
    }

    @Test
    @DisplayName("支付 - 订单不存在")
    void payOrder_notFound() {
        when(memberOrderMapper.selectById(9999L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.payOrder(9999L, "alipay"));
        assertTrue(ex.getMessage().contains("订单不存在"));
    }

    @Test
    @DisplayName("支付 - 重复支付应被拒")
    void payOrder_alreadyPaid() {
        MemberOrder order = new MemberOrder();
        order.setOrderId(3001L);
        order.setStatus("1");
        when(memberOrderMapper.selectById(3001L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.payOrder(3001L, "alipay"));
        assertTrue(ex.getMessage().contains("不允许支付"));
    }

    // ==================== 退款测试 ====================

    @Test
    @DisplayName("退款 - 已支付订单可退款")
    void refundOrder_success() {
        MemberOrder order = new MemberOrder();
        order.setOrderId(3001L);
        order.setFamilyId(2001L);
        order.setStatus("1");
        order.setRefundStatus("0");
        when(memberOrderMapper.selectById(3001L)).thenReturn(order);

        Family family = new Family();
        family.setFamilyId(2001L);
        when(familyMapper.selectById(2001L)).thenReturn(family);

        orderService.refundOrder(3001L, "测试退款");
        verify(memberOrderMapper).updateById(argThat(u -> "3".equals(u.getStatus()) && "2".equals(u.getRefundStatus())));
        verify(familyMapper).updateById(argThat(f -> "free".equals(f.getMemberLevel())));
    }

    @Test
    @DisplayName("退款 - 重复退款应被拒")
    void refundOrder_alreadyRefunded() {
        MemberOrder order = new MemberOrder();
        order.setOrderId(3001L);
        order.setStatus("1");
        order.setRefundStatus("2");
        when(memberOrderMapper.selectById(3001L)).thenReturn(order);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.refundOrder(3001L, "再次退款"));
        assertTrue(ex.getMessage().contains("已退款"));
    }
}
