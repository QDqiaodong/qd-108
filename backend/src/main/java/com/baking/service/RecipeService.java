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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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

    private static final double W_VIEW = 1.0;
    private static final double W_FAVORITE = 8.0;
    private static final double W_COMMENT = 6.0;
    private static final double W_LIKE = 4.0;
    private static final double W_TRIAL = 10.0;
    private static final double DECAY_GRAVITY = 1.8;

    public static double calculateBaseScore(Recipe r) {
        return nullToZero(r.getViewCount()) * W_VIEW
                + nullToZero(r.getFavoriteCount()) * W_FAVORITE
                + nullToZero(r.getCommentCount()) * W_COMMENT
                + nullToZero(r.getLikeCount()) * W_LIKE
                + nullToZero(r.getTrialReceiptCount()) * W_TRIAL;
    }

    public static double calculateDecayFactor(LocalDateTime createdAt) {
        if (createdAt == null) return 0.01;
        long hours = Duration.between(createdAt, LocalDateTime.now()).toHours();
        double days = Math.max(0, hours / 24.0);
        return 1.0 / Math.pow(1.0 + days, DECAY_GRAVITY);
    }

    public static double calculateHotScore(Recipe r) {
        double base = calculateBaseScore(r);
        double decay = calculateDecayFactor(r.getCreatedAt());
        return base * decay;
    }

    private static int nullToZero(Integer v) {
        return v == null ? 0 : v;
    }

    public IPage<Recipe> getRecipePage(int pageNum, int pageSize, Long categoryId, String keyword, String sortBy) {
        Page<Recipe> page = new Page<>(pageNum, pageSize);
        String searchKeyword = keyword;
        if (keyword != null && !keyword.trim().isEmpty()) {
            searchKeyword = ingredientAliasService.normalize(keyword.trim());
        }

        IPage<Recipe> result;
        if ("hot".equalsIgnoreCase(sortBy)) {
            result = recipeMapper.selectRecipePageHot(page, categoryId, searchKeyword);
            List<Recipe> reranked = result.getRecords().stream()
                    .sorted(Comparator.comparingDouble(RecipeService::calculateHotScore).reversed())
                    .collect(Collectors.toList());
            result.setRecords(reranked);
        } else {
            result = recipeMapper.selectRecipePage(page, categoryId, searchKeyword);
        }
        return result;
    }

    public IPage<Recipe> getRecipePage(int pageNum, int pageSize, Long categoryId, String keyword) {
        return getRecipePage(pageNum, pageSize, categoryId, keyword, null);
    }

    @SuppressWarnings("unchecked")
    public List<Recipe> getHotRecipes(int limit) {
        return getHotRecipes(limit, null);
    }

    @SuppressWarnings("unchecked")
    public List<Recipe> getHotRecipes(int limit, Long categoryId) {
        String cacheKey = categoryId == null
                ? HOT_RECIPES_KEY
                : HOT_RECIPES_KEY + ":" + categoryId;

        List<Recipe> cached = (List<Recipe>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null && cached.size() >= limit) {
            return cached.stream().limit(limit).collect(Collectors.toList());
        }

        int candidateLimit = Math.max(limit * 3, 30);
        List<Recipe> candidates = recipeMapper.selectHotRecipeCandidates(candidateLimit, categoryId);

        List<Recipe> reranked = candidates.stream()
                .sorted(Comparator.comparingDouble(RecipeService::calculateHotScore).reversed())
                .limit(candidateLimit)
                .collect(Collectors.toList());

        redisTemplate.opsForValue().set(cacheKey, reranked, HOT_RECIPES_TTL, TimeUnit.SECONDS);
        return reranked.stream().limit(limit).collect(Collectors.toList());
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
        return createRecipe(recipe, imageUrls, null, null);
    }

    @Transactional
    public Recipe createRecipe(Recipe recipe, List<String> imageUrls, List<String> thumbnailUrls, List<String> imageTypes) {
        recipe.setViewCount(0);
        recipe.setLikeCount(0);
        recipe.setCommentCount(0);
        recipe.setFavoriteCount(0);
        recipe.setTrialReceiptCount(0);
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
                if (thumbnailUrls != null && i < thumbnailUrls.size()) {
                    image.setThumbnailUrl(thumbnailUrls.get(i));
                }
                if (imageTypes != null && i < imageTypes.size()) {
                    image.setImageType(imageTypes.get(i));
                } else {
                    image.setImageType(i == 0 ? "cover" : "step");
                }
                recipeImageMapper.insert(image);
            }
        }

        clearAllHotRecipeCaches();
        redisTemplate.delete(HOT_INGREDIENTS_KEY);
        return recipe;
    }

    private void clearAllHotRecipeCaches() {
        try {
            var keys = redisTemplate.keys(HOT_RECIPES_KEY + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception ignored) {
            redisTemplate.delete(HOT_RECIPES_KEY);
        }
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
            clearAllHotRecipeCaches();
        }
    }

    public void incrementCommentCount(Long recipeId, int delta) {
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe != null) {
            recipe.setCommentCount(Math.max(0, recipe.getCommentCount() + delta));
            recipeMapper.updateById(recipe);
            clearAllHotRecipeCaches();
        }
    }

    public void incrementTrialReceiptCount(Long recipeId, int delta) {
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe != null) {
            recipe.setTrialReceiptCount(Math.max(0, recipe.getTrialReceiptCount() + delta));
            recipeMapper.updateById(recipe);
            clearAllHotRecipeCaches();
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

    public List<Recipe> getRecipesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return recipeMapper.selectRecipesByIds(ids);
    }
}
