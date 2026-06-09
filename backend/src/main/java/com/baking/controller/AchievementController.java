package com.baking.controller;

import com.baking.common.Result;
import com.baking.entity.Achievement;
import com.baking.entity.UserAchievement;
import com.baking.service.AchievementService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/achievements")
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;

    @GetMapping
    public Result<List<Achievement>> getAllAchievements() {
        return Result.success(achievementService.getAllAchievements());
    }

    @GetMapping("/user/{userId}")
    public Result<List<UserAchievement>> getUserAchievements(@PathVariable Long userId) {
        return Result.success(achievementService.getUserAchievements(userId));
    }

    @PostMapping("/check-in/{userId}")
    public Result<Map<String, Object>> checkIn(@PathVariable Long userId) {
        List<UserAchievement> newlyUnlocked = achievementService.checkIn(userId);
        int streakDays = achievementService.getStreakDays(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("streakDays", streakDays);
        result.put("newlyUnlocked", newlyUnlocked);
        return Result.success(result);
    }

    @GetMapping("/check-in/{userId}/status")
    public Result<Map<String, Object>> getCheckInStatus(@PathVariable Long userId) {
        boolean hasCheckedIn = achievementService.hasCheckedInToday(userId);
        int streakDays = achievementService.getStreakDays(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("hasCheckedInToday", hasCheckedIn);
        result.put("streakDays", streakDays);
        return Result.success(result);
    }

    @PostMapping("/check-publish/{userId}")
    public Result<List<UserAchievement>> checkPublishAchievements(@PathVariable Long userId) {
        return Result.success(achievementService.checkPublishAchievements(userId));
    }

    @PostMapping("/check-favorite/{userId}")
    public Result<List<UserAchievement>> checkFavoriteAchievements(@PathVariable Long userId) {
        return Result.success(achievementService.checkFavoriteAchievements(userId));
    }
}
