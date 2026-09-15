package com.picturebook.user.service;

import com.picturebook.user.domain.MemberOrder;

import java.util.List;
import java.util.Map;

/**
 * 订单与支付服务接口
 * 对应 PRD M39：订阅 / 支付 / 退款 / 试用 / 续费。
 *
 * @author Agent-4
 */
public interface OrderService {

    /**
     * 开通 7 天试用
     * 同一家庭仅可试用一次；试用期间 member_level 临时升为 growth。
     *
     * @param familyId 家庭ID
     * @return 订单信息
     */
    MemberOrder startTrial(Long familyId);

    /**
     * 创建订阅订单
     * 支持三档套餐：monthly(¥29/30天) / quarterly(¥79/90天) / yearly(¥199/365天)
     *
     * @param familyId      家庭ID
     * @param planType      套餐类型 monthly/quarterly/yearly（空则默认 yearly）
     * @param paymentMethod wechat / alipay / manual
     * @return 待支付订单
     */
    MemberOrder createOrder(Long familyId, String planType, String paymentMethod);

    /**
     * 支付订单
     *
     * @param orderId       订单ID
     * @param paymentMethod 实际支付方式
     */
    void payOrder(Long orderId, String paymentMethod);

    /**
     * 退款
     *
     * @param orderId 订单ID
     * @param reason  退款原因
     */
    void refundOrder(Long orderId, String reason);

    /**
     * 续费（在已有订单到期前/后再次下单，复用 createOrder + payOrder）
     * 默认实现：调用 createOrder 后立即标记为已支付（手动开通）。
     *
     * @param familyId      家庭ID
     * @param planType      套餐类型 monthly/quarterly/yearly
     * @param paymentMethod 支付方式
     */
    MemberOrder renew(Long familyId, String planType, String paymentMethod);

    /**
     * 查询家庭订单列表
     *
     * @param familyId 家庭ID（用户端使用）
     * @return 订单列表
     */
    List<MemberOrder> listByFamily(Long familyId);

    /**
     * 查询订单详情
     */
    MemberOrder getById(Long orderId);

    /**
     * 管理端：分页查询所有订单
     *
     * @param filter 过滤条件：familyId/memberStatus/planType/refundStatus
     * @return 订单列表
     */
    List<MemberOrder> listForAdmin(Map<String, Object> filter);
}
