package com.picturebook.user.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.picturebook.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 阅读打卡记录 biz_habit_checkin
 *
 * @author Phase2
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_habit_checkin")
public class HabitCheckin extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long familyId;
    private Long childId;

    @TableField("checkin_date")
    private Date checkinDate;

    /** 本次阅读分钟 */
    private Integer readingMinutes;

    private Long bookId;
    private Long taskId;

    /** 打卡后连续天数 */
    private Integer continuousDays;

    private String note;
}
