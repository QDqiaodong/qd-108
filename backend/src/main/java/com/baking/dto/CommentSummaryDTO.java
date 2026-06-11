package com.baking.dto;

import lombok.Data;

import java.util.List;

@Data
public class CommentSummaryDTO {

    private List<KeywordItem> riskKeywords;

    private List<KeywordItem> remedyKeywords;

    private int totalComments;

    @Data
    public static class KeywordItem {
        private String keyword;
        private int count;

        public KeywordItem(String keyword, int count) {
            this.keyword = keyword;
            this.count = count;
        }
    }
}
