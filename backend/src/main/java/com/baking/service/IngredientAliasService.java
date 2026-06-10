package com.baking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baking.entity.IngredientAlias;
import com.baking.mapper.IngredientAliasMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngredientAliasService {

    private final IngredientAliasMapper ingredientAliasMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String ALIAS_CACHE_KEY = "ingredient:alias:map";
    private static final long ALIAS_CACHE_TTL = 86400;

    private final Map<String, String> localCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            loadAliasCache();
        } catch (Exception e) {
            log.warn("Failed to initialize ingredient alias cache on startup", e);
        }
    }

    @SuppressWarnings("unchecked")
    public void loadAliasCache() {
        try {
            Map<String, String> cached = (Map<String, String>) redisTemplate.opsForValue().get(ALIAS_CACHE_KEY);
            if (cached != null && !cached.isEmpty()) {
                localCache.putAll(cached);
                return;
            }
        } catch (Exception e) {
            log.warn("Failed to load alias cache from Redis, loading from database", e);
        }

        refreshAliasCache();
    }

    public void refreshAliasCache() {
        List<IngredientAlias> allAliases = ingredientAliasMapper.selectList(
                new LambdaQueryWrapper<>()
        );

        Map<String, String> aliasMap = new HashMap<>();
        for (IngredientAlias alias : allAliases) {
            if (alias.getCanonicalName() != null && alias.getAliasName() != null) {
                aliasMap.put(alias.getAliasName(), alias.getCanonicalName());
            }
        }

        localCache.clear();
        localCache.putAll(aliasMap);

        try {
            redisTemplate.opsForValue().set(ALIAS_CACHE_KEY, aliasMap, ALIAS_CACHE_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Failed to cache alias map to Redis", e);
        }
    }

    public String normalize(String ingredientName) {
        if (ingredientName == null || ingredientName.trim().isEmpty()) {
            return ingredientName;
        }

        String trimmed = ingredientName.trim();
        String canonical = localCache.get(trimmed);
        if (canonical != null) {
            return canonical;
        }

        canonical = ingredientAliasMapper.findCanonicalName(trimmed);
        if (canonical != null) {
            localCache.put(trimmed, canonical);
            return canonical;
        }

        return trimmed;
    }

    public List<String> normalizeList(List<String> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return ingredients;
        }

        List<String> result = new ArrayList<>(ingredients.size());
        for (String ingredient : ingredients) {
            result.add(normalize(ingredient));
        }
        return result;
    }

    public List<Map<String, Object>> parseIngredientsFromJson(String ingredientsJson) {
        if (ingredientsJson == null || ingredientsJson.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(ingredientsJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.error("Failed to parse ingredients JSON: {}", ingredientsJson, e);
            return new ArrayList<>();
        }
    }

    public List<String> extractIngredientNames(String ingredientsJson) {
        List<Map<String, Object>> ingredients = parseIngredientsFromJson(ingredientsJson);
        List<String> names = new ArrayList<>();
        for (Map<String, Object> ingredient : ingredients) {
            Object name = ingredient.get("name");
            if (name != null) {
                names.add(name.toString());
            }
        }
        return names;
    }

    public List<String> extractAndNormalizeIngredientNames(String ingredientsJson) {
        List<String> names = extractIngredientNames(ingredientsJson);
        return normalizeList(names);
    }

    public Map<String, Integer> countIngredients(List<String> ingredientNames) {
        Map<String, Integer> countMap = new HashMap<>();
        if (ingredientNames == null || ingredientNames.isEmpty()) {
            return countMap;
        }

        for (String name : ingredientNames) {
            String normalized = normalize(name);
            countMap.put(normalized, countMap.getOrDefault(normalized, 0) + 1);
        }
        return countMap;
    }

    public String normalizeIngredientsJson(String ingredientsJson) {
        if (ingredientsJson == null || ingredientsJson.trim().isEmpty()) {
            return ingredientsJson;
        }

        try {
            List<Map<String, Object>> ingredients = parseIngredientsFromJson(ingredientsJson);
            for (Map<String, Object> ingredient : ingredients) {
                Object name = ingredient.get("name");
                if (name != null) {
                    ingredient.put("name", normalize(name.toString()));
                }
            }
            return objectMapper.writeValueAsString(ingredients);
        } catch (Exception e) {
            log.error("Failed to normalize ingredients JSON", e);
            return ingredientsJson;
        }
    }
}
