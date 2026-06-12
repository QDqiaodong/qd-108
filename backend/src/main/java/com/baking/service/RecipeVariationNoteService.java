package com.baking.service;

import com.baking.entity.RecipeVariationNote;
import com.baking.mapper.RecipeVariationNoteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecipeVariationNoteService {

    private final RecipeVariationNoteMapper variationNoteMapper;

    public Map<String, List<RecipeVariationNote>> getVariationNotesGrouped(Long recipeId) {
        List<RecipeVariationNote> notes = variationNoteMapper.selectByRecipeId(recipeId);
        return notes.stream().collect(Collectors.groupingBy(RecipeVariationNote::getTopic));
    }

    public List<String> getTopics(Long recipeId) {
        return variationNoteMapper.selectTopicsByRecipeId(recipeId);
    }

    public RecipeVariationNote addVariationNote(RecipeVariationNote note) {
        note.setLikeCount(0);
        variationNoteMapper.insert(note);
        return variationNoteMapper.selectById(note.getId());
    }

    public void likeVariationNote(Long id) {
        RecipeVariationNote note = variationNoteMapper.selectById(id);
        if (note != null) {
            note.setLikeCount(note.getLikeCount() + 1);
            variationNoteMapper.updateById(note);
        }
    }
}
