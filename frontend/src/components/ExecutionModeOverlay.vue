<template>
  <Teleport to="body">
    <Transition name="exec-overlay">
      <div class="execution-overlay" v-if="active">
        <div class="exec-header">
          <div class="exec-header-left">
            <span class="exec-recipe-title">{{ recipeTitle }}</span>
          </div>
          <div class="exec-header-right">
            <el-button text class="exec-close-btn" @click="$emit('exit')">
              <el-icon :size="20"><Close /></el-icon>
              <span>退出执行模式</span>
            </el-button>
          </div>
        </div>

        <div class="exec-progress-bar">
          <div class="exec-progress-track">
            <div class="exec-progress-fill" :style="{ width: progressPercent + '%' }"></div>
          </div>
          <span class="exec-progress-text">{{ stepIndex + 1 }} / {{ totalSteps }}</span>
        </div>

        <div class="exec-card-wrapper" v-if="!isAllDone">
          <div class="exec-card">
            <div class="exec-step-badge">
              <span class="exec-step-number">{{ stepIndex + 1 }}</span>
            </div>

            <div class="exec-step-body">
              <p class="exec-step-desc">{{ currentStep?.description }}</p>
              <div class="exec-step-image" v-if="currentStep?.image">
                <img :src="currentStep.image" alt="步骤图片" />
              </div>
            </div>

            <div class="exec-key-info" v-if="bakeTemp || bakeTime">
              <div class="exec-info-chip" v-if="bakeTemp">
                <el-icon><Sunrise /></el-icon>
                <span>{{ bakeTemp }}℃</span>
              </div>
              <div class="exec-info-chip" v-if="bakeTime">
                <el-icon><Timer /></el-icon>
                <span>{{ bakeTime }}分钟</span>
              </div>
            </div>

            <div class="exec-remaining-hint" v-if="remainingSteps > 0">
              还剩 {{ remainingSteps }} 步
            </div>
            <div class="exec-remaining-hint exec-last-step" v-else>
              这是最后一步！
            </div>
          </div>
        </div>

        <div class="exec-card-wrapper" v-else>
          <div class="exec-card exec-card-done">
            <div class="exec-done-icon">
              <el-icon :size="64" color="#67c23a"><CircleCheckFilled /></el-icon>
            </div>
            <h2 class="exec-done-title">全部完成！</h2>
            <p class="exec-done-desc">所有步骤已执行完毕，辛苦了！</p>
          </div>
        </div>

        <div class="exec-nav">
          <el-button
            size="large"
            :disabled="stepIndex <= 0"
            @click="$emit('prev')"
            class="exec-nav-btn"
          >
            <el-icon><ArrowLeft /></el-icon>
            上一步
          </el-button>

          <div class="exec-step-dots" v-if="totalSteps <= 10">
            <span
              v-for="i in totalSteps"
              :key="i"
              class="exec-dot"
              :class="{
                'exec-dot-active': i - 1 === stepIndex,
                'exec-dot-done': i - 1 < stepIndex
              }"
              @click="$emit('go-to', i - 1)"
            ></span>
          </div>

          <el-button
            size="large"
            type="primary"
            @click="$emit('next')"
            class="exec-nav-btn"
            v-if="!isAllDone"
          >
            {{ isLastStep ? '完成' : '下一步' }}
            <el-icon><ArrowRight /></el-icon>
          </el-button>
          <el-button
            size="large"
            type="success"
            @click="$emit('exit')"
            class="exec-nav-btn"
            v-else
          >
            返回详情
          </el-button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import {
  Close,
  ArrowLeft,
  ArrowRight,
  Sunrise,
  Timer,
  CircleCheckFilled
} from '@element-plus/icons-vue'

defineProps({
  active: Boolean,
  recipeTitle: { type: String, default: '' },
  stepIndex: { type: Number, default: 0 },
  totalSteps: { type: Number, default: 0 },
  currentStep: { type: Object, default: null },
  remainingSteps: { type: Number, default: 0 },
  progressPercent: { type: Number, default: 0 },
  isLastStep: Boolean,
  isAllDone: Boolean,
  bakeTemp: { type: [Number, String], default: null },
  bakeTime: { type: [Number, String], default: null }
})

