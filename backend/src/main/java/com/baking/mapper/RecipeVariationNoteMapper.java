package com.baking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baking.entity.RecipeVariationNote;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RecipeVariationNoteMapper extends BaseMapper<RecipeVariationNote> {

    @Select("SELECT n.*, u.nickname as userName, u.avatar as userAvatar " +
            "FROM recipe_variation_note n LEFT JOIN user u ON n.user_id = u.id " +
            "WHERE n.recipe_id = #{recipeId} " +
            "ORDER BY n.topic ASC, n.created_at DESC")
    List<RecipeVariationNote> selectByRecipeId(@Param("recipeId") Long recipeId);

    @Select("SELECT DISTINCT n.topic " +
            "FROM recipe_variation_note n " +
            "WHERE n.recipe_id = #{recipeId} " +
            "ORDER BY n.topic ASC")
    List<String> selectTopicsByRecipeId(@Param("recipeId") Long recipeId);
}
