package com.baking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baking.entity.Category;
import com.baking.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY = "category:all";
    private static final long CACHE_TTL = 7200;

    @SuppressWarnings("unchecked")
    public List<Category> getAllCategories() {
        List<Category> cached = (List<Category>) redisTemplate.opsForValue().get(CACHE_KEY);
        if (cached != null) {
            return cached;
        }

        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getSortOrder);
        List<Category> categories = categoryMapper.selectList(wrapper);

        redisTemplate.opsForValue().set(CACHE_KEY, categories, CACHE_TTL, TimeUnit.SECONDS);
        return categories;
    }

    public Category getById(Long id) {
        return categoryMapper.selectById(id);
    }
}
