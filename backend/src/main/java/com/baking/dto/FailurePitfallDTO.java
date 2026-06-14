package com.baking.dto;

import lombok.Data;

import java.util.List;

@Data
public class FailurePitfallDTO {

    private List<ThemeCluster> themeClusters;

    private int totalFeedbackCount;

    private int totalFailureCount;

    @Data
    public static class ThemeCluster {
        private String themeCode;
        private String themeName;
        private String description;
        private int count;
        private List<String> matchedKeywords;

        public ThemeCluster(String themeCode, String themeName, String description, int count, List<String> matchedKeywords) {
            this.themeCode = themeCode;
            this.themeName = themeName;
            this.description = description;
            this.count = count;
            this.matchedKeywords = matchedKeywords;
        }
    }
}