defineEmits(['next', 'prev', 'exit', 'go-to'])
</script>

<style scoped>
.execution-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.exec-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.exec-recipe-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  max-width: 400px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.exec-close-btn {
  color: #666;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.exec-close-btn:hover {
  color: #ff6b6b;
}

.exec-progress-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  background: #fff;
  flex-shrink: 0;
}

.exec-progress-track {
  flex: 1;
  height: 6px;
  background: #e4e7ed;
  border-radius: 3px;
  overflow: hidden;
}

.exec-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #ff9a56, #ff6b6b);
  border-radius: 3px;
  transition: width 0.4s ease;
}

.exec-progress-text {
  font-size: 14px;
  color: #999;
  font-weight: 500;
  white-space: nowrap;
}

.exec-card-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow-y: auto;
}

.exec-card {
  width: 100%;
  max-width: 680px;
  background: #fff;
  border-radius: 20px;
  padding: 40px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.exec-card-done {
  padding: 60px 40px;
}

.exec-done-icon {
  margin-bottom: 24px;
}

.exec-done-title {
  font-size: 28px;
  color: #333;
  margin-bottom: 12px;
  font-weight: 700;
}

.exec-done-desc {
  font-size: 16px;
  color: #999;
}

.exec-step-badge {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff9a56 0%, #ff6b6b 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 28px;
  box-shadow: 0 4px 16px rgba(255, 107, 107, 0.35);
}

.exec-step-number {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
}

.exec-step-body {
  width: 100%;
  margin-bottom: 24px;
}

.exec-step-desc {
  font-size: 20px;
  line-height: 1.8;
  color: #333;
  text-align: left;
  margin-bottom: 16px;
}

.exec-step-image {
  border-radius: 12px;
  overflow: hidden;
  max-width: 100%;
}

.exec-step-image img {
  width: 100%;
  max-height: 320px;
  object-fit: cover;
  border-radius: 12px;
}

.exec-key-info {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.exec-info-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 16px;
  font-weight: 600;
}

.exec-info-chip:first-child {
  background: #fef0f0;
  color: #f56c6c;
}

.exec-info-chip:last-child,
.exec-info-chip:only-child {
  background: #f0f9eb;
  color: #67c23a;
}

.exec-remaining-hint {
  font-size: 14px;
  color: #999;
  padding: 6px 16px;
  background: #f5f7fa;
  border-radius: 12px;
}

.exec-last-step {
  background: linear-gradient(135deg, #fff8e6, #fff3d6);
  color: #e6a23c;
  font-weight: 500;
}

.exec-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  padding: 20px 24px;
  background: #fff;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}

.exec-nav-btn {
  min-width: 120px;
}

.exec-step-dots {
  display: flex;
  gap: 8px;
  align-items: center;
}

.exec-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #dcdfe6;
  cursor: pointer;
  transition: all 0.25s ease;
}

.exec-dot:hover {
  background: #c0c4cc;
  transform: scale(1.3);
}

.exec-dot-active {
  background: #ff6b6b;
  transform: scale(1.4);
  box-shadow: 0 0 8px rgba(255, 107, 107, 0.5);
}

.exec-dot-done {
  background: #67c23a;
}

.exec-overlay-enter-active,
.exec-overlay-leave-active {
  transition: opacity 0.35s ease, transform 0.35s ease;
}

.exec-overlay-enter-from,
.exec-overlay-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

@media (max-width: 768px) {
  .exec-card {
    padding: 28px 20px;
    border-radius: 16px;
  }

  .exec-step-desc {
    font-size: 17px;
  }

  .exec-step-badge {
    width: 56px;
    height: 56px;
  }

  .exec-step-number {
    font-size: 24px;
  }

  .exec-nav {
    gap: 12px;
    padding: 16px;
  }

  .exec-nav-btn {
    min-width: 90px;
  }

  .exec-step-dots {
    gap: 6px;
  }

  .exec-dot {
    width: 8px;
    height: 8px;
  }
}
</style>
