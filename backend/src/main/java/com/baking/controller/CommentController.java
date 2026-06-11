package com.baking.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baking.common.Result;
import com.baking.dto.CommentSummaryDTO;
import com.baking.entity.Comment;
import com.baking.entity.UserAchievement;
import com.baking.service.AchievementService;
import com.baking.service.CommentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final AchievementService achievementService;

    @GetMapping
    public Result<IPage<Comment>> getComments(
            @RequestParam Long recipeId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(commentService.getComments(recipeId, pageNum, pageSize));
    }

    @GetMapping("/summary")
    public Result<CommentSummaryDTO> getCommentSummary(@RequestParam Long recipeId) {
        return Result.success(commentService.getCommentSummary(recipeId));
    }

    @PostMapping
    public Result<Map<String, Object>> addComment(@RequestBody CommentRequest request) {
        Comment comment = new Comment();
        comment.setRecipeId(request.getRecipeId());
        comment.setUserId(request.getUserId());
        comment.setContent(request.getContent());
        comment.setParentId(request.getParentId());
        Comment createdComment = commentService.addComment(comment);

        List<UserAchievement> newlyUnlocked = achievementService.checkIn(request.getUserId());
        int streakDays = achievementService.getStreakDays(request.getUserId());

        Map<String, Object> result = new HashMap<>();
        result.put("comment", createdComment);
        result.put("newlyUnlocked", newlyUnlocked);
        result.put("streakDays", streakDays);
        return Result.success(result);
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
