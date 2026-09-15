package com.picturebook.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.picturebook.admin.domain.SysAdmin;
import com.picturebook.admin.mapper.SysAdminMapper;
import com.picturebook.common.core.Result;
import com.picturebook.common.utils.SecurityUtil;
import com.picturebook.user.domain.Family;
import com.picturebook.user.domain.MemberOrder;
import com.picturebook.user.mapper.FamilyMapper;
import com.picturebook.user.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 管理端：管理员与权限 + 会员与订单管理
 * 对应 PRD M21 管理员与权限 / M22 会员与订单管理。
 * 管理端接口前缀 /api/admin/user。
 *
 * @author Agent-4
 */
@RestController
@RequestMapping("/api/admin/user")
@PreAuthorize("hasAnyRole('SUPER','OPERATOR')")
public class AdminUserController {

    @Autowired
    private SysAdminMapper sysAdminMapper;

    @Autowired
    private FamilyMapper familyMapper;

    @Autowired
    private OrderService orderService;

    // ==================== 管理员账号 M21 ====================

    /**
     * 管理员列表
     */
    @PreAuthorize("@ss.hasPermi('admin:user:list')")
    @GetMapping("/admin")
    public Result<List<SysAdmin>> listAdmin() {
        List<SysAdmin> list = sysAdminMapper.selectList(new LambdaQueryWrapper<SysAdmin>()
                .eq(SysAdmin::getDelFlag, "0")
                .orderByDesc(SysAdmin::getAdminId));
        list.forEach(a -> a.setPassword(null));
        return Result.ok(list);
    }

    /**
     * 新增管理员
     */
    @PreAuthorize("@ss.hasPermi('admin:user:add')")
    @PostMapping("/admin")
    public Result<Long> addAdmin(@RequestBody SysAdmin admin) {
        Long exist = sysAdminMapper.selectCount(new LambdaQueryWrapper<SysAdmin>()
                .eq(SysAdmin::getUsername, admin.getUsername())
                .eq(SysAdmin::getDelFlag, "0"));
        if (exist != null && exist > 0) {
            return Result.fail("账号已存在");
        }
        if (admin.getPassword() == null || admin.getPassword().length() < 8) {
            return Result.fail("密码至少 8 位");
        }
        admin.setPassword(SecurityUtil.encryptPassword(admin.getPassword()));
        admin.setRole("operator".equals(admin.getRole()) ? "operator" : "super");
        admin.setStatus("0");
        admin.setDelFlag("0");
        admin.setCreateBy(currentAdminName());
        admin.setCreateTime(new Date());
        sysAdminMapper.insert(admin);
        return Result.ok(admin.getAdminId());
    }

    /**
     * 修改管理员（不含密码）
     */
    @PreAuthorize("@ss.hasPermi('admin:user:edit')")
    @PutMapping("/admin")
    public Result<Void> editAdmin(@RequestBody SysAdmin admin) {
        SysAdmin exist = sysAdminMapper.selectById(admin.getAdminId());
        if (exist == null) {
            return Result.fail("管理员不存在");
        }
        admin.setPassword(null); // 不通过此接口改密码
        admin.setUpdateBy(currentAdminName());
        admin.setUpdateTime(new Date());
        sysAdminMapper.updateById(admin);
        return Result.ok();
    }

    /**
     * 修改管理员密码
     */
    @PreAuthorize("@ss.hasPermi('admin:user:edit')")
    @PutMapping("/admin/{id}/password")
    public Result<Void> changeAdminPassword(@PathVariable("id") Long adminId, @RequestBody Map<String, String> body) {
        SysAdmin exist = sysAdminMapper.selectById(adminId);
        if (exist == null) {
            return Result.fail("管理员不存在");
        }
        String newPwd = body.get("newPassword");
        if (newPwd == null || newPwd.length() < 8) {
            return Result.fail("密码至少 8 位");
        }
        SysAdmin update = new SysAdmin();
        update.setAdminId(adminId);
        update.setPassword(SecurityUtil.encryptPassword(newPwd));
        update.setUpdateBy(currentAdminName());
        update.setUpdateTime(new Date());
        sysAdminMapper.updateById(update);
        return Result.ok();
    }

    /**
     * 启用/禁用管理员
     */
    @PreAuthorize("@ss.hasPermi('admin:user:edit')")
    @PutMapping("/admin/{id}/status")
    public Result<Void> toggleAdminStatus(@PathVariable("id") Long adminId, @RequestBody Map<String, String> body) {
        SysAdmin update = new SysAdmin();
        update.setAdminId(adminId);
        update.setStatus("1".equals(body.get("status")) ? "1" : "0");
        update.setUpdateBy(currentAdminName());
        update.setUpdateTime(new Date());
        sysAdminMapper.updateById(update);
        return Result.ok();
    }

