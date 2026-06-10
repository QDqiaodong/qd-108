package com.baking.controller;

import com.baking.common.Result;
import com.baking.entity.RecipeStepProgress;
import com.baking.service.RecipeStepProgressService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recipe-progress")
@RequiredArgsConstructor
public class RecipeStepProgressController {

    private final RecipeStepProgressService recipeStepProgressService;

    @GetMapping
    public Result<RecipeStepProgress> getProgress(
            @RequestParam Long userId,
            @RequestParam Long recipeId) {
        return Result.success(recipeStepProgressService.getProgress(userId, recipeId));
    }

    @PostMapping
    public Result<RecipeStepProgress> saveProgress(@RequestBody ProgressRequest request) {
        RecipeStepProgress progress = recipeStepProgressService.saveProgress(
                request.getUserId(),
                request.getRecipeId(),
                request.getCompletedSteps(),
                request.getLastStepIndex()
        );
        return Result.success(progress);
    }

    @DeleteMapping
    public Result<Void> resetProgress(
            @RequestParam Long userId,
            @RequestParam Long recipeId) {
        recipeStepProgressService.resetProgress(userId, recipeId);
        return Result.success();
    }

    @Data
    public static class ProgressRequest {
        private Long userId;
        private Long recipeId;
        private String completedSteps;
        private Integer lastStepIndex;
    }
}
