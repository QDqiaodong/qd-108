package com.baking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baking.dto.WeeklyReviewDTO;
import com.baking.entity.*;
import com.baking.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementMapper achievementMapper;
    private final UserAchievementMapper userAchievementMapper;
    private final CheckInMapper checkInMapper;
    private final RecipeMapper recipeMapper;
    private final FavoriteMapper favoriteMapper;
    private final CommentMapper commentMapper;
    private final TrialReceiptMapper trialReceiptMapper;
    private final CategoryMapper categoryMapper;

    public List<Achievement> getAllAchievements() {
        LambdaQueryWrapper<Achievement> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Achievement::getSortOrder);
        return achievementMapper.selectList(wrapper);
    }

    public List<UserAchievement> getUserAchievements(Long userId) {
        LambdaQueryWrapper<UserAchievement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAchievement::getUserId, userId);
        List<UserAchievement> userAchievements = userAchievementMapper.selectList(wrapper);

        List<Achievement> allAchievements = getAllAchievements();

        for (Achievement achievement : allAchievements) {
            boolean exists = userAchievements.stream()
                    .anyMatch(ua -> ua.getAchievementId().equals(achievement.getId()));
            if (!exists) {
                UserAchievement ua = new UserAchievement();
                ua.setUserId(userId);
                ua.setAchievementId(achievement.getId());
                ua.setProgress(0);
                ua.setUnlocked(0);
                ua.setAchievement(achievement);
                userAchievements.add(ua);
            } else {
                userAchievements.stream()
                        .filter(ua -> ua.getAchievementId().equals(achievement.getId()))
                        .findFirst()
                        .ifPresent(ua -> ua.setAchievement(achievement));
            }
        }

        userAchievements.sort((a, b) -> {
            int sortA = a.getAchievement() != null ? a.getAchievement().getSortOrder() : 999;
            int sortB = b.getAchievement() != null ? b.getAchievement().getSortOrder() : 999;
            return Integer.compare(sortA, sortB);
        });

        return userAchievements;
    }

    @Transactional
    public List<UserAchievement> checkPublishAchievements(Long userId) {
        LambdaQueryWrapper<Recipe> recipeWrapper = new LambdaQueryWrapper<>();
        recipeWrapper.eq(Recipe::getUserId, userId);
        recipeWrapper.eq(Recipe::getStatus, 1);
        int publishCount = recipeMapper.selectCount(recipeWrapper).intValue();

        return updateAchievementsByType(userId, "publish", publishCount, null);
    }

    @Transactional
    public List<UserAchievement> checkFavoriteAchievements(Long userId) {
        LambdaQueryWrapper<Favorite> favoriteWrapper = new LambdaQueryWrapper<>();
        favoriteWrapper.eq(Favorite::getUserId, userId);
        int favoriteCount = favoriteMapper.selectCount(favoriteWrapper).intValue();

        return updateAchievementsByType(userId, "favorite", favoriteCount, null);
    }

    @Transactional
    public List<UserAchievement> checkCategoryAchievements(Long userId, Long categoryId) {
        LambdaQueryWrapper<Recipe> recipeWrapper = new LambdaQueryWrapper<>();
        recipeWrapper.eq(Recipe::getUserId, userId);
        recipeWrapper.eq(Recipe::getStatus, 1);
        recipeWrapper.eq(Recipe::getCategoryId, categoryId);
        int categoryCount = recipeMapper.selectCount(recipeWrapper).intValue();

        return updateAchievementsByType(userId, "category", categoryCount, categoryId);
    }

    @Transactional
    public List<UserAchievement> checkCheckInAchievements(Long userId) {
        int streakDays = getStreakDays(userId);
        return updateAchievementsByType(userId, "checkin", streakDays, null);
    }

    private List<UserAchievement> updateAchievementsByType(Long userId, String type, int progress, Long categoryId) {
        LambdaQueryWrapper<Achievement> achWrapper = new LambdaQueryWrapper<>();
        achWrapper.eq(Achievement::getType, type);
        if (categoryId != null) {
            achWrapper.eq(Achievement::getCategoryId, categoryId);
        }
        List<Achievement> achievements = achievementMapper.selectList(achWrapper);

        List<UserAchievement> newlyUnlocked = new ArrayList<>();

        for (Achievement achievement : achievements) {
            LambdaQueryWrapper<UserAchievement> uaWrapper = new LambdaQueryWrapper<>();
            uaWrapper.eq(UserAchievement::getUserId, userId);
            uaWrapper.eq(UserAchievement::getAchievementId, achievement.getId());
            UserAchievement userAchievement = userAchievementMapper.selectOne(uaWrapper);

            if (userAchievement == null) {
                userAchievement = new UserAchievement();
                userAchievement.setUserId(userId);
                userAchievement.setAchievementId(achievement.getId());
                userAchievement.setProgress(0);
                userAchievement.setUnlocked(0);
            }

            int oldProgress = userAchievement.getProgress();
            boolean wasUnlocked = userAchievement.getUnlocked() != null && userAchievement.getUnlocked() == 1;

            userAchievement.setProgress(Math.min(progress, achievement.getTarget()));

            if (!wasUnlocked && progress >= achievement.getTarget()) {
                userAchievement.setUnlocked(1);
                userAchievement.setUnlockedAt(LocalDateTime.now());
                userAchievement.setAchievement(achievement);
                newlyUnlocked.add(userAchievement);
            }

            if (userAchievement.getId() == null) {
                userAchievementMapper.insert(userAchievement);
            } else {
                userAchievementMapper.updateById(userAchievement);
            }
        }

        return newlyUnlocked;
    }

    public int getStreakDays(Long userId) {
        LocalDate today = LocalDate.now();
        int streak = 0;
        LocalDate currentDate = today;

        while (true) {
            LambdaQueryWrapper<CheckIn> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CheckIn::getUserId, userId);
            wrapper.eq(CheckIn::getCheckDate, currentDate);
            CheckIn checkIn = checkInMapper.selectOne(wrapper);

            if (checkIn != null) {
                streak++;
                currentDate = currentDate.minusDays(1);
            } else {
                break;
            }
        }

        return streak;
    }

    @Transactional
    public List<UserAchievement> checkIn(Long userId) {
        LocalDate today = LocalDate.now();

        LambdaQueryWrapper<CheckIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckIn::getUserId, userId);
        wrapper.eq(CheckIn::getCheckDate, today);
        CheckIn existing = checkInMapper.selectOne(wrapper);

        if (existing != null) {
            return new ArrayList<>();
        }

        CheckIn checkIn = new CheckIn();
        checkIn.setUserId(userId);
        checkIn.setCheckDate(today);
        checkInMapper.insert(checkIn);

        return checkCheckInAchievements(userId);
    }

    public boolean hasCheckedInToday(Long userId) {
        LocalDate today = LocalDate.now();
        LambdaQueryWrapper<CheckIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckIn::getUserId, userId);
        wrapper.eq(CheckIn::getCheckDate, today);
        return checkInMapper.selectCount(wrapper) > 0;
    }

    public List<CheckIn> getMonthCheckIns(Long userId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);
        return getCheckInsBetween(userId, startDate, endDate);
    }

    public List<CheckIn> getCheckInsBetween(Long userId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<CheckIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckIn::getUserId, userId);
        wrapper.between(CheckIn::getCheckDate, startDate, endDate);
        return checkInMapper.selectList(wrapper);
    }

    public WeeklyReviewDTO getWeeklyReview(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        LocalDateTime startOfWeek = weekStart.atStartOfDay();
        LocalDateTime endOfWeek = weekEnd.atTime(LocalTime.MAX);

        WeeklyReviewDTO dto = new WeeklyReviewDTO();
        dto.setWeekStart(weekStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        dto.setWeekEnd(weekEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        dto.setStreakDays(getStreakDays(userId));

        LambdaQueryWrapper<Recipe> recipeWrapper = new LambdaQueryWrapper<>();
        recipeWrapper.eq(Recipe::getUserId, userId);
        recipeWrapper.eq(Recipe::getStatus, 1);
        recipeWrapper.between(Recipe::getCreatedAt, startOfWeek, endOfWeek);
        recipeWrapper.orderByDesc(Recipe::getCreatedAt);
        List<Recipe> weekRecipes = recipeMapper.selectList(recipeWrapper);
        dto.setRecipeCount(weekRecipes.size());

        LambdaQueryWrapper<Comment> commentWrapper = new LambdaQueryWrapper<>();
        commentWrapper.eq(Comment::getUserId, userId);
        commentWrapper.between(Comment::getCreatedAt, startOfWeek, endOfWeek);
        int commentCount = commentMapper.selectCount(commentWrapper).intValue();
        dto.setCommentCount(commentCount);

        LambdaQueryWrapper<CheckIn> checkInWrapper = new LambdaQueryWrapper<>();
        checkInWrapper.eq(CheckIn::getUserId, userId);
        checkInWrapper.between(CheckIn::getCheckDate, weekStart, weekEnd);
        int checkInCount = checkInMapper.selectCount(checkInWrapper).intValue();
        dto.setCheckInCount(checkInCount);

        LambdaQueryWrapper<TrialReceipt> trialWrapper = new LambdaQueryWrapper<>();
        trialWrapper.eq(TrialReceipt::getUserId, userId);
        trialWrapper.between(TrialReceipt::getCreatedAt, startOfWeek, endOfWeek);
        int trialCount = trialReceiptMapper.selectCount(trialWrapper).intValue();
        dto.setTrialCount(trialCount);

        Map<Long, Integer> categoryCountMap = new HashMap<>();
        for (Recipe recipe : weekRecipes) {
            Long categoryId = recipe.getCategoryId();
            if (categoryId != null) {
                categoryCountMap.put(categoryId, categoryCountMap.getOrDefault(categoryId, 0) + 1);
            }
        }

        List<Map<String, Object>> categoryStats = new ArrayList<>();
        List<Category> categories = categoryMapper.selectList(null);
        Map<Long, String> categoryNameMap = new HashMap<>();
        for (Category cat : categories) {
            categoryNameMap.put(cat.getId(), cat.getName());
        }

        for (Map.Entry<Long, Integer> entry : categoryCountMap.entrySet()) {
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("categoryId", entry.getKey());
            stat.put("categoryName", categoryNameMap.getOrDefault(entry.getKey(), "未分类"));
            stat.put("count", entry.getValue());
            categoryStats.add(stat);
        }
        categoryStats.sort((a, b) -> (Integer) b.get("count") - (Integer) a.get("count"));
        dto.setCategoryStats(categoryStats);

        List<Map<String, Object>> dailyActivity = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate dayDate = weekStart.plusDays(i);
            LocalDateTime startOfDay = dayDate.atStartOfDay();
            LocalDateTime endOfDay = dayDate.atTime(LocalTime.MAX);

            LambdaQueryWrapper<Recipe> dayRecipeWrapper = new LambdaQueryWrapper<>();
            dayRecipeWrapper.eq(Recipe::getUserId, userId);
            dayRecipeWrapper.eq(Recipe::getStatus, 1);
            dayRecipeWrapper.between(Recipe::getCreatedAt, startOfDay, endOfDay);
            int dayRecipeCount = recipeMapper.selectCount(dayRecipeWrapper).intValue();

            LambdaQueryWrapper<CheckIn> dayCheckInWrapper = new LambdaQueryWrapper<>();
            dayCheckInWrapper.eq(CheckIn::getUserId, userId);
            dayCheckInWrapper.eq(CheckIn::getCheckDate, dayDate);
            boolean hasCheckIn = checkInMapper.selectCount(dayCheckInWrapper) > 0;

            LambdaQueryWrapper<TrialReceipt> dayTrialWrapper = new LambdaQueryWrapper<>();
            dayTrialWrapper.eq(TrialReceipt::getUserId, userId);
            dayTrialWrapper.between(TrialReceipt::getCreatedAt, startOfDay, endOfDay);
            int dayTrialCount = trialReceiptMapper.selectCount(dayTrialWrapper).intValue();

            Map<String, Object> dayStat = new LinkedHashMap<>();
            dayStat.put("date", dayDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            dayStat.put("dayOfWeek", dayDate.getDayOfWeek().getValue());
            dayStat.put("dayLabel", getDayLabel(i));
            dayStat.put("recipeCount", dayRecipeCount);
            dayStat.put("trialCount", dayTrialCount);
            dayStat.put("hasCheckIn", hasCheckIn);
            dayStat.put("isToday", dayDate.equals(today));
            dayStat.put("isFuture", dayDate.isAfter(today));
            dailyActivity.add(dayStat);
        }
        dto.setDailyActivity(dailyActivity);

        List<Map<String, Object>> topRecipes = weekRecipes.stream()
                .sorted(Comparator.comparingInt(Recipe::getViewCount).reversed())
                .limit(5)
                .map(recipe -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", recipe.getId());
                    map.put("title", recipe.getTitle());
                    map.put("coverImage", recipe.getCoverImage());
                    map.put("viewCount", recipe.getViewCount());
                    map.put("commentCount", recipe.getCommentCount());
                    map.put("favoriteCount", recipe.getFavoriteCount());
                    map.put("categoryId", recipe.getCategoryId());
                    map.put("categoryName", categoryNameMap.getOrDefault(recipe.getCategoryId(), "未分类"));
                    return map;
                })
                .collect(Collectors.toList());
        dto.setTopRecipes(topRecipes);

        return dto;
    }

    private String getDayLabel(int index) {
        String[] labels = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return labels[index];
    }
}
