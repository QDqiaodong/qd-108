package com.baking.service;

import com.baking.dto.StepDurationParseResult;
import com.baking.entity.Recipe;
import com.baking.mapper.RecipeMapper;
import com.baking.util.StepDurationParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StepDurationService {

    private final RecipeMapper recipeMapper;

    public StepDurationParseResult parseRecipeSteps(Long recipeId) {
        Recipe recipe = recipeMapper.selectById(recipeId);
        if (recipe == null) {
            StepDurationParseResult result = new StepDurationParseResult();
            result.setStages(new java.util.ArrayList<>());
            result.setTotalDurationMinutes(0);
            result.setParsedStepCount(0);
            result.setTotalStepCount(0);
            return result;
        }
        return parseSteps(recipe.getSteps());
    }

    public StepDurationParseResult parseSteps(String stepsJson) {
        return StepDurationParser.parse(stepsJson);
    }

    public StepDurationParseResult parseStepList(List<String> stepTexts) {
        StepDurationParseResult result = new StepDurationParseResult();
        List<StepDurationParseResult.StageInfo> stages = StepDurationParser.parseStepList(stepTexts);

        int totalMinutes = stages.stream()
                .mapToInt(StepDurationParseResult.StageInfo::getDurationMinutes)
                .sum();

        result.setStages(stages);
        result.setTotalDurationMinutes(totalMinutes);
        result.setParsedStepCount(stages.size());
        result.setTotalStepCount(stepTexts != null ? stepTexts.size() : 0);

        return result;
    }

    public StepDurationParseResult.StageInfo parseSingleStep(String stepText) {
        return StepDurationParser.parseStep(stepText, 0);
    }

    public String formatDuration(int minutes) {
        if (minutes < 60) {
            return minutes + "分钟";
        }
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;
        if (remainingMinutes == 0) {
            return hours + "小时";
        }
        return hours + "小时" + remainingMinutes + "分钟";
    }
}
