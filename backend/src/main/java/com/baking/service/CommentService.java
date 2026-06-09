package com.baking.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baking.entity.Comment;
import com.baking.mapper.CommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final RecipeService recipeService;

    public IPage<Comment> getComments(Long recipeId, int pageNum, int pageSize) {
        Page<Comment> page = new Page<>(pageNum, pageSize);
        IPage<Comment> parentPage = commentMapper.selectParentComments(page, recipeId);

        for (Comment parent : parentPage.getRecords()) {
            List<Comment> replies = commentMapper.selectReplies(parent.getId());
            parent.setReplies(replies);
        }

        return parentPage;
    }

    @Transactional
    public Comment addComment(Comment comment) {
        comment.setLikeCount(0);
        commentMapper.insert(comment);
        recipeService.incrementCommentCount(comment.getRecipeId(), 1);
        return comment;
    }

    public void likeComment(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment != null) {
            comment.setLikeCount(comment.getLikeCount() + 1);
            commentMapper.updateById(comment);
        }
    }
}
