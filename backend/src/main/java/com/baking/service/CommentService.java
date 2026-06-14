package com.baking.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baking.config.CommentKeywordConfig;
import com.baking.config.FailureThemeCluster;
import com.baking.dto.CommentSummaryDTO;
import com.baking.dto.FailurePitfallDTO;
import com.baking.entity.Comment;
import com.baking.mapper.CommentMapper;
import com.baking.mapper.TrialReceiptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final TrialReceiptMapper trialReceiptMapper;
    private final RecipeService recipeService;

    private static final int MAX_KEYWORDS = 5;
    private static final int MIN_COUNT = 1;

    public IPage<Comment> getComments(Long recipeId, int pageNum, int pageSize) {
        Page<Comment> page = new Page<>(pageNum, pageSize);
        IPage<Comment> parentPage = commentMapper.selectParentComments(page, recipeId);

        for (Comment parent : parentPage.getRecords()) {
            List<Comment> replies = commentMapper.selectReplies(parent.getId());
            parent.setReplies(replies);
        }

        return parentPage;
    }

    @Transactional
    public Comment addComment(Comment comment) {
        comment.setLikeCount(0);
        commentMapper.insert(comment);
        recipeService.incrementCommentCount(comment.getRecipeId(), 1);
        return comment;
    }

    public void likeComment(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment != null) {
            comment.setLikeCount(comment.getLikeCount() + 1);
            commentMapper.updateById(comment);
        }
    }

    public CommentSummaryDTO getCommentSummary(Long recipeId) {
        List<String> contents = commentMapper.selectAllCommentContents(recipeId);

        CommentSummaryDTO summary = new CommentSummaryDTO();
        summary.setTotalComments(contents.size());

        if (contents.isEmpty()) {
            summary.setRiskKeywords(new ArrayList<>());
            summary.setRemedyKeywords(new ArrayList<>());
            return summary;
        }

        Map<String, Integer> riskCountMap = countKeywords(contents, CommentKeywordConfig.RISK_KEYWORDS);
        Map<String, Integer> remedyCountMap = countKeywords(contents, CommentKeywordConfig.REMEDY_KEYWORDS);

        List<CommentSummaryDTO.KeywordItem> riskKeywords = buildKeywordItems(riskCountMap);
        List<CommentSummaryDTO.KeywordItem> remedyKeywords = buildKeywordItems(remedyCountMap);

        summary.setRiskKeywords(riskKeywords);
        summary.setRemedyKeywords(remedyKeywords);

        return summary;
    }

    private Map<String, Integer> countKeywords(List<String> contents, List<String> keywords) {
        Map<String, Integer> countMap = new HashMap<>();
        for (String content : contents) {
            if (content == null || content.isEmpty()) {
                continue;
            }
            for (String keyword : keywords) {
                if (content.contains(keyword)) {
                    countMap.put(keyword, countMap.getOrDefault(keyword, 0) + 1);
                }
            }
        }
        return countMap;
    }

    private List<CommentSummaryDTO.KeywordItem> buildKeywordItems(Map<String, Integer> countMap) {
        return countMap.entrySet().stream()
                .filter(entry -> entry.getValue() >= MIN_COUNT)
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(MAX_KEYWORDS)
                .map(entry -> new CommentSummaryDTO.KeywordItem(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    public FailurePitfallDTO getFailurePitfalls(Long recipeId) {
        List<String> commentContents = commentMapper.selectAllCommentContents(recipeId);
        List<String> trialContents = trialReceiptMapper.selectAllTextContents(recipeId);

        List<String> allContents = new ArrayList<>(commentContents);
        allContents.addAll(trialContents);

        FailurePitfallDTO dto = new FailurePitfallDTO();
        dto.setTotalFeedbackCount(allContents.size());

        if (allContents.isEmpty()) {
            dto.setThemeClusters(new ArrayList<>());
            dto.setTotalFailureCount(0);
            return dto;
        }

        List<FailurePitfallDTO.ThemeCluster> clusters = new ArrayList<>();
        int totalFailureCount = 0;

        for (FailureThemeCluster.FailureTheme theme : FailureThemeCluster.FailureTheme.values()) {
            Map<String, Integer> matchedKeywords = new HashMap<>();
            int themeCount = 0;

            for (String content : allContents) {
                if (content == null || content.isEmpty()) {
                    continue;
                }
                boolean contentMatchedTheme = false;
                for (String keyword : theme.getKeywords()) {
                    if (content.contains(keyword)) {
                        matchedKeywords.merge(keyword, 1, Integer::sum);
                        if (!contentMatchedTheme) {
                            themeCount++;
                            contentMatchedTheme = true;
                        }
                    }
                }
            }

            if (themeCount > 0) {
                List<String> topKeywords = matchedKeywords.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                                .thenComparing(Map.Entry.comparingByKey()))
                        .limit(5)
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());

                totalFailureCount += themeCount;
                clusters.add(new FailurePitfallDTO.ThemeCluster(
                        theme.name(),
                        theme.getThemeName(),
                        theme.getDescription(),
                        themeCount,
                        topKeywords
                ));
            }
        }

        clusters.sort(Comparator.comparingInt(FailurePitfallDTO.ThemeCluster::getCount).reversed());

        dto.setThemeClusters(clusters);
        dto.setTotalFailureCount(totalFailureCount);
        return dto;
    }
}
