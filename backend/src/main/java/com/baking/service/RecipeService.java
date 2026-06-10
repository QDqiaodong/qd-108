package com.baking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baking.entity.Recipe;
import com.baking.entity.RecipeImage;
import com.baking.mapper.RecipeImageMapper;
import com.baking.mapper.RecipeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeMapper recipeMapper;
    private final RecipeImageMapper recipeImageMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final IngredientAliasService ingredientAliasService;

    private static final String HOT_RECIPES_KEY = "recipe:hot";
    private static final long HOT_RECIPES_TTL = 3600;
    private static final String HOT_INGREDIENTS_KEY = "ingredient:hot";
    private static final long HOT_INGREDIENTS_TTL = 3600;

    public IPage<Recipe> getRecipePage(int pageNum, int pageSize, Long categoryId, String keyword) {
        Page<Recipe> page = new Page<>(pageNum, pageSize);
        String searchKeyword = keyword;
        if (keyword != null && !keyword.trim().isEmpty()) {
            searchKeyword = ingredientAliasService.normalize(keyword.trim());
        }
        return recipeMapper.selectRecipePage(page, categoryId, searchKeyword);
    }

    @SuppressWarnings("unchecked")
    public List<Recipe> getHotRecipes(int limit) {
        List<Recipe> cached = (List<Recipe>) redisTemplate.opsForValue().get(HOT_RECIPES_KEY);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        List<Recipe> recipes = recipeMapper.selectHotRecipes(limit);
        redisTemplate.opsForValue().set(HOT_RECIPES_KEY, recipes, HOT_RECIPES_TTL, TimeUnit.SECONDS);
        return recipes;
    }

    public Recipe getRecipeDetail(Long id) {
        Recipe recipe = recipeMapper.selectRecipeDetail(id);
        if (recipe != null) {
            LambdaQueryWrapper<RecipeImage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(RecipeImage::getRecipeId, id);
            wrapper.orderByAsc(RecipeImage::getSortOrder);
            List<RecipeImage> images = recipeImageMapper.selectList(wrapper);
            recipe.setImages(images);

            recipe.setViewCount(recipe.getViewCount() + 1);
            recipeMapper.updateById(recipe);
        }
        return recipe;
    }

    @Transactional
    public Recipe createRecipe(Recipe recipe, List<String> imageUrls) {
        recipe.setViewCount(0);
        recipe.setLikeCount(0);
        recipe.setCommentCount(0);
        recipe.setFavoriteCount(0);
        recipe.setStatus(1);

        String normalizedIngredients = ingredientAliasService.normalizeIngredientsJson(recipe.getIngredients());
        recipe.setIngredients(normalizedIngredients);

        recipeMapper.insert(recipe);

        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (int i = 0; i < imageUrls.size(); i++) {
                RecipeImage image = new RecipeImage();
                image.setRecipeId(recipe.getId());
                image.setImageUrl(imageUrls.get(i));
                image.setSortOrder(i);
                recipeImageMapper.insert(image);
            }
        }

        redisTemplate.delete(HOT_RECIPES_KEY);
        redisTemplate.delete(HOT_INGREDIENTS_KEY);
        return recipe;
    }

    public IPage<Recipe> getUserRecipes(Long userId, int pageNum, int pageSize) {
        Page<Recipe> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Recipe> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Recipe::getUserId, userId);
        wrapper.eq(Recipe::getStatus, 1);
        wrapper.orderByDesc(Recipe::getCreatedAt);
        return recipeMapper.selectPage(page, wrapper);
    }

    public IPage<Recipe> getFavoriteRecipes(Long userId, int pageNum, int pageSize) {
        Page<Recipe> page = new Page<>(pageNum, pageSize);
        return recipeMapper.selectFavoriteRecipes(page, userId);
    }

    public void incrementFavoriteCount(Long recipeId, int delta) {
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe != null) {
            recipe.setFavoriteCount(Math.max(0, recipe.getFavoriteCount() + delta));
            recipeMapper.updateById(recipe);
        }
    }

    public void incrementCommentCount(Long recipeId, int delta) {
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe != null) {
            recipe.setCommentCount(Math.max(0, recipe.getCommentCount() + delta));
            recipeMapper.updateById(recipe);
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getHotIngredients(int limit) {
        List<Map<String, Object>> cached = (List<Map<String, Object>>) redisTemplate.opsForValue().get(HOT_INGREDIENTS_KEY);
        if (cached != null && !cached.isEmpty()) {
            return cached.stream().limit(limit).collect(Collectors.toList());
        }

        List<Map<String, Object>> hotIngredients = calculateHotIngredients();
        redisTemplate.opsForValue().set(HOT_INGREDIENTS_KEY, hotIngredients, HOT_INGREDIENTS_TTL, TimeUnit.SECONDS);
        return hotIngredients.stream().limit(limit).collect(Collectors.toList());
    }

    private List<Map<String, Object>> calculateHotIngredients() {
        LambdaQueryWrapper<Recipe> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Recipe::getStatus, 1);
        wrapper.select(Recipe::getIngredients);
        List<Recipe> recipes = recipeMapper.selectList(wrapper);

        Map<String, Integer> ingredientCount = new HashMap<>();
        for (Recipe recipe : recipes) {
            List<String> normalizedNames = ingredientAliasService.extractAndNormalizeIngredientNames(recipe.getIngredients());
            for (String name : normalizedNames) {
                ingredientCount.put(name, ingredientCount.getOrDefault(name, 0) + 1);
            }
        }

        return ingredientCount.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getFavoriteIngredientsSummary(Long userId) {
        IPage<Recipe> page = getFavoriteRecipes(userId, 1, Integer.MAX_VALUE);
        List<Recipe> recipes = page.getRecords();

        Map<String, Integer> ingredientCount = new HashMap<>();
        for (Recipe recipe : recipes) {
            List<String> normalizedNames = ingredientAliasService.extractAndNormalizeIngredientNames(recipe.getIngredients());
            for (String name : normalizedNames) {
                ingredientCount.put(name, ingredientCount.getOrDefault(name, 0) + 1);
            }
        }

        return ingredientCount.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
    }

    public void refreshHotIngredientsCache() {
        redisTemplate.delete(HOT_INGREDIENTS_KEY);
    }
}
