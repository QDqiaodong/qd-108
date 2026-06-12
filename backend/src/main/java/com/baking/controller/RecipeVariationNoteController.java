package com.baking.controller;

import com.baking.common.Result;
import com.baking.entity.RecipeVariationNote;
import com.baking.service.RecipeVariationNoteService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/variation-notes")
@RequiredArgsConstructor
public class RecipeVariationNoteController {

    private final RecipeVariationNoteService variationNoteService;

    @GetMapping
    public Result<Map<String, List<RecipeVariationNote>>> getVariationNotes(@RequestParam Long recipeId) {
        return Result.success(variationNoteService.getVariationNotesGrouped(recipeId));
    }

    @GetMapping("/topics")
    public Result<List<String>> getTopics(@RequestParam Long recipeId) {
        return Result.success(variationNoteService.getTopics(recipeId));
    }

    @PostMapping
    public Result<RecipeVariationNote> addVariationNote(@RequestBody VariationNoteRequest request) {
        RecipeVariationNote note = new RecipeVariationNote();
        note.setRecipeId(request.getRecipeId());
        note.setUserId(request.getUserId());
        note.setTopic(request.getTopic());
        note.setContent(request.getContent());
        return Result.success(variationNoteService.addVariationNote(note));
    }

    @PostMapping("/{id}/like")
    public Result<Void> likeVariationNote(@PathVariable Long id) {
        variationNoteService.likeVariationNote(id);
        return Result.success();
    }

    @Data
    public static class VariationNoteRequest {
        private Long recipeId;
        private Long userId;
        private String topic;
        private String content;
    }
}
