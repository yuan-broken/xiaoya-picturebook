package com.picturebook.book.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.picturebook.common.core.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 绘本页面表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_book_page")
public class BookPage extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long pageId;

    private Long bookId;
    private Integer pageNum;
    private String illustrationUrl;
    private String roleEmoji;
    @TableField("bg_color")
    private String roleColor;
    private String narration;
    @TableField("decoration")
    private String decorations;
    private Integer sortOrder;
}
