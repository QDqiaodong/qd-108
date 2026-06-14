package com.baking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baking.entity.TrialReceipt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TrialReceiptMapper extends BaseMapper<TrialReceipt> {

    @Select("SELECT t.*, u.nickname as userName, u.avatar as userAvatar " +
            "FROM trial_receipt t LEFT JOIN user u ON t.user_id = u.id " +
            "WHERE t.recipe_id = #{recipeId} " +
            "ORDER BY t.created_at DESC")
    IPage<TrialReceipt> selectByRecipeId(Page<TrialReceipt> page, @Param("recipeId") Long recipeId);

    @Select("SELECT CONCAT_WS(' ', taste_comment, temp_adjustment, mold_difference, notes) " +
            "FROM trial_receipt " +
            "WHERE recipe_id = #{recipeId} " +
            "AND (taste_comment IS NOT NULL OR temp_adjustment IS NOT NULL OR mold_difference IS NOT NULL OR notes IS NOT NULL)")
    List<String> selectAllTextContents(@Param("recipeId") Long recipeId);
}
