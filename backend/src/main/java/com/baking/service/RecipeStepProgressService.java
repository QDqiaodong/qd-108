package com.baking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baking.entity.RecipeStepProgress;
import com.baking.mapper.RecipeStepProgressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecipeStepProgressService {

    private final RecipeStepProgressMapper recipeStepProgressMapper;

    public RecipeStepProgress getProgress(Long userId, Long recipeId) {
        LambdaQueryWrapper<RecipeStepProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecipeStepProgress::getUserId, userId);
        wrapper.eq(RecipeStepProgress::getRecipeId, recipeId);
        return recipeStepProgressMapper.selectOne(wrapper);
    }

    public RecipeStepProgress saveProgress(Long userId, Long recipeId, String completedSteps, Integer lastStepIndex) {
        LambdaQueryWrapper<RecipeStepProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecipeStepProgress::getUserId, userId);
        wrapper.eq(RecipeStepProgress::getRecipeId, recipeId);
        RecipeStepProgress existing = recipeStepProgressMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setCompletedSteps(completedSteps);
            existing.setLastStepIndex(lastStepIndex);
            recipeStepProgressMapper.updateById(existing);
            return existing;
        } else {
            RecipeStepProgress progress = new RecipeStepProgress();
            progress.setUserId(userId);
            progress.setRecipeId(recipeId);
            progress.setCompletedSteps(completedSteps);
            progress.setLastStepIndex(lastStepIndex);
            recipeStepProgressMapper.insert(progress);
            return progress;
        }
    }

    public void resetProgress(Long userId, Long recipeId) {
        LambdaQueryWrapper<RecipeStepProgress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecipeStepProgress::getUserId, userId);
        wrapper.eq(RecipeStepProgress::getRecipeId, recipeId);
        recipeStepProgressMapper.delete(wrapper);
    }
}
