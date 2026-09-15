package com.picturebook.user.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.picturebook.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会员订单实体 biz_member_order
 * 对应 PRD M39：订阅/支付/退款/试用/续费。
 *
 * @author Agent-4
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_member_order")
public class MemberOrder extends BaseEntity {

    /** 订单ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long orderId;

    /** 家庭ID */
    private Long familyId;

    /** 订单号（业务唯一） */
    private String orderNo;

    /** 计划类型（DB列名 plan_id） */
    @TableField("plan_id")
    private String planType;

    /** 金额 */
    private java.math.BigDecimal amount;

    /** 支付方式 */
    private String paymentMethod;

    /** 支付时间（schema无此列，Phase2扩展） */
    @TableField(exist = false)
    private java.util.Date paidAt;

    /** 退款状态（schema无此列，Phase2扩展） */
    @TableField(exist = false)
    private String refundStatus;

    /** 退款时间（schema无此列） */
    @TableField(exist = false)
    private java.util.Date refundAt;

    /** 订单开始时间（DB列名 start_time） */
    @TableField("start_time")
    private java.util.Date startAt;

    /** 订单到期时间（DB列名 expire_time） */
    @TableField("expire_time")
    private java.util.Date endAt;

    /** 订单状态（DB列名 pay_status: pending/paid/refunded） */
    @TableField("pay_status")
    private String status;

    /** 备注（schema无此列） */
    @TableField(exist = false)
    private String remark;
}
