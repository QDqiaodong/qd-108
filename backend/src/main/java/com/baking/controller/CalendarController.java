package com.baking.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baking.common.Result;
import com.baking.entity.BakePlan;
import com.baking.entity.CheckIn;
import com.baking.entity.Comment;
import com.baking.entity.Recipe;
import com.baking.mapper.CommentMapper;
import com.baking.mapper.RecipeMapper;
import com.baking.service.AchievementService;
import com.baking.service.BakePlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final AchievementService achievementService;
    private final BakePlanService bakePlanService;
    private final RecipeMapper recipeMapper;
    private final CommentMapper commentMapper;

    @GetMapping("/month/{userId}")
    public Result<Map<String, Object>> getMonthData(
            @PathVariable Long userId,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate queryStartDate;
        LocalDate queryEndDate;

        if (startDate != null && endDate != null) {
            queryStartDate = LocalDate.parse(startDate);
            queryEndDate = LocalDate.parse(endDate);
        } else {
            queryStartDate = LocalDate.of(year, month, 1);
            queryEndDate = queryStartDate.plusMonths(1).minusDays(1);
        }

        List<CheckIn> checkIns = achievementService.getCheckInsBetween(userId, queryStartDate, queryEndDate);
        List<BakePlan> plans = bakePlanService.getPlansBetween(userId, queryStartDate, queryEndDate);
        int streakDays = achievementService.getStreakDays(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("checkIns", checkIns);
        result.put("plans", plans);
        result.put("streakDays", streakDays);
        return Result.success(result);
    }

    @GetMapping("/day/{userId}")
    public Result<Map<String, Object>> getDayDetail(
            @PathVariable Long userId,
            @RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        LocalDateTime startOfDay = localDate.atStartOfDay();
        LocalDateTime endOfDay = localDate.atTime(LocalTime.MAX);

        LambdaQueryWrapper<Recipe> recipeWrapper = new LambdaQueryWrapper<>();
        recipeWrapper.eq(Recipe::getUserId, userId);
        recipeWrapper.eq(Recipe::getStatus, 1);
        recipeWrapper.between(Recipe::getCreatedAt, startOfDay, endOfDay);
        recipeWrapper.orderByDesc(Recipe::getCreatedAt);
        List<Recipe> recipes = recipeMapper.selectList(recipeWrapper);

        LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(Comment::getUserId, userId);
        commentWrapper.between(Comment::getCreatedAt, startOfDay, endOfDay);
        commentWrapper.orderByDesc(Comment::getCreatedAt);
        List<Comment> comments = commentMapper.selectList(commentWrapper);

        BakePlan plan = bakePlanService.getPlanByDate(userId, localDate);

        boolean hasCheckedIn = false;
        List<CheckIn> checkIns = achievementService.getMonthCheckIns(userId, localDate.getYear(), localDate.getMonthValue());
        for (CheckIn checkIn : checkIns) {
            if (checkIn.getCheckDate().equals(localDate)) {
                hasCheckedIn = true;
                break;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("date", date);
        result.put("hasCheckedIn", hasCheckedIn);
        result.put("recipes", recipes);
        result.put("comments", comments);
        result.put("plan", plan);
        return Result.success(result);
    }

    @PostMapping("/plan")
    public Result<BakePlan> createPlan(@RequestBody BakePlan plan) {
        return Result.success(bakePlanService.createPlan(plan));
    }

    @PutMapping("/plan")
    public Result<BakePlan> updatePlan(@RequestBody BakePlan plan) {
        return Result.success(bakePlanService.updatePlan(plan));
    }

    @DeleteMapping("/plan/{id}")
    public Result<Boolean> deletePlan(@PathVariable Long id, @RequestParam Long userId) {
        return Result.success(bakePlanService.deletePlan(id, userId));
    }
}
