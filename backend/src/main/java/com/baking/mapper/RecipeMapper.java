package com.baking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baking.entity.Recipe;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RecipeMapper extends BaseMapper<Recipe> {

    @Select("SELECT r.*, c.name as categoryName, u.nickname as authorName, u.avatar as authorAvatar " +
            "FROM recipe r LEFT JOIN category c ON r.category_id = c.id " +
            "LEFT JOIN user u ON r.user_id = u.id " +
            "WHERE r.status = 1 " +
            "AND (#{categoryId} IS NULL OR r.category_id = #{categoryId}) " +
            "AND (#{keyword} IS NULL OR #{keyword} = '' " +
            "OR r.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR r.ingredients LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY r.created_at DESC")
    IPage<Recipe> selectRecipePage(Page<Recipe> page,
                                    @Param("categoryId") Long categoryId,
                                    @Param("keyword") String keyword);

    @Select("SELECT r.*, c.name as categoryName, u.nickname as authorName, u.avatar as authorAvatar " +
            "FROM recipe r LEFT JOIN category c ON r.category_id = c.id " +
            "LEFT JOIN user u ON r.user_id = u.id " +
            "WHERE r.status = 1 " +
            "AND (#{categoryId} IS NULL OR r.category_id = #{categoryId}) " +
            "AND (#{keyword} IS NULL OR #{keyword} = '' " +
            "OR r.title LIKE CONCAT('%', #{keyword}, '%') " +
            "OR r.ingredients LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY (" +
            "  COALESCE(r.view_count, 0) * 1 + " +
            "  COALESCE(r.favorite_count, 0) * 8 + " +
            "  COALESCE(r.comment_count, 0) * 6 + " +
            "  COALESCE(r.like_count, 0) * 4 + " +
            "  COALESCE(r.trial_receipt_count, 0) * 10" +
            ") DESC, r.created_at DESC")
    IPage<Recipe> selectRecipePageHot(Page<Recipe> page,
                                       @Param("categoryId") Long categoryId,
                                       @Param("keyword") String keyword);

    @Select("SELECT r.*, c.name as categoryName, u.nickname as authorName, u.avatar as authorAvatar " +
            "FROM recipe r LEFT JOIN category c ON r.category_id = c.id " +
            "LEFT JOIN user u ON r.user_id = u.id " +
            "WHERE r.id = #{id} AND r.status = 1")
    Recipe selectRecipeDetail(@Param("id") Long id);

    @Select("SELECT r.*, c.name as categoryName, u.nickname as authorName, u.avatar as authorAvatar " +
            "FROM recipe r LEFT JOIN category c ON r.category_id = c.id " +
            "LEFT JOIN user u ON r.user_id = u.id " +
            "WHERE r.status = 1 " +
            "ORDER BY r.view_count DESC, r.favorite_count DESC " +
            "LIMIT #{limit}")
    java.util.List<Recipe> selectHotRecipes(@Param("limit") Integer limit);

    @Select("SELECT r.*, c.name as categoryName, u.nickname as authorName, u.avatar as authorAvatar " +
            "FROM recipe r LEFT JOIN category c ON r.category_id = c.id " +
            "LEFT JOIN user u ON r.user_id = u.id " +
            "WHERE r.status = 1 " +
            "AND (#{categoryId} IS NULL OR r.category_id = #{categoryId}) " +
            "ORDER BY (" +
            "  COALESCE(r.view_count, 0) * 1 + " +
            "  COALESCE(r.favorite_count, 0) * 8 + " +
            "  COALESCE(r.comment_count, 0) * 6 + " +
            "  COALESCE(r.like_count, 0) * 4 + " +
            "  COALESCE(r.trial_receipt_count, 0) * 10" +
            ") DESC, r.created_at DESC " +
            "LIMIT #{limit}")
    java.util.List<Recipe> selectHotRecipeCandidates(@Param("limit") Integer limit,
                                                      @Param("categoryId") Long categoryId);

    @Select("SELECT r.*, c.name as categoryName, u.nickname as authorName, u.avatar as authorAvatar " +
            "FROM favorite f LEFT JOIN recipe r ON f.recipe_id = r.id " +
            "LEFT JOIN category c ON r.category_id = c.id " +
            "LEFT JOIN user u ON r.user_id = u.id " +
            "WHERE f.user_id = #{userId} AND r.status = 1 " +
            "ORDER BY f.created_at DESC")
    IPage<Recipe> selectFavoriteRecipes(Page<Recipe> page, @Param("userId") Long userId);
}
