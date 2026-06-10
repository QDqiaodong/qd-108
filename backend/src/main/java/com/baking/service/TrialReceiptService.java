package com.baking.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baking.entity.TrialReceipt;
import com.baking.mapper.TrialReceiptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrialReceiptService {

    private final TrialReceiptMapper trialReceiptMapper;
    private final RecipeService recipeService;

    public IPage<TrialReceipt> getTrialReceipts(Long recipeId, int pageNum, int pageSize) {
        Page<TrialReceipt> page = new Page<>(pageNum, pageSize);
        return trialReceiptMapper.selectByRecipeId(page, recipeId);
    }

    @Transactional
    public TrialReceipt addTrialReceipt(TrialReceipt trialReceipt) {
        trialReceiptMapper.insert(trialReceipt);
        recipeService.incrementTrialReceiptCount(trialReceipt.getRecipeId(), 1);
        return trialReceipt;
    }
}
