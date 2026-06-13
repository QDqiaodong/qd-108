package com.baking.util;

import com.baking.dto.StepDurationParseResult;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StepDurationParserTest {

    @Test
    void testParseStep_BakingMinutes() {
        String text = "放入预热好的烤箱，上下火180度，烘烤18分钟";
        StepDurationParseResult.StageInfo stage = StepDurationParser.parseStep(text, 0);

        assertNotNull(stage);
        assertEquals("烘烤", stage.getStageName());
        assertEquals("baking", stage.getStageType());
        assertEquals(18, stage.getDurationMinutes());
        assertEquals("minutes", stage.getTimeUnit());
        assertEquals(18, stage.getTimeValue());
        assertFalse(stage.isApproximate());
        assertEquals(0, stage.getStepIndex());
    }

    @Test
    void testParseStep_SecondFermentation() {
        String text = "放在温暖湿润处进行二发40分钟，至面团两倍大";
        StepDurationParseResult.StageInfo stage = StepDurationParser.parseStep(text, 1);

        assertNotNull(stage);
        assertEquals("第二次发酵", stage.getStageName());
        assertEquals("second_fermentation", stage.getStageType());
        assertEquals(40, stage.getDurationMinutes());
        assertEquals("minutes", stage.getTimeUnit());
        assertEquals(40, stage.getTimeValue());
    }

    @Test
    void testParseStep_RefrigerationOvernight() {
        String text = "揉好的面团放入冰箱冷藏一夜";
        StepDurationParseResult.StageInfo stage = StepDurationParser.parseStep(text, 2);

        assertNotNull(stage);
        assertEquals("冷藏过夜", stage.getStageName());
        assertEquals("refrigeration", stage.getStageType());
        assertEquals(480, stage.getDurationMinutes());
        assertTrue(stage.isApproximate());
    }

    @Test
    void testParseStep_ApproximateTime() {
        String text = "松弛约20分钟";
        StepDurationParseResult.StageInfo stage = StepDurationParser.parseStep(text, 3);

        assertNotNull(stage);
        assertEquals("松弛", stage.getStageName());
        assertEquals("relaxation", stage.getStageType());
        assertEquals(20, stage.getDurationMinutes());
        assertTrue(stage.isApproximate());
        assertEquals("minutes", stage.getTimeUnit());
    }

    @Test
    void testParseStep_Hours() {
        String text = "基础发酵1小时，至面团两倍大";
        StepDurationParseResult.StageInfo stage = StepDurationParser.parseStep(text, 4);

        assertNotNull(stage);
        assertEquals("第一次发酵", stage.getStageName());
        assertEquals("first_fermentation", stage.getStageType());
        assertEquals(60, stage.getDurationMinutes());
        assertEquals("hours", stage.getTimeUnit());
        assertEquals(1, stage.getTimeValue());
    }

    @Test
    void testParseStep_HalfHour() {
        String text = "中间松弛30分钟";
        StepDurationParseResult.StageInfo stage = StepDurationParser.parseStep(text, 5);

        assertNotNull(stage);
        assertEquals("松弛", stage.getStageName());
        assertEquals("relaxation", stage.getStageType());
        assertEquals(30, stage.getDurationMinutes());
        assertEquals("minutes", stage.getTimeUnit());
        assertEquals(30, stage.getTimeValue());
    }

    @Test
    void testParseStep_Cooling() {
        String text = "出炉后晾凉10分钟再脱模";
        StepDurationParseResult.StageInfo stage = StepDurationParser.parseStep(text, 6);

        assertNotNull(stage);
        assertEquals("冷却", stage.getStageName());
        assertEquals("cooling", stage.getStageType());
        assertEquals(10, stage.getDurationMinutes());
        assertEquals("minutes", stage.getTimeUnit());
    }

    @Test
    void testParseStep_TimeWithRightApproximate() {
        String text = "烘烤25分钟左右";
        StepDurationParseResult.StageInfo stage = StepDurationParser.parseStep(text, 7);

        assertNotNull(stage);
        assertEquals("烘烤", stage.getStageName());
        assertEquals(25, stage.getDurationMinutes());
        assertTrue(stage.isApproximate());
    }

    @Test
    void testParseStep_NoDuration() {
        String text = "将面粉和糖混合均匀";
        StepDurationParseResult.StageInfo stage = StepDurationParser.parseStep(text, 8);

        assertNull(stage);
    }

    @Test
    void testParseStep_EmptyText() {
        StepDurationParseResult.StageInfo stage = StepDurationParser.parseStep("", 0);
        assertNull(stage);

        stage = StepDurationParser.parseStep(null, 0);
        assertNull(stage);
    }

    @Test
    void testParse_JsonSteps() {
        String stepsJson = "[{\"description\":\"揉好的面团放入冰箱冷藏一夜\"}," +
                "{\"description\":\"取出面团，室温松弛20分钟\"}," +
                "{\"description\":\"擀开后进行二发40分钟\"}," +
                "{\"description\":\"放入烤箱，180度烘烤18分钟\"}]";

        StepDurationParseResult result = StepDurationParser.parse(stepsJson);

        assertNotNull(result);
        assertEquals(4, result.getTotalStepCount());
        assertEquals(4, result.getParsedStepCount());
        assertEquals(4, result.getStages().size());
        assertEquals(480 + 20 + 40 + 18, result.getTotalDurationMinutes());

        assertEquals("refrigeration", result.getStages().get(0).getStageType());
        assertEquals("relaxation", result.getStages().get(1).getStageType());
        assertEquals("second_fermentation", result.getStages().get(2).getStageType());
        assertEquals("baking", result.getStages().get(3).getStageType());
    }

    @Test
    void testParse_EmptyJson() {
        StepDurationParseResult result = StepDurationParser.parse("[]");
        assertNotNull(result);
        assertEquals(0, result.getTotalStepCount());
        assertEquals(0, result.getParsedStepCount());
        assertTrue(result.getStages().isEmpty());
        assertEquals(0, result.getTotalDurationMinutes());
    }

    @Test
    void testParse_NullOrEmpty() {
        StepDurationParseResult result = StepDurationParser.parse(null);
        assertNotNull(result);
        assertEquals(0, result.getTotalStepCount());

        result = StepDurationParser.parse("");
        assertNotNull(result);
        assertEquals(0, result.getTotalStepCount());
    }

    @Test
    void testParseStepList_MultipleSteps() {
        List<String> steps = Arrays.asList(
                "揉好的面团冷藏一夜",
                "取出后松弛20分钟",
                "进行一发1小时",
                "排气分割松弛15分钟",
                "整形后二发45分钟",
                "烤箱预热后烘烤35分钟",
                "出炉晾凉30分钟",
                "将材料混合均匀"
        );

        List<StepDurationParseResult.StageInfo> stages = StepDurationParser.parseStepList(steps);

        assertEquals(7, stages.size());
        assertEquals("refrigeration", stages.get(0).getStageType());
        assertEquals("relaxation", stages.get(1).getStageType());
        assertEquals("first_fermentation", stages.get(2).getStageType());
        assertEquals("relaxation", stages.get(3).getStageType());
        assertEquals("second_fermentation", stages.get(4).getStageType());
        assertEquals("baking", stages.get(5).getStageType());
        assertEquals("cooling", stages.get(6).getStageType());

        assertEquals(480 + 20 + 60 + 15 + 45 + 35 + 30,
                stages.stream().mapToInt(StepDurationParseResult.StageInfo::getDurationMinutes).sum());
    }

    @Test
    void testParseStep_OneAndHalfHours() {
        String text = "冷藏1个半小时";
        StepDurationParseResult.StageInfo stage = StepDurationParser.parseStep(text, 0);

        assertNotNull(stage);
        assertEquals("冷藏", stage.getStageName());
        assertEquals("refrigeration", stage.getStageType());
        assertEquals(90, stage.getDurationMinutes());
    }

    @Test
    void testParseStep_Seconds() {
        String text = "高速打发60秒";
        StepDurationParseResult.StageInfo stage = StepDurationParser.parseStep(text, 0);

        assertNotNull(stage);
        assertEquals("打发", stage.getStageName());
        assertEquals("whipping", stage.getStageType());
        assertEquals(1, stage.getDurationMinutes());
        assertEquals("seconds", stage.getTimeUnit());
        assertEquals(60, stage.getTimeValue());
    }

    @Test
    void testParseStep_Days() {
        String text = "浸泡3天";
        StepDurationParseResult.StageInfo stage = StepDurationParser.parseStep(text, 0);

        assertNotNull(stage);
        assertEquals("浸泡", stage.getStageName());
        assertEquals("soaking", stage.getStageType());
        assertEquals(4320, stage.getDurationMinutes());
        assertEquals("days", stage.getTimeUnit());
    }

    @Test
    void testParseStep_FirstFermentation() {
        String text = "基础发酵约60分钟";
        StepDurationParseResult.StageInfo stage = StepDurationParser.parseStep(text, 0);

        assertNotNull(stage);
        assertEquals("第一次发酵", stage.getStageName());
        assertEquals("first_fermentation", stage.getStageType());
        assertTrue(stage.isApproximate());
    }

    @Test
    void testParseStep_Marinating() {
        String text = "腌制2小时入味";
        StepDurationParseResult.StageInfo stage = StepDurationParser.parseStep(text, 0);

        assertNotNull(stage);
        assertEquals("腌制", stage.getStageName());
        assertEquals("marinating", stage.getStageType());
        assertEquals(120, stage.getDurationMinutes());
    }

    @Test
    void testParseStep_OriginalText() {
        String text = "烤箱180度烘烤约25分钟左右，至表面金黄";
        StepDurationParseResult.StageInfo stage = StepDurationParser.parseStep(text, 0);

        assertNotNull(stage);
        assertTrue(stage.getOriginalText().contains("25分钟"));
        assertEquals("烘烤", stage.getStageName());
        assertTrue(stage.isApproximate());
    }

    @Test
    void testParseStep_Freezing() {
        String text = "放入冰箱冷冻30分钟定型";
        StepDurationParseResult.StageInfo stage = StepDurationParser.parseStep(text, 0);

        assertNotNull(stage);
        assertEquals("冷冻", stage.getStageName());
        assertEquals("freezing", stage.getStageType());
        assertEquals(30, stage.getDurationMinutes());
    }

    @Test
    void testParseStep_Steaming() {
        String text = "水开后蒸15分钟";
        StepDurationParseResult.StageInfo stage = StepDurationParser.parseStep(text, 0);

        assertNotNull(stage);
        assertEquals("蒸制", stage.getStageName());
        assertEquals("steaming", stage.getStageType());
        assertEquals(15, stage.getDurationMinutes());
    }
}
