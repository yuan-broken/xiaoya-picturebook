package com.picturebook.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picturebook.common.exception.BusinessException;
import com.picturebook.user.domain.Family;
import com.picturebook.user.domain.MemberOrder;
import com.picturebook.user.mapper.FamilyMapper;
import com.picturebook.user.mapper.MemberOrderMapper;
import com.picturebook.user.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 订单与支付服务实现
 * 第一期支付渠道未接入真实网关，payOrder 标记为「后台手动开通」。
 * TODO: Agent-5 接入真实微信/支付宝支付 SDK 后替换。
 *
 * @author Agent-4
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private MemberOrderMapper memberOrderMapper;

    @Autowired
    private FamilyMapper familyMapper;

    // 套餐配置：planType → {amount, days, name}
    private static final Map<String, int[]> PLANS = new HashMap<>();
    private static final Map<String, String> PLAN_NAMES = new HashMap<>();
    private static final Map<String, BigDecimal> PLAN_AMOUNTS = new HashMap<>();
    static {
        // 月卡 ¥29 / 30 天
        PLAN_AMOUNTS.put("monthly", new BigDecimal("29.00"));
        PLANS.put("monthly", new int[]{0, 30});
        PLAN_NAMES.put("monthly", "月卡");
        // 季卡 ¥79 / 90 天
        PLAN_AMOUNTS.put("quarterly", new BigDecimal("79.00"));
        PLANS.put("quarterly", new int[]{0, 90});
        PLAN_NAMES.put("quarterly", "季卡");
        // 年卡 ¥199 / 365 天
        PLAN_AMOUNTS.put("yearly", new BigDecimal("199.00"));
        PLANS.put("yearly", new int[]{0, 365});
        PLAN_NAMES.put("yearly", "年卡");
    }
    // 试用 7 天
    private static final int TRIAL_DAYS = 7;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberOrder startTrial(Long familyId) {
        Family family = familyMapper.selectById(familyId);
        if (family == null) {
            throw new BusinessException("家庭不存在");
        }
        // 同一家庭仅可试用一次
        Long existTrials = memberOrderMapper.selectCount(new LambdaQueryWrapper<MemberOrder>()
                .eq(MemberOrder::getFamilyId, familyId)
                .eq(MemberOrder::getPlanType, "trial")
                .eq(MemberOrder::getDelFlag, "0"));
        if (existTrials != null && existTrials > 0) {
            throw new BusinessException("您已开通过 7 天试用，无法再次开通");
        }
        // 创建试用订单
        MemberOrder order = new MemberOrder();
        order.setFamilyId(familyId);
        order.setOrderNo(generateOrderNo("T"));
        order.setPlanType("trial");
        order.setAmount(BigDecimal.ZERO);
        order.setPaymentMethod("manual");
        order.setStartAt(new Date());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, TRIAL_DAYS);
        order.setEndAt(cal.getTime());
        order.setPaidAt(new Date());
        order.setRefundStatus("0");
        order.setStatus("1"); // 已支付（免费试用）
        order.setDelFlag("0");
        order.setCreateBy(family.getUsername());
        order.setCreateTime(new Date());
        memberOrderMapper.insert(order);

        // 同步家庭会员状态
        Family update = new Family();
        update.setFamilyId(familyId);
        update.setMemberLevel("growth");
        update.setMemberStatus("1");
        update.setMemberStartAt(order.getStartAt());
        update.setMemberEndAt(order.getEndAt());
        update.setUpdateBy(family.getUsername());
        update.setUpdateTime(new Date());
        familyMapper.updateById(update);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberOrder createOrder(Long familyId, String planType, String paymentMethod) {
        Family family = familyMapper.selectById(familyId);
        if (family == null) {
            throw new BusinessException("家庭不存在");
        }
        // 套餐校验，默认 yearly
        String plan = StringUtils.hasText(planType) ? planType : "yearly";
        if (!PLANS.containsKey(plan)) {
            throw new BusinessException("套餐类型不支持: " + planType);
        }
        int days = PLANS.get(plan)[1];
        BigDecimal amount = PLAN_AMOUNTS.get(plan);

        MemberOrder order = new MemberOrder();
        order.setFamilyId(familyId);
        order.setOrderNo(generateOrderNo("A"));
        order.setPlanType(plan);
        order.setAmount(amount);
        order.setPaymentMethod(StringUtils.hasText(paymentMethod) ? paymentMethod : "manual");
        order.setRefundStatus("0");
        order.setStatus("0"); // 待支付
        order.setDelFlag("0");
        order.setCreateBy(family.getUsername());
        order.setCreateTime(new Date());
        memberOrderMapper.insert(order);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long orderId, String paymentMethod) {
        MemberOrder order = memberOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"0".equals(order.getStatus())) {
            throw new BusinessException("订单状态不允许支付，当前状态: " + order.getStatus());
        }
        // TODO: 接入真实支付网关，回写 transactionId 等
        Family family = familyMapper.selectById(order.getFamilyId());

        MemberOrder update = new MemberOrder();
        update.setOrderId(orderId);
        update.setPaymentMethod(StringUtils.hasText(paymentMethod) ? paymentMethod : order.getPaymentMethod());
        update.setPaidAt(new Date());
        update.setStatus("1");
        // 按订单的 planType 取天数（月30/季90/年365）
        String plan = order.getPlanType() != null ? order.getPlanType() : "yearly";
        int days = PLANS.containsKey(plan) ? PLANS.get(plan)[1] : 365;
        // 计算订阅周期：从今日起 days 天；若家庭已有未到期会员，则从原到期日续起
        Date startAt = new Date();
        if (family.getMemberEndAt() != null && family.getMemberEndAt().after(startAt)) {
            startAt = family.getMemberEndAt();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(startAt);
        cal.add(Calendar.DAY_OF_MONTH, days);
        update.setStartAt(startAt);
        update.setEndAt(cal.getTime());
        update.setUpdateBy(family.getUsername());
        update.setUpdateTime(new Date());
        memberOrderMapper.updateById(update);

        // 同步家庭会员状态
        Family familyUpdate = new Family();
        familyUpdate.setFamilyId(family.getFamilyId());
        familyUpdate.setMemberLevel("growth");
        familyUpdate.setMemberStatus("1");
        familyUpdate.setMemberStartAt(startAt);
        familyUpdate.setMemberEndAt(update.getEndAt());
        familyUpdate.setUpdateBy(family.getUsername());
        familyUpdate.setUpdateTime(new Date());
        familyMapper.updateById(familyUpdate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refundOrder(Long orderId, String reason) {
        MemberOrder order = memberOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"1".equals(order.getStatus())) {
            throw new BusinessException("仅已支付订单可退款，当前状态: " + order.getStatus());
        }
        if ("2".equals(order.getRefundStatus())) {
            throw new BusinessException("订单已退款，不可重复退款");
        }
        // TODO: 对接支付平台原路退款（第一期仅标记状态）
        MemberOrder update = new MemberOrder();
        update.setOrderId(orderId);
        update.setRefundStatus("2");
        update.setRefundAt(new Date());
        update.setStatus("3");
        update.setRemark(reason);
        update.setUpdateBy("admin"); // TODO: 改为当前管理员账号
        update.setUpdateTime(new Date());
        memberOrderMapper.updateById(update);

        // 同步家庭会员状态：立即失效
        Family family = familyMapper.selectById(order.getFamilyId());
        if (family != null) {
            Family familyUpdate = new Family();
            familyUpdate.setFamilyId(family.getFamilyId());
            familyUpdate.setMemberLevel("free");
            familyUpdate.setMemberStatus("3");
            familyUpdate.setMemberEndAt(new Date());
            familyUpdate.setUpdateBy("admin");
            familyUpdate.setUpdateTime(new Date());
            familyMapper.updateById(familyUpdate);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberOrder renew(Long familyId, String planType, String paymentMethod) {
        MemberOrder order = createOrder(familyId, planType, paymentMethod);
        payOrder(order.getOrderId(), paymentMethod);
        return memberOrderMapper.selectById(order.getOrderId());
    }

    @Override
    public List<MemberOrder> listByFamily(Long familyId) {
        return memberOrderMapper.selectList(new LambdaQueryWrapper<MemberOrder>()
                .eq(MemberOrder::getFamilyId, familyId)
                .eq(MemberOrder::getDelFlag, "0")
                .orderByDesc(MemberOrder::getCreateTime));
    }

    @Override
    public MemberOrder getById(Long orderId) {
        MemberOrder order = memberOrderMapper.selectById(orderId);
        if (order == null || "2".equals(order.getDelFlag())) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    @Override
    public List<MemberOrder> listForAdmin(Map<String, Object> filter) {
        LambdaQueryWrapper<MemberOrder> wrapper = new LambdaQueryWrapper<MemberOrder>()
                .eq(MemberOrder::getDelFlag, "0")
                .orderByDesc(MemberOrder::getCreateTime);
        if (filter != null) {
            if (filter.get("familyId") != null) {
                wrapper.eq(MemberOrder::getFamilyId, Long.valueOf(filter.get("familyId").toString()));
            }
            if (filter.get("planType") != null) {
                wrapper.eq(MemberOrder::getPlanType, filter.get("planType").toString());
            }
            if (filter.get("status") != null) {
                wrapper.eq(MemberOrder::getStatus, filter.get("status").toString());
            }
            if (filter.get("refundStatus") != null) {
                wrapper.eq(MemberOrder::getRefundStatus, filter.get("refundStatus").toString());
            }
        }
        return memberOrderMapper.selectList(wrapper);
    }

    /**
     * 生成订单号：前缀 + yyyyMMddHHmmss + 4位随机
     */
    private String generateOrderNo(String prefix) {
        return prefix + new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + (int) (Math.random() * 9000 + 1000);
    }
}
