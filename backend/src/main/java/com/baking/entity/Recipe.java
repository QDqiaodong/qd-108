package com.baking.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("recipe")
public class Recipe {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String coverImage;

    private String description;

    private String ingredients;

    private String steps;

    private Integer bakeTemp;

    private Integer bakeTime;

    private Integer difficulty;

    private Integer servings;

    private Long categoryId;

    private Long userId;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer favoriteCount;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private String authorName;

    @TableField(exist = false)
    private String authorAvatar;

    @TableField(exist = false)
    private List<RecipeImage> images;
}
