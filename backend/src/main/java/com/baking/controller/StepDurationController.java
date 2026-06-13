package com.baking.controller;

import com.baking.common.Result;
import com.baking.dto.StepDurationParseResult;
import com.baking.service.StepDurationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/step-duration")
@RequiredArgsConstructor
public class StepDurationController {

    private final StepDurationService stepDurationService;

    @GetMapping("/recipe/{recipeId}")
    public Result<StepDurationParseResult> parseRecipeSteps(@PathVariable Long recipeId) {
        return Result.success(stepDurationService.parseRecipeSteps(recipeId));
    }

    @PostMapping("/parse")
    public Result<StepDurationParseResult> parseSteps(@RequestBody ParseRequest request) {
        if (request.getStepsJson() != null && !request.getStepsJson().trim().isEmpty()) {
            return Result.success(stepDurationService.parseSteps(request.getStepsJson()));
        }
        if (request.getSteps() != null && !request.getSteps().isEmpty()) {
            return Result.success(stepDurationService.parseStepList(request.getSteps()));
        }
        StepDurationParseResult emptyResult = new StepDurationParseResult();
        emptyResult.setStages(new java.util.ArrayList<>());
        emptyResult.setTotalDurationMinutes(0);
        emptyResult.setParsedStepCount(0);
        emptyResult.setTotalStepCount(0);
        return Result.success(emptyResult);
    }

    @PostMapping("/parse-step")
    public Result<StepDurationParseResult.StageInfo> parseSingleStep(@RequestBody ParseStepRequest request) {
        return Result.success(stepDurationService.parseSingleStep(request.getText()));
    }

    @Data
    public static class ParseRequest {
        private String stepsJson;
        private List<String> steps;
    }

    @Data
    public static class ParseStepRequest {
        private String text;
    }
}
