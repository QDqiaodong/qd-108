package com.baking.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baking.common.Result;
import com.baking.dto.CategoryBakeStats;
import com.baking.entity.Recipe;
import com.baking.entity.UserAchievement;
import com.baking.service.AchievementService;
import com.baking.service.RecipeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;
    private final AchievementService achievementService;

    @GetMapping
    public Result<IPage<Recipe>> getRecipePage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy) {
        return Result.success(recipeService.getRecipePage(pageNum, pageSize, categoryId, keyword, sortBy));
    }

    @GetMapping("/hot")
    public Result<List<Recipe>> getHotRecipes(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Long categoryId) {
        return Result.success(recipeService.getHotRecipes(limit, categoryId));
    }

    @GetMapping("/ingredients/hot")
    public Result<List<Map<String, Object>>> getHotIngredients(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(recipeService.getHotIngredients(limit));
    }

    @GetMapping("/{id}")
    public Result<Recipe> getRecipeDetail(@PathVariable Long id) {
        return Result.success(recipeService.getRecipeDetail(id));
    }

    @GetMapping("/category/bake-stats")
    public Result<List<CategoryBakeStats>> getCategoryBakeStats() {
        return Result.success(recipeService.getCategoryBakeStats());
    }

    @GetMapping("/{id}/bake-stats")
    public Result<CategoryBakeStats> getRecipeCategoryBakeStats(@PathVariable Long id) {
        Recipe recipe = recipeService.getRecipeDetail(id);
        if (recipe == null) {
            return Result.success(null);
        }
        return Result.success(recipeService.getCategoryBakeStats(recipe.getCategoryId()));
    }

    @PostMapping
    public Result<Map<String, Object>> createRecipe(@RequestBody RecipeRequest request) {
        Recipe recipe = new Recipe();
        recipe.setTitle(request.getTitle());
        recipe.setCoverImage(request.getCoverImage());
        recipe.setDescription(request.getDescription());
        recipe.setIngredients(request.getIngredients());
        recipe.setSteps(request.getSteps());
        recipe.setBakeTemp(request.getBakeTemp());
        recipe.setBakeTime(request.getBakeTime());
        recipe.setDifficulty(request.getDifficulty());
        recipe.setServings(request.getServings());
        recipe.setCategoryId(request.getCategoryId());
        recipe.setUserId(request.getUserId());
        Recipe createdRecipe = recipeService.createRecipe(
                recipe, request.getImages(),
                request.getThumbnails(),
                request.getImageTypes());

        List<UserAchievement> publishUnlocked = achievementService.checkPublishAchievements(request.getUserId());
        List<UserAchievement> categoryUnlocked = achievementService.checkCategoryAchievements(
                request.getUserId(), request.getCategoryId());
        List<UserAchievement> checkInUnlocked = achievementService.checkIn(request.getUserId());

        List<UserAchievement> allUnlocked = new ArrayList<>();
        allUnlocked.addAll(publishUnlocked);
        allUnlocked.addAll(categoryUnlocked);
        allUnlocked.addAll(checkInUnlocked);

        Set<Long> seenIds = new HashSet<>();
        List<UserAchievement> newlyUnlocked = new ArrayList<>();
        for (UserAchievement ua : allUnlocked) {
            if (ua.getAchievementId() != null && seenIds.add(ua.getAchievementId())) {
                newlyUnlocked.add(ua);
            }
        }

        int streakDays = achievementService.getStreakDays(request.getUserId());

        Map<String, Object> result = new HashMap<>();
        result.put("recipe", createdRecipe);
        result.put("newlyUnlocked", newlyUnlocked);
        result.put("streakDays", streakDays);
        return Result.success(result);
    }

    @GetMapping("/user/{userId}")
    public Result<IPage<Recipe>> getUserRecipes(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(recipeService.getUserRecipes(userId, pageNum, pageSize));
    }

    @GetMapping("/favorite/{userId}")
    public Result<IPage<Recipe>> getFavoriteRecipes(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(recipeService.getFavoriteRecipes(userId, pageNum, pageSize));
    }

    @PostMapping("/batch")
    public Result<List<Recipe>> getRecipesByIds(@RequestBody Map<String, List<Long>> request) {
        List<Long> ids = request.get("ids");
        return Result.success(recipeService.getRecipesByIds(ids));
    }

    @Data
    public static class RecipeRequest {
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
        private List<String> images;
        private List<String> thumbnails;
        private List<String> imageTypes;
    }
}
