package com.baking.controller;

import com.baking.common.Result;
import com.baking.entity.FavoriteFolder;
import com.baking.entity.UserAchievement;
import com.baking.service.AchievementService;
import com.baking.service.FavoriteService;
import com.baking.service.RecipeService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final AchievementService achievementService;
    private final RecipeService recipeService;

    @GetMapping("/check")
    public Result<Boolean> isFavorited(@RequestParam Long userId, @RequestParam Long recipeId) {
        return Result.success(favoriteService.isFavorited(userId, recipeId));
    }

    @PostMapping
    public Result<Map<String, Object>> addFavorite(@RequestBody FavoriteRequest request) {
        favoriteService.addFavorite(request.getUserId(), request.getRecipeId(), request.getFolderId());
        List<UserAchievement> newlyUnlocked = achievementService.checkFavoriteAchievements(request.getUserId());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("newlyUnlocked", newlyUnlocked);
        return Result.success(result);
    }

    @DeleteMapping
    public Result<Void> removeFavorite(@RequestParam Long userId, @RequestParam Long recipeId) {
        favoriteService.removeFavorite(userId, recipeId);
        return Result.success();
    }

    @GetMapping("/folders/{userId}")
    public Result<List<FavoriteFolder>> getUserFolders(@PathVariable Long userId) {
        return Result.success(favoriteService.getUserFolders(userId));
    }

    @PostMapping("/folders")
    public Result<FavoriteFolder> createFolder(@RequestBody FolderRequest request) {
        return Result.success(favoriteService.createFolder(
                request.getUserId(), request.getName(), request.getDescription()));
    }

    @DeleteMapping("/folders/{userId}/{folderId}")
    public Result<Void> deleteFolder(@PathVariable Long userId, @PathVariable Long folderId) {
        favoriteService.deleteFolder(userId, folderId);
        return Result.success();
    }

    @GetMapping("/ingredients/summary/{userId}")
    public Result<List<Map<String, Object>>> getFavoriteIngredientsSummary(@PathVariable Long userId) {
        return Result.success(recipeService.getFavoriteIngredientsSummary(userId));
    }

    @Data
    public static class FavoriteRequest {
        private Long userId;
        private Long recipeId;
        private Long folderId;
    }

    @Data
    public static class FolderRequest {
        private Long userId;
        private String name;
        private String description;
    }
}
