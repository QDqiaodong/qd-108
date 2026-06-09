package com.baking.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baking.common.Result;
import com.baking.entity.Comment;
import com.baking.service.CommentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public Result<IPage<Comment>> getComments(
            @RequestParam Long recipeId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(commentService.getComments(recipeId, pageNum, pageSize));
    }

    @PostMapping
    public Result<Comment> addComment(@RequestBody CommentRequest request) {
        Comment comment = new Comment();
        comment.setRecipeId(request.getRecipeId());
        comment.setUserId(request.getUserId());
        comment.setContent(request.getContent());
        comment.setParentId(request.getParentId());
        return Result.success(commentService.addComment(comment));
    }

    @PostMapping("/{id}/like")
    public Result<Void> likeComment(@PathVariable Long id) {
        commentService.likeComment(id);
        return Result.success();
    }

    @Data
    public static class CommentRequest {
        private Long recipeId;
        private Long userId;
        private String content;
        private Long parentId;
    }
}
