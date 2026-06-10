package com.baking.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baking.common.Result;
import com.baking.entity.Recipe;
import com.baking.entity.UserAchievement;
import com.baking.service.AchievementService;
import com.baking.service.RecipeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            @RequestParam(required = false) String keyword) {
        return Result.success(recipeService.getRecipePage(pageNum, pageSize, categoryId, keyword));
    }

    @GetMapping("/hot")
    public Result<List<Recipe>> getHotRecipes(@RequestParam(defaultValue = "10") int limit) {
        return Result.success(recipeService.getHotRecipes(limit));
    }

    @GetMapping("/ingredients/hot")
    public Result<List<Map<String, Object>>> getHotIngredients(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(recipeService.getHotIngredients(limit));
    }

    @GetMapping("/{id}")
    public Result<Recipe> getRecipeDetail(@PathVariable Long id) {
        return Result.success(recipeService.getRecipeDetail(id));
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
        Recipe createdRecipe = recipeService.createRecipe(recipe, request.getImages());

        List<UserAchievement> newlyUnlocked = achievementService.checkPublishAchievements(request.getUserId());
        List<UserAchievement> categoryUnlocked = achievementService.checkCategoryAchievements(
                request.getUserId(), request.getCategoryId());
        newlyUnlocked.addAll(categoryUnlocked);

        List<UserAchievement> checkInUnlocked = achievementService.checkIn(request.getUserId());
        newlyUnlocked.addAll(checkInUnlocked);

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
    }
}
