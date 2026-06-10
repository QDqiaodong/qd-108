package com.baking.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baking.common.Result;
import com.baking.entity.IngredientAlias;
import com.baking.mapper.IngredientAliasMapper;
import com.baking.service.IngredientAliasService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ingredient-aliases")
@RequiredArgsConstructor
public class IngredientAliasController {

    private final IngredientAliasMapper ingredientAliasMapper;
    private final IngredientAliasService ingredientAliasService;

    @GetMapping
    public Result<IPage<IngredientAlias>> getAliases(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword) {
        Page<IngredientAlias> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<IngredientAlias> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            wrapper.like(IngredientAlias::getCanonicalName, keyword)
                    .or()
                    .like(IngredientAlias::getAliasName, keyword);
        }
        wrapper.orderByAsc(IngredientAlias::getCanonicalName);
        return Result.success(ingredientAliasMapper.selectPage(page, wrapper));
    }

    @GetMapping("/canonical-names")
    public Result<List<String>> getCanonicalNames() {
        return Result.success(ingredientAliasMapper.findAllCanonicalNames());
    }

    @GetMapping("/normalize")
    public Result<String> normalize(@RequestParam String name) {
        return Result.success(ingredientAliasService.normalize(name));
    }

    @PostMapping
    public Result<IngredientAlias> createAlias(@RequestBody AliasRequest request) {
        IngredientAlias alias = new IngredientAlias();
        alias.setCanonicalName(request.getCanonicalName());
        alias.setAliasName(request.getAliasName());
        ingredientAliasMapper.insert(alias);
        ingredientAliasService.refreshAliasCache();
        return Result.success(alias);
    }

    @PutMapping("/{id}")
    public Result<IngredientAlias> updateAlias(@PathVariable Long id, @RequestBody AliasRequest request) {
        IngredientAlias alias = ingredientAliasMapper.selectById(id);
        if (alias == null) {
            return Result.error("别名不存在");
        }
        alias.setCanonicalName(request.getCanonicalName());
        alias.setAliasName(request.getAliasName());
        ingredientAliasMapper.updateById(alias);
        ingredientAliasService.refreshAliasCache();
        return Result.success(alias);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteAlias(@PathVariable Long id) {
        ingredientAliasMapper.deleteById(id);
        ingredientAliasService.refreshAliasCache();
        return Result.success();
    }

    @PostMapping("/refresh-cache")
    public Result<Void> refreshCache() {
        ingredientAliasService.refreshAliasCache();
        return Result.success();
    }

    @Data
    public static class AliasRequest {
        private String canonicalName;
        private String aliasName;
    }
}
