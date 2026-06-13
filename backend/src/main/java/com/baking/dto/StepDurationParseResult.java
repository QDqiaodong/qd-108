package com.baking.dto;

import lombok.Data;
import java.util.List;

@Data
public class StepDurationParseResult {

    private List<StageInfo> stages;

    private int totalDurationMinutes;

    private int parsedStepCount;

    private int totalStepCount;

    @Data
    public static class StageInfo {

        private String stageName;

        private String stageType;

        private int durationMinutes;

        private String originalText;

        private int stepIndex;

        private String timeUnit;

        private int timeValue;

        private boolean isApproximate;
    }
}
