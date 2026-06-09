package com.baking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baking.entity.*;
import com.baking.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementMapper achievementMapper;
    private final UserAchievementMapper userAchievementMapper;
    private final CheckInMapper checkInMapper;
    private final RecipeMapper recipeMapper;
    private final FavoriteMapper favoriteMapper;

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

        LambdaQueryWrapper<CheckIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CheckIn::getUserId, userId);
        wrapper.between(CheckIn::getCheckDate, startDate, endDate);
        return checkInMapper.selectList(wrapper);
    }
}
