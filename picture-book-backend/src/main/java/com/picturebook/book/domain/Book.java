package com.picturebook.book.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.picturebook.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 绘本内容表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_book")
public class Book extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long bookId;

    private String title;
    private String author;
    @TableField(exist = false)
    private String publisher;
    @TableField(exist = false)
    private String isbn;
    private String ageGroup;
    private String theme;
    @TableField("language")
    private String languageType;
    @TableField("cover_url")
    private String coverImage;
    @TableField(exist = false)
    private String coverEmoji;
    @TableField(exist = false)
    private String coverColor;
    @TableField("description")
    private String summary;
    @TableField("page_count")
    private Integer totalPages;
    @TableField("duration")
    private Integer readDuration;
    private BigDecimal rating;
    private String tags;
    private Integer sortOrder;
    /** 上架状态（1=上架 0=下架） */
    private Integer status;
    /** 审核状态（pass/block/pending） */
    private String auditStatus;
}
