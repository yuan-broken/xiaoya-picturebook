package com.picturebook.user.controller;

import com.picturebook.common.core.Result;
import com.picturebook.user.domain.Family;
import com.picturebook.user.domain.MemberOrder;
import com.picturebook.user.mapper.FamilyMapper;
import com.picturebook.user.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 订单与支付 Controller（用户端）
 * 对应 PRD M39 / M05 会员升级。
 * 用户端接口前缀 /api/order、/api/user/trial。
 *
 * @author Agent-4
 */
@RestController
@RequestMapping("/api")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private FamilyMapper familyMapper;

    /**
     * 开通 7 天试用
     * POST /api/user/trial
     */
    @PostMapping("/user/trial")
    public Result<MemberOrder> startTrial() {
        return Result.ok(orderService.startTrial(currentFamilyId()));
    }

    /**
     * 创建订阅订单
     * POST /api/order
     * body: {planType: monthly|quarterly|yearly, paymentMethod: wechat|alipay|manual}
     */
    @PostMapping("/order")
    public Result<MemberOrder> createOrder(@RequestBody Map<String, String> body) {
        String planType = body.get("planType");
        String paymentMethod = body.get("paymentMethod");
        return Result.ok(orderService.createOrder(currentFamilyId(), planType, paymentMethod));
    }

    /**
     * 支付订单（Mock 模拟支付：点击立即支付后直接成功）
     * POST /api/order/{id}/pay
     * body: {paymentMethod}
     */
    @PostMapping("/order/{id}/pay")
    public Result<Void> payOrder(@PathVariable("id") Long orderId, @RequestBody Map<String, String> body) {
        orderService.payOrder(orderId, body.get("paymentMethod"));
        return Result.ok();
    }

    /**
     * 续费（手动开通，常用于后台运营为家长开通）
     * POST /api/order/renew
     * body: {planType, paymentMethod}
     */
    @PostMapping("/order/renew")
    public Result<MemberOrder> renew(@RequestBody Map<String, String> body) {
        return Result.ok(orderService.renew(currentFamilyId(), body.get("planType"), body.get("paymentMethod")));
    }

    /**
     * 查询当前家庭订单列表
     * GET /api/order/list
     */
    @GetMapping("/order/list")
    public Result<List<MemberOrder>> listByFamily() {
        return Result.ok(orderService.listByFamily(currentFamilyId()));
    }

    /**
     * 查询订单详情
     * GET /api/order/{id}
     */
    @GetMapping("/order/{id}")
    public Result<MemberOrder> getById(@PathVariable("id") Long orderId) {
        return Result.ok(orderService.getById(orderId));
    }

    /**
     * 申请退款
     * POST /api/order/{id}/refund
     * body: {reason}
     */
    @PostMapping("/order/{id}/refund")
    public Result<Void> refund(@PathVariable("id") Long orderId, @RequestBody Map<String, String> body) {
        orderService.refundOrder(orderId, body.get("reason"));
        return Result.ok();
    }

    /**
     * 会员信息查询（home.html/upgrade.html 升级按钮的依据）
     * GET /api/user/member
     * 返回 {level, status, expireTime, daysRemaining}
     */
    @GetMapping("/user/member")
    public Result<Map<String, Object>> memberInfo() {
        Long familyId = currentFamilyId();
        Family family = familyMapper.selectById(familyId);
        Map<String, Object> r = new HashMap<>();
        if (family == null) {
            r.put("level", "free");
            r.put("status", "0");
            r.put("expireTime", null);
            r.put("daysRemaining", 0);
            return Result.ok(r);
        }
        String level = family.getMemberLevel() != null ? family.getMemberLevel() : "free";
        String status = family.getMemberStatus() != null ? family.getMemberStatus() : "0";
        Date expire = family.getMemberEndAt();
        long daysRemaining = 0;
        if (expire != null && expire.after(new Date())) {
            daysRemaining = TimeUnit.DAYS.convert(expire.getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }
        r.put("level", level);
        r.put("status", status);
        r.put("expireTime", expire);
        r.put("daysRemaining", daysRemaining);
        return Result.ok(r);
    }

    // ==================== 工具方法 ====================

    /**
     * 从 SecurityContext 获取当前家庭ID（参考 UserController 的实现）
     */
    private Long currentFamilyId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Long) {
            return (Long) auth.getDetails();
        }
        return 1L; // 兜底
    }
}
