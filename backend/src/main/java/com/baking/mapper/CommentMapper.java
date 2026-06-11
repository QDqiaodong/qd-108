package com.baking.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baking.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    @Select("SELECT c.*, u.nickname as userName, u.avatar as userAvatar " +
            "FROM comment c LEFT JOIN user u ON c.user_id = u.id " +
            "WHERE c.recipe_id = #{recipeId} AND c.parent_id IS NULL " +
            "ORDER BY c.created_at DESC")
    IPage<Comment> selectParentComments(Page<Comment> page, @Param("recipeId") Long recipeId);

    @Select("SELECT c.*, u.nickname as userName, u.avatar as userAvatar " +
            "FROM comment c LEFT JOIN user u ON c.user_id = u.id " +
            "WHERE c.parent_id = #{parentId} " +
            "ORDER BY c.created_at ASC")
    java.util.List<Comment> selectReplies(@Param("parentId") Long parentId);

    @Select("SELECT content FROM comment WHERE recipe_id = #{recipeId} AND content IS NOT NULL AND content != ''")
    java.util.List<String> selectAllCommentContents(@Param("recipeId") Long recipeId);
}
