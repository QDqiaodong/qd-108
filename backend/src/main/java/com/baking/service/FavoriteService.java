package com.baking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baking.entity.Favorite;
import com.baking.entity.FavoriteFolder;
import com.baking.mapper.FavoriteFolderMapper;
import com.baking.mapper.FavoriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final FavoriteFolderMapper favoriteFolderMapper;
    private final RecipeService recipeService;

    public boolean isFavorited(Long userId, Long recipeId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId);
        wrapper.eq(Favorite::getRecipeId, recipeId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }

    @Transactional
    public boolean addFavorite(Long userId, Long recipeId, Long folderId) {
        if (isFavorited(userId, recipeId)) {
            return false;
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setRecipeId(recipeId);
        favorite.setFolderId(folderId);
        try {
            favoriteMapper.insert(favorite);
        } catch (DuplicateKeyException e) {
            return false;
        }
        recipeService.incrementFavoriteCount(recipeId, 1);
        return true;
    }

    @Transactional
    public boolean removeFavorite(Long userId, Long recipeId) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId);
        wrapper.eq(Favorite::getRecipeId, recipeId);
        int deleted = favoriteMapper.delete(wrapper);
        if (deleted > 0) {
            recipeService.incrementFavoriteCount(recipeId, -1);
            return true;
        }
        return false;
    }

    public List<FavoriteFolder> getUserFolders(Long userId) {
        LambdaQueryWrapper<FavoriteFolder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FavoriteFolder::getUserId, userId);
        wrapper.orderByDesc(FavoriteFolder::getCreatedAt);
        return favoriteFolderMapper.selectList(wrapper);
    }

    public FavoriteFolder createFolder(Long userId, String name, String description) {
        FavoriteFolder folder = new FavoriteFolder();
        folder.setUserId(userId);
        folder.setName(name);
        folder.setDescription(description);
        favoriteFolderMapper.insert(folder);
        return folder;
    }

    public void deleteFolder(Long userId, Long folderId) {
        LambdaQueryWrapper<FavoriteFolder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FavoriteFolder::getId, folderId);
        wrapper.eq(FavoriteFolder::getUserId, userId);
        favoriteFolderMapper.delete(wrapper);
    }
}
