package com.baking.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baking.common.Result;
import com.baking.entity.TrialReceipt;
import com.baking.service.TrialReceiptService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/trial-receipts")
@RequiredArgsConstructor
public class TrialReceiptController {

    private final TrialReceiptService trialReceiptService;

    @GetMapping
    public Result<IPage<TrialReceipt>> getTrialReceipts(
            @RequestParam Long recipeId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(trialReceiptService.getTrialReceipts(recipeId, pageNum, pageSize));
    }

    @PostMapping
    public Result<TrialReceipt> addTrialReceipt(@RequestBody TrialReceiptRequest request) {
        TrialReceipt trialReceipt = new TrialReceipt();
        trialReceipt.setRecipeId(request.getRecipeId());
        trialReceipt.setUserId(request.getUserId());
        trialReceipt.setSuccess(request.getSuccess());
        trialReceipt.setTasteRating(request.getTasteRating());
        trialReceipt.setTasteComment(request.getTasteComment());
        trialReceipt.setTempAdjustment(request.getTempAdjustment());
        trialReceipt.setMoldDifference(request.getMoldDifference());
        trialReceipt.setNotes(request.getNotes());
        trialReceipt.setResultImageList(request.getResultImages());
        return Result.success(trialReceiptService.addTrialReceipt(trialReceipt));
    }

    @Data
    public static class TrialReceiptRequest {
        private Long recipeId;
        private Long userId;
        private Integer success;
        private Integer tasteRating;
        private String tasteComment;
        private String tempAdjustment;
        private String moldDifference;
        private String notes;
        private java.util.List<String> resultImages;
    }
}
