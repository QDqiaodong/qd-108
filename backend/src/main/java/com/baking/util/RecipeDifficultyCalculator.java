package com.baking.util;

import com.baking.dto.StepDurationParseResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeDifficultyCalculator {

    private static final double W_INGREDIENTS = 0.25;
    private static final double W_STEPS = 0.25;
    private static final double W_FERMENTATION = 0.25;
    private static final double W_TEMPERATURE = 0.25;

    public static class DifficultyResult {
        private int difficulty;
        private double totalScore;
        private double ingredientScore;
        private double stepScore;
        private double fermentationScore;
        private double temperatureScore;
        private int ingredientCount;
        private int stepCount;
        private int fermentationCount;
        private int temperatureStageCount;

        public int getDifficulty() { return difficulty; }
        public void setDifficulty(int difficulty) { this.difficulty = difficulty; }
        public double getTotalScore() { return totalScore; }
        public void setTotalScore(double totalScore) { this.totalScore = totalScore; }
        public double getIngredientScore() { return ingredientScore; }
        public void setIngredientScore(double ingredientScore) { this.ingredientScore = ingredientScore; }
        public double getStepScore() { return stepScore; }
        public void setStepScore(double stepScore) { this.stepScore = stepScore; }
        public double getFermentationScore() { return fermentationScore; }
        public void setFermentationScore(double fermentationScore) { this.fermentationScore = fermentationScore; }
        public double getTemperatureScore() { return temperatureScore; }
        public void setTemperatureScore(double temperatureScore) { this.temperatureScore = temperatureScore; }
        public int getIngredientCount() { return ingredientCount; }
        public void setIngredientCount(int ingredientCount) { this.ingredientCount = ingredientCount; }
        public int getStepCount() { return stepCount; }
        public void setStepCount(int stepCount) { this.stepCount = stepCount; }
        public int getFermentationCount() { return fermentationCount; }
        public void setFermentationCount(int fermentationCount) { this.fermentationCount = fermentationCount; }
        public int getTemperatureStageCount() { return temperatureStageCount; }
        public void setTemperatureStageCount(int temperatureStageCount) { this.temperatureStageCount = temperatureStageCount; }
    }

    public static DifficultyResult calculate(String ingredientsJson, String stepsJson, Integer bakeTemp) {
        DifficultyResult result = new DifficultyResult();

        int ingredientCount = parseIngredientCount(ingredientsJson);
        List<Map<String, String>> steps = parseStepsJson(stepsJson);
        int stepCount = steps.size();

        result.setIngredientCount(ingredientCount);
        result.setStepCount(stepCount);

        double ingredientScore = calculateIngredientScore(ingredientCount);
        double stepScore = calculateStepScore(steps);
        double fermentationScore = calculateFermentationScore(steps, result);
        double temperatureScore = calculateTemperatureScore(steps, bakeTemp, result);

        result.setIngredientScore(ingredientScore);
        result.setStepScore(stepScore);
        result.setFermentationScore(fermentationScore);
        result.setTemperatureScore(temperatureScore);

        double totalScore = ingredientScore * W_INGREDIENTS
                + stepScore * W_STEPS
                + fermentationScore * W_FERMENTATION
                + temperatureScore * W_TEMPERATURE;

        result.setTotalScore(totalScore);
        result.setDifficulty(determineDifficulty(totalScore));

        return result;
    }

    private static int parseIngredientCount(String ingredientsJson) {
        if (ingredientsJson == null || ingredientsJson.trim().isEmpty()) {
            return 0;
        }
        try {
            if (ingredientsJson.startsWith("[") && ingredientsJson.endsWith("]")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<Object> rawList = mapper.readValue(ingredientsJson, List.class);
                int count = 0;
                for (Object item : rawList) {
                    if (item instanceof Map) {
                        Map<String, Object> map = (Map<String, Object>) item;
                        Object name = map.get("name");
                        if (name != null && !String.valueOf(name).trim().isEmpty()) {
                            count++;
                        }
                    }
                }
                return count;
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, String>> parseStepsJson(String stepsJson) {
        List<Map<String, String>> steps = new ArrayList<>();
        if (stepsJson == null || stepsJson.trim().isEmpty()) {
            return steps;
        }
        try {
            if (stepsJson.startsWith("[") && stepsJson.endsWith("]")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<Object> rawList = mapper.readValue(stepsJson, List.class);
                for (Object item : rawList) {
                    if (item instanceof Map) {
                        Map<String, String> stepMap = new HashMap<>();
                        Map<String, Object> rawMap = (Map<String, Object>) item;
                        Object desc = rawMap.get("description");
                        if (desc != null && !String.valueOf(desc).trim().isEmpty()) {
                            stepMap.put("description", String.valueOf(desc));
                            steps.add(stepMap);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Map<String, String> stepMap = new HashMap<>();
            stepMap.put("description", stepsJson);
            steps.add(stepMap);
        }
        return steps;
    }

    private static double calculateIngredientScore(int count) {
        if (count <= 3) return 20.0;
        if (count <= 5) return 40.0;
        if (count <= 7) return 60.0;
        if (count <= 10) return 80.0;
        return 100.0;
    }

    private static double calculateStepScore(List<Map<String, String>> steps) {
        int stepCount = steps.size();
        int totalLength = 0;
        for (Map<String, String> step : steps) {
            String desc = step.get("description");
            if (desc != null) {
                totalLength += desc.length();
            }
        }
        double avgLength = stepCount > 0 ? (double) totalLength / stepCount : 0;

        double countScore;
        if (stepCount <= 3) countScore = 20.0;
        else if (stepCount <= 5) countScore = 40.0;
        else if (stepCount <= 7) countScore = 60.0;
        else if (stepCount <= 10) countScore = 80.0;
        else countScore = 100.0;

        double complexityScore;
        if (avgLength < 30) complexityScore = 20.0;
        else if (avgLength < 50) complexityScore = 40.0;
        else if (avgLength < 80) complexityScore = 60.0;
        else if (avgLength < 120) complexityScore = 80.0;
        else complexityScore = 100.0;

        return countScore * 0.6 + complexityScore * 0.4;
    }

    private static double calculateFermentationScore(List<Map<String, String>> steps, DifficultyResult result) {
        int fermentationCount = 0;
        boolean hasFirstFermentation = false;
        boolean hasSecondFermentation = false;
        boolean hasColdFermentation = false;
        boolean hasPreFerment = false;

        for (Map<String, String> step : steps) {
            String desc = step.get("description");
            if (desc == null) continue;

            if (desc.contains("中种") || desc.contains("波兰种") || desc.contains("液种") || desc.contains("烫种") || desc.contains("老面")) {
                hasPreFerment = true;
                fermentationCount++;
            }

            if (desc.contains("一发") || desc.contains("一醒") || desc.contains("第一次发酵") || desc.contains("基础发酵")) {
                if (!hasFirstFermentation) {
                    hasFirstFermentation = true;
                    fermentationCount++;
                }
                if (desc.contains("冷藏") || desc.contains("冰箱")) {
                    hasColdFermentation = true;
                }
            }

            if (desc.contains("二发") || desc.contains("二醒") || desc.contains("第二次发酵") || desc.contains("最终发酵")) {
                if (!hasSecondFermentation) {
                    hasSecondFermentation = true;
                    fermentationCount++;
                }
            }

            if (desc.contains("发酵") || desc.contains("醒发")) {
                if (!hasFirstFermentation && !hasSecondFermentation) {
                    fermentationCount++;
                }
            }
        }

        result.setFermentationCount(fermentationCount);

        double score = 0.0;
        if (hasPreFerment) score += 30.0;
        if (hasFirstFermentation) score += 25.0;
        if (hasSecondFermentation) score += 30.0;
        if (hasColdFermentation) score += 15.0;

        return Math.min(100.0, score);
    }

    private static double calculateTemperatureScore(List<Map<String, String>> steps, Integer bakeTemp, DifficultyResult result) {
        int tempStageCount = 0;
        boolean hasBakeTemp = bakeTemp != null && bakeTemp > 0;
        boolean hasMultipleTemps = false;
        boolean hasPreciseControl = false;
        boolean hasRefrigeration = false;
        boolean hasFreezing = false;

        if (hasBakeTemp) {
            tempStageCount++;
        }

        for (Map<String, String> step : steps) {
            String desc = step.get("description");
            if (desc == null) continue;

            if (desc.contains("冷藏") || desc.contains("冰箱")) {
                if (!hasRefrigeration) {
                    hasRefrigeration = true;
                    tempStageCount++;
                }
            }

            if (desc.contains("冷冻")) {
                if (!hasFreezing) {
                    hasFreezing = true;
                    tempStageCount++;
                }
            }

            if (desc.contains("℃") || desc.contains("°C") || desc.contains("度")) {
                java.util.regex.Pattern tempPattern = java.util.regex.Pattern.compile("(\\d+)\\s*(℃|°C|度)");
                java.util.regex.Matcher matcher = tempPattern.matcher(desc);
                java.util.Set<Integer> temps = new java.util.HashSet<>();
                while (matcher.find()) {
                    try {
                        temps.add(Integer.parseInt(matcher.group(1)));
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (temps.size() > 1) {
                    hasMultipleTemps = true;
                }
                if (!temps.isEmpty()) {
                    hasPreciseControl = true;
                }
            }

            if (desc.contains("转") && (desc.contains("℃") || desc.contains("°C") || desc.contains("度"))) {
                hasMultipleTemps = true;
            }
        }

        result.setTemperatureStageCount(tempStageCount);

        double score = 0.0;
        if (hasBakeTemp) score += 20.0;
        if (hasPreciseControl) score += 20.0;
        if (hasRefrigeration) score += 20.0;
        if (hasFreezing) score += 20.0;
        if (hasMultipleTemps) score += 20.0;

        return Math.min(100.0, score);
    }

    private static int determineDifficulty(double totalScore) {
        if (totalScore <= 35.0) return 1;
        if (totalScore <= 65.0) return 2;
        return 3;
    }
}