    /**
     * 删除管理员（软删除，超级管理员不可删除）
     */
    @PreAuthorize("@ss.hasPermi('admin:user:remove')")
    @DeleteMapping("/admin/{id}")
    public Result<Void> removeAdmin(@PathVariable("id") Long adminId) {
        SysAdmin exist = sysAdminMapper.selectById(adminId);
        if (exist == null) {
            return Result.fail("管理员不存在");
        }
        if ("super".equals(exist.getRole())) {
            return Result.fail("超级管理员不可删除");
        }
        // 用 deleteById 触发逻辑删除（@TableLogic 字段不会被 updateById 更新）
        sysAdminMapper.deleteById(adminId);
        return Result.ok();
    }

    // ==================== 会员与订单管理 M22 ====================

    /**
     * 会员账号列表（家庭账号）
     */
    @PreAuthorize("@ss.hasPermi('admin:member:list')")
    @GetMapping("/family")
    public Result<List<Family>> listFamily(@RequestParam(value = "memberLevel", required = false) String memberLevel,
                                       @RequestParam(value = "memberStatus", required = false) String memberStatus,
                                       @RequestParam(value = "keyword", required = false) String keyword) {
        LambdaQueryWrapper<Family> wrapper = new LambdaQueryWrapper<Family>()
                .eq(Family::getDelFlag, "0")
                .orderByDesc(Family::getFamilyId);
        if (memberLevel != null) {
            wrapper.eq(Family::getMemberLevel, memberLevel);
        }
        if (memberStatus != null) {
            wrapper.eq(Family::getMemberStatus, memberStatus);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(Family::getUsername, keyword)
                    .or().like(Family::getParentName, keyword)
                    .or().like(Family::getPhone, keyword));
        }
        List<Family> list = familyMapper.selectList(wrapper);
        list.forEach(f -> f.setPassword(null));
        return Result.ok(list);
    }

    /**
     * 查看家庭详情
     */
    @PreAuthorize("@ss.hasPermi('admin:member:query')")
    @GetMapping("/family/{id}")
    public Result<Family> getFamily(@PathVariable("id") Long familyId) {
        Family family = familyMapper.selectById(familyId);
        if (family == null || "2".equals(family.getDelFlag())) {
            return Result.fail("家庭不存在");
        }
        family.setPassword(null);
        return Result.ok(family);
    }

    /**
     * 订阅订单查询
     */
    @PreAuthorize("@ss.hasPermi('admin:order:list')")
    @GetMapping("/order")
    public Result<List<MemberOrder>> listOrder(@RequestParam(required = false) Long familyId,
                                          @RequestParam(required = false) String planType,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String refundStatus) {
        return Result.ok(orderService.listForAdmin(Map.of(
                "familyId", familyId == null ? "" : familyId,
                "planType", planType == null ? "" : planType,
                "status", status == null ? "" : status,
                "refundStatus", refundStatus == null ? "" : refundStatus
        )));
    }

    /**
     * 订单详情
     */
    @PreAuthorize("@ss.hasPermi('admin:order:query')")
    @GetMapping("/order/{id}")
    public Result<MemberOrder> getOrder(@PathVariable("id") Long orderId) {
        return Result.ok(orderService.getById(orderId));
    }

    /**
     * 退款处理
     */
    @PreAuthorize("@ss.hasPermi('admin:order:refund')")
    @PostMapping("/order/{id}/refund")
    public Result<Void> refundOrder(@PathVariable("id") Long orderId, @RequestBody Map<String, String> body) {
        orderService.refundOrder(orderId, body.get("reason"));
        return Result.ok();
    }

    /**
     * 后台为家长手动开通/续费（运营场景）
     */
    @PreAuthorize("@ss.hasPermi('admin:order:add')")
    @PostMapping("/order/manual")
    public Result<MemberOrder> manualOpen(@RequestBody Map<String, Object> body) {
        Long familyId = Long.valueOf(body.get("familyId").toString());
        String planType = body.get("planType") == null ? "yearly" : body.get("planType").toString();
        String paymentMethod = body.get("paymentMethod") == null ? "manual" : body.get("paymentMethod").toString();
        return Result.ok(orderService.renew(familyId, planType, paymentMethod));
    }

    // ==================== 工具方法 ====================

    /**
     * 从 SecurityContext 获取当前管理员账号
     * TODO: Agent-5 提供 JwtAuthenticationFilter 后替换
     */
    private String currentAdminName() {
        return "admin"; // Mock
    }
}
