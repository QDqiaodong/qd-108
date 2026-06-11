package com.baking.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baking.entity.TrialReceipt;
import com.baking.mapper.TrialReceiptMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrialReceiptService {

    private final TrialReceiptMapper trialReceiptMapper;
    private final RecipeService recipeService;
    private final ObjectMapper objectMapper;

    public IPage<TrialReceipt> getTrialReceipts(Long recipeId, int pageNum, int pageSize) {
        Page<TrialReceipt> page = new Page<>(pageNum, pageSize);
        IPage<TrialReceipt> result = trialReceiptMapper.selectByRecipeId(page, recipeId);
        for (TrialReceipt receipt : result.getRecords()) {
            parseResultImages(receipt);
        }
        return result;
    }

    @Transactional
    public TrialReceipt addTrialReceipt(TrialReceipt trialReceipt) {
        if (trialReceipt.getResultImageList() != null && !trialReceipt.getResultImageList().isEmpty()) {
            try {
                trialReceipt.setResultImages(objectMapper.writeValueAsString(trialReceipt.getResultImageList()));
            } catch (JsonProcessingException e) {
                log.error("序列化成品图片失败", e);
            }
        }
        trialReceiptMapper.insert(trialReceipt);
        parseResultImages(trialReceipt);
        recipeService.incrementTrialReceiptCount(trialReceipt.getRecipeId(), 1);
        return trialReceipt;
    }

    private void parseResultImages(TrialReceipt receipt) {
        if (receipt.getResultImages() != null && !receipt.getResultImages().isEmpty()) {
            try {
                List<String> images = objectMapper.readValue(
                        receipt.getResultImages(),
                        new TypeReference<List<String>>() {}
                );
                receipt.setResultImageList(images);
            } catch (JsonProcessingException e) {
                log.error("解析成品图片失败", e);
                receipt.setResultImageList(new ArrayList<>());
            }
        } else {
            receipt.setResultImageList(new ArrayList<>());
        }
    }
}
