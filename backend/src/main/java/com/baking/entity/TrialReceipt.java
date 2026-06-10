package com.baking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trial_receipt")
public class TrialReceipt {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recipeId;

    private Long userId;

    private Integer success;

    private Integer tasteRating;

    private String tasteComment;

    private String tempAdjustment;

    private String moldDifference;

    private String notes;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String userName;

    @TableField(exist = false)
    private String userAvatar;
}
