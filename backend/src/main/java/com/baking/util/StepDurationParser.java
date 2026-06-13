package com.baking.util;

import com.baking.dto.StepDurationParseResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StepDurationParser {

    private static final Map<String, String> STAGE_TYPE_MAP = new HashMap<>();
    private static final Map<String, Integer> TIME_UNIT_TO_MINUTES = new HashMap<>();
    private static final List<Pattern> TIME_PATTERNS = new ArrayList<>();

    static {
        STAGE_TYPE_MAP.put("冷藏", "refrigeration");
        STAGE_TYPE_MAP.put("冰箱", "refrigeration");
        STAGE_TYPE_MAP.put("冷冻", "freezing");
        STAGE_TYPE_MAP.put("一发", "first_fermentation");
        STAGE_TYPE_MAP.put("一醒", "first_fermentation");
        STAGE_TYPE_MAP.put("第一次发酵", "first_fermentation");
        STAGE_TYPE_MAP.put("基础发酵", "first_fermentation");
        STAGE_TYPE_MAP.put("二发", "second_fermentation");
        STAGE_TYPE_MAP.put("二醒", "second_fermentation");
        STAGE_TYPE_MAP.put("第二次发酵", "second_fermentation");
        STAGE_TYPE_MAP.put("最终发酵", "second_fermentation");
        STAGE_TYPE_MAP.put("发酵", "fermentation");
        STAGE_TYPE_MAP.put("醒发", "fermentation");
        STAGE_TYPE_MAP.put("松弛", "relaxation");
        STAGE_TYPE_MAP.put("静置", "relaxation");
        STAGE_TYPE_MAP.put("饧", "relaxation");
        STAGE_TYPE_MAP.put("醒", "relaxation");
        STAGE_TYPE_MAP.put("烘烤", "baking");
        STAGE_TYPE_MAP.put("烤", "baking");
        STAGE_TYPE_MAP.put("烘焙", "baking");
        STAGE_TYPE_MAP.put("烤制", "baking");
        STAGE_TYPE_MAP.put("冷却", "cooling");
        STAGE_TYPE_MAP.put("晾凉", "cooling");
        STAGE_TYPE_MAP.put("放凉", "cooling");
        STAGE_TYPE_MAP.put("冷藏过夜", "refrigeration");
        STAGE_TYPE_MAP.put("冷藏一夜", "refrigeration");
        STAGE_TYPE_MAP.put("蒸", "steaming");
        STAGE_TYPE_MAP.put("煮", "boiling");
        STAGE_TYPE_MAP.put("煎", "frying");
        STAGE_TYPE_MAP.put("炸", "frying");
        STAGE_TYPE_MAP.put("打发", "whipping");
        STAGE_TYPE_MAP.put("搅拌", "mixing");
        STAGE_TYPE_MAP.put("揉面", "kneading");
        STAGE_TYPE_MAP.put("浸泡", "soaking");
        STAGE_TYPE_MAP.put("泡", "soaking");
        STAGE_TYPE_MAP.put("腌制", "marinating");
        STAGE_TYPE_MAP.put("腌", "marinating");

        TIME_UNIT_TO_MINUTES.put("秒", 1);
        TIME_UNIT_TO_MINUTES.put("秒钟", 1);
        TIME_UNIT_TO_MINUTES.put("分", 1);
        TIME_UNIT_TO_MINUTES.put("分钟", 1);
        TIME_UNIT_TO_MINUTES.put("min", 1);
        TIME_UNIT_TO_MINUTES.put("minute", 1);
        TIME_UNIT_TO_MINUTES.put("minutes", 1);
        TIME_UNIT_TO_MINUTES.put("时", 60);
        TIME_UNIT_TO_MINUTES.put("小时", 60);
        TIME_UNIT_TO_MINUTES.put("h", 60);
        TIME_UNIT_TO_MINUTES.put("hour", 60);
        TIME_UNIT_TO_MINUTES.put("hours", 60);
        TIME_UNIT_TO_MINUTES.put("天", 1440);
        TIME_UNIT_TO_MINUTES.put("日", 1440);
        TIME_UNIT_TO_MINUTES.put("day", 1440);
        TIME_UNIT_TO_MINUTES.put("days", 1440);
        TIME_UNIT_TO_MINUTES.put("夜", 480);
        TIME_UNIT_TO_MINUTES.put("晚", 480);
        TIME_UNIT_TO_MINUTES.put("晚上", 480);
        TIME_UNIT_TO_MINUTES.put("过夜", 480);
        TIME_UNIT_TO_MINUTES.put("一夜", 480);
        TIME_UNIT_TO_MINUTES.put("整晚", 480);

        TIME_PATTERNS.add(Pattern.compile("(约|大概|大约|左右|差不多)?\\s*(\\d+(?:\\.\\d+)?)\\s*(秒|秒钟|分|分钟|min|minutes?|时|小时|h|hours?|天|日|days?)\\s*(左右|大概|大约|约)?", Pattern.CASE_INSENSITIVE));
        TIME_PATTERNS.add(Pattern.compile("(冷藏|放|静置|醒|饧|腌|泡)\\s*(一夜|一晚|一晚上|过夜|一整天|一天)"));
        TIME_PATTERNS.add(Pattern.compile("(\\d+)\\s*个?\\s*(半)?\\s*(小时|分钟)"));
        TIME_PATTERNS.add(Pattern.compile("(\\d+)\\s*[:：]\\s*(\\d{2})"));
    }

    public static StepDurationParseResult parse(String stepsJson) {
        StepDurationParseResult result = new StepDurationParseResult();
        List<StepDurationParseResult.StageInfo> stages = new ArrayList<>();
        int totalMinutes = 0;
        int parsedCount = 0;

        if (stepsJson == null || stepsJson.trim().isEmpty()) {
            result.setStages(stages);
            result.setTotalDurationMinutes(0);
            result.setParsedStepCount(0);
            result.setTotalStepCount(0);
            return result;
        }

        List<Map<String, String>> steps = parseStepsJson(stepsJson);
        result.setTotalStepCount(steps.size());

        for (int i = 0; i < steps.size(); i++) {
            Map<String, String> step = steps.get(i);
            String description = step.get("description");
            if (description == null || description.trim().isEmpty()) {
                continue;
            }

            StepDurationParseResult.StageInfo stage = parseStep(description, i);
            if (stage != null) {
                stages.add(stage);
                totalMinutes += stage.getDurationMinutes();
                parsedCount++;
            }
        }

        result.setStages(stages);
        result.setTotalDurationMinutes(totalMinutes);
        result.setParsedStepCount(parsedCount);
        return result;
    }

    public static StepDurationParseResult.StageInfo parseStep(String stepText, int stepIndex) {
        if (stepText == null || stepText.trim().isEmpty()) {
            return null;
        }

        String normalizedText = stepText.trim();

        for (Pattern pattern : TIME_PATTERNS) {
            Matcher matcher = pattern.matcher(normalizedText);
            if (matcher.find()) {
                StepDurationParseResult.StageInfo stage = new StepDurationParseResult.StageInfo();
                stage.setStepIndex(stepIndex);
                stage.setOriginalText(matcher.group());

                boolean isApproximate = matcher.group(1) != null &&
                        (matcher.group(1).contains("约") || matcher.group(1).contains("大概") || matcher.group(1).contains("大约") || matcher.group(1).contains("左右"));

                if (matcher.groupCount() >= 3 && matcher.group(2) != null && matcher.group(3) != null) {
                    try {
                        double value = Double.parseDouble(matcher.group(2));
                        String unit = matcher.group(3);
                        int unitMinutes = TIME_UNIT_TO_MINUTES.getOrDefault(unit.toLowerCase(), 1);
                        int durationMinutes = (int) Math.ceil(value * unitMinutes / (unit.equals("秒") || unit.equals("秒钟") ? 60.0 : 1.0));

                        if (unit.equals("秒") || unit.equals("秒钟")) {
                            durationMinutes = (int) Math.ceil(value / 60.0);
                            stage.setTimeUnit("seconds");
                        } else if (unit.equals("分") || unit.equals("分钟") || unit.equalsIgnoreCase("min") || unit.equalsIgnoreCase("minute") || unit.equalsIgnoreCase("minutes")) {
                            stage.setTimeUnit("minutes");
                        } else if (unit.equals("时") || unit.equals("小时") || unit.equalsIgnoreCase("h") || unit.equalsIgnoreCase("hour") || unit.equalsIgnoreCase("hours")) {
                            stage.setTimeUnit("hours");
                        } else if (unit.equals("天") || unit.equals("日") || unit.equalsIgnoreCase("day") || unit.equalsIgnoreCase("days")) {
                            stage.setTimeUnit("days");
                        } else {
                            stage.setTimeUnit(unit);
                        }

                        stage.setTimeValue((int) Math.ceil(value));
                        stage.setDurationMinutes(durationMinutes);
                        stage.setApproximate(isApproximate);

                        String stageName = extractStageName(normalizedText, unit);
                        stage.setStageName(stageName);
                        stage.setStageType(determineStageType(normalizedText, stageName));

                        return stage;
                    } catch (NumberFormatException e) {
                        continue;
                    }
                } else if (matcher.group(1) != null && matcher.group(2) != null &&
                        (matcher.group(2).contains("一夜") || matcher.group(2).contains("一晚") ||
                                matcher.group(2).contains("一晚上") || matcher.group(2).contains("过夜") ||
                                matcher.group(2).contains("一整天") || matcher.group(2).contains("一天"))) {
                    String durationKeyword = matcher.group(2);
                    stage.setTimeValue(durationKeyword.contains("天") ? 1 : 8);
                    stage.setTimeUnit(durationKeyword.contains("天") ? "days" : "hours");
                    stage.setDurationMinutes(durationKeyword.contains("天") ? 1440 : 480);
                    stage.setApproximate(true);

                    String action = matcher.group(1);
                    String stageName = action + durationKeyword;
                    stage.setStageName(stageName);
                    stage.setStageType(determineStageType(normalizedText, stageName));

                    return stage;
                } else if (matcher.groupCount() >= 3 && matcher.group(1) != null && matcher.group(3) != null) {
                    try {
                        int value = Integer.parseInt(matcher.group(1));
                        boolean isHalf = matcher.group(2) != null;
                        String unit = matcher.group(3);
                        int unitMinutes = TIME_UNIT_TO_MINUTES.getOrDefault(unit, 1);
                        double totalValue = value + (isHalf ? 0.5 : 0);
                        int durationMinutes = (int) Math.ceil(totalValue * unitMinutes);

                        stage.setTimeValue((int) Math.ceil(totalValue));
                        stage.setTimeUnit(unit.equals("小时") ? "hours" : "minutes");
                        stage.setDurationMinutes(durationMinutes);
                        stage.setApproximate(false);

                        String stageName = extractStageName(normalizedText, unit);
                        stage.setStageName(stageName);
                        stage.setStageType(determineStageType(normalizedText, stageName));

                        return stage;
                    } catch (NumberFormatException e) {
                        continue;
                    }
                } else if (matcher.groupCount() >= 2 && matcher.group(1) != null && matcher.group(2) != null) {
                    try {
                        int hours = Integer.parseInt(matcher.group(1));
                        int minutes = Integer.parseInt(matcher.group(2));
                        int durationMinutes = hours * 60 + minutes;

                        stage.setTimeValue(hours * 60 + minutes);
                        stage.setTimeUnit("minutes");
                        stage.setDurationMinutes(durationMinutes);
                        stage.setApproximate(false);

                        String stageName = extractStageName(normalizedText, "分钟");
                        stage.setStageName(stageName);
                        stage.setStageType(determineStageType(normalizedText, stageName));

                        return stage;
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
        }

        return null;
    }

    private static String extractStageName(String text, String unit) {
        for (String keyword : STAGE_TYPE_MAP.keySet()) {
            if (text.contains(keyword)) {
                if (keyword.length() >= 2) {
                    return keyword;
                }
            }
        }

        if (text.contains("发酵") || text.contains("醒发")) {
            if (text.contains("二") || text.contains("第二次") || text.contains("最终")) {
                return "第二次发酵";
            } else if (text.contains("一") || text.contains("第一次") || text.contains("基础")) {
                return "第一次发酵";
            }
            return "发酵";
        }

        if (text.contains("烘烤") || text.contains("烘焙") || text.contains("烤制")) {
            return "烘烤";
        }
        if (text.contains("烤") && (text.contains("箱") || text.contains("度") || text.contains("℃"))) {
            return "烘烤";
        }

        if (text.contains("冷藏") || text.contains("冰箱")) {
            if (text.contains("一夜") || text.contains("过夜") || text.contains("晚上")) {
                return "冷藏过夜";
            }
            return "冷藏";
        }

        if (text.contains("松弛") || text.contains("静置") || text.contains("饧") || text.contains("醒")) {
            return "松弛";
        }

        if (text.contains("冷却") || text.contains("晾凉") || text.contains("放凉")) {
            return "冷却";
        }

        if (text.contains("冷冻")) {
            return "冷冻";
        }

        if (text.contains("蒸")) {
            return "蒸制";
        }

        int unitIndex = text.indexOf(unit);
        if (unitIndex > 0) {
            int start = Math.max(0, unitIndex - 10);
            String prefix = text.substring(start, unitIndex).trim();
            if (!prefix.isEmpty()) {
                String[] parts = prefix.split("[，。！？、；：,.!?;:]");
                if (parts.length > 0) {
                    String lastPart = parts[parts.length - 1].trim();
                    if (lastPart.length() > 0 && lastPart.length() <= 15) {
                        return lastPart + unit;
                    }
                }
            }
        }

        return "阶段";
    }

    private static String determineStageType(String text, String stageName) {
        for (Map.Entry<String, String> entry : STAGE_TYPE_MAP.entrySet()) {
            if (stageName.contains(entry.getKey()) || text.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        if (stageName.contains("发酵") || text.contains("发酵") || text.contains("醒发")) {
            if (stageName.contains("二") || text.contains("二发") || text.contains("第二次") || text.contains("最终")) {
                return "second_fermentation";
            }
            return "first_fermentation";
        }

        if (stageName.contains("烘烤") || stageName.contains("烤") || text.contains("烘烤") || text.contains("烘焙")) {
            return "baking";
        }

        if (stageName.contains("冷藏") || text.contains("冷藏") || text.contains("冰箱")) {
            return "refrigeration";
        }

        if (stageName.contains("松弛") || stageName.contains("静置") || text.contains("松弛") || text.contains("静置") || text.contains("饧")) {
            return "relaxation";
        }

        if (stageName.contains("冷却") || text.contains("冷却") || text.contains("晾凉")) {
            return "cooling";
        }

        if (stageName.contains("冷冻") || text.contains("冷冻")) {
            return "freezing";
        }

        return "other";
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, String>> parseStepsJson(String stepsJson) {
        List<Map<String, String>> steps = new ArrayList<>();
        try {
            if (stepsJson.startsWith("[") && stepsJson.endsWith("]")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<Object> rawList = mapper.readValue(stepsJson, List.class);
                for (Object item : rawList) {
                    if (item instanceof Map) {
                        Map<String, String> stepMap = new HashMap<>();
                        Map<String, Object> rawMap = (Map<String, Object>) item;
                        Object desc = rawMap.get("description");
                        if (desc != null) {
                            stepMap.put("description", String.valueOf(desc));
                        }
                        Object image = rawMap.get("image");
                        if (image != null) {
                            stepMap.put("image", String.valueOf(image));
                        }
                        if (!stepMap.isEmpty()) {
                            steps.add(stepMap);
                        }
                    } else if (item instanceof String) {
                        Map<String, String> stepMap = new HashMap<>();
                        stepMap.put("description", (String) item);
                        steps.add(stepMap);
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

    public static List<StepDurationParseResult.StageInfo> parseStepList(List<String> stepTexts) {
        List<StepDurationParseResult.StageInfo> stages = new ArrayList<>();
        if (stepTexts == null || stepTexts.isEmpty()) {
            return stages;
        }
        for (int i = 0; i < stepTexts.size(); i++) {
            StepDurationParseResult.StageInfo stage = parseStep(stepTexts.get(i), i);
            if (stage != null) {
                stages.add(stage);
            }
        }
        return stages;
    }
}
