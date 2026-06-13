package com.baking.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WeeklyReviewDTO {

    private String weekStart;

    private String weekEnd;

    private Integer recipeCount;

    private Integer trialCount;

    private Integer commentCount;

    private Integer checkInCount;

    private Integer streakDays;

    private List<Map<String, Object>> categoryStats;

    private List<Map<String, Object>> dailyActivity;

    private List<Map<String, Object>> topRecipes;
}
