package com.baking.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baking.entity.BakePlan;
import com.baking.mapper.BakePlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BakePlanService {

    private final BakePlanMapper bakePlanMapper;

    public List<BakePlan> getMonthPlans(Long userId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1).minusDays(1);

        LambdaQueryWrapper<BakePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BakePlan::getUserId, userId);
        wrapper.between(BakePlan::getPlanDate, startDate, endDate);
        return bakePlanMapper.selectList(wrapper);
    }

    public BakePlan getPlanByDate(Long userId, LocalDate date) {
        LambdaQueryWrapper<BakePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BakePlan::getUserId, userId);
        wrapper.eq(BakePlan::getPlanDate, date);
        return bakePlanMapper.selectOne(wrapper);
    }

    @Transactional
    public BakePlan createPlan(BakePlan plan) {
        plan.setCreatedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        bakePlanMapper.insert(plan);
        return plan;
    }

    @Transactional
    public BakePlan updatePlan(BakePlan plan) {
        plan.setUpdatedAt(LocalDateTime.now());
        bakePlanMapper.updateById(plan);
        return plan;
    }

    @Transactional
    public boolean deletePlan(Long id, Long userId) {
        LambdaQueryWrapper<BakePlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BakePlan::getId, id);
        wrapper.eq(BakePlan::getUserId, userId);
        return bakePlanMapper.delete(wrapper) > 0;
    }
}
