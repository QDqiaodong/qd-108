<template>
  <div class="ingredient-scaler">
    <div class="scaler-label">
      <el-icon><ScaleToOriginal /></el-icon>
      <span>份量缩放</span>
    </div>
    <div class="scaler-controls">
      <el-button
        size="small"
        circle
        :disabled="scaleFactor <= 0.5"
        @click="decreaseScale(0.5)"
      >
        <el-icon><Minus /></el-icon>
      </el-button>
      <div class="scaler-display" @click="resetScale">
        <span class="scaler-value">{{ formatScale(scaleFactor) }}</span>
        <span class="scaler-unit">倍</span>
      </div>
      <el-button
        size="small"
        circle
        :disabled="scaleFactor >= 10"
        @click="increaseScale(0.5)"
      >
        <el-icon><Plus /></el-icon>
      </el-button>
    </div>
    <div class="scaler-hint" v-if="scaleFactor !== 1">
      点击倍数重置为 1 倍 · 缩放仅影响当前视图
    </div>
  </div>
</template>

<script setup>
import { Minus, Plus, ScaleToOriginal } from '@element-plus/icons-vue'

defineProps({
  scaleFactor: {
    type: Number,
    required: true
  }
})

const emit = defineEmits(['increase', 'decrease', 'reset'])

const increaseScale = (step) => emit('increase', step)
const decreaseScale = (step) => emit('decrease', step)
const resetScale = () => emit('reset')

const formatScale = (val) => {
  if (val === Math.floor(val)) return String(val)
  return val.toFixed(1)
}
</script>

<style scoped>
.ingredient-scaler {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #fdf6ec 0%, #fef9f0 100%);
  border-radius: 8px;
  border: 1px solid #faecd8;
}

.scaler-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #e6a23c;
  font-weight: 500;
  white-space: nowrap;
}

.scaler-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.scaler-display {
  display: flex;
  align-items: baseline;
  gap: 2px;
  cursor: pointer;
  padding: 4px 12px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
  transition: all 0.2s ease;
  user-select: none;
}

.scaler-display:hover {
  border-color: #e6a23c;
  box-shadow: 0 2px 8px rgba(230, 162, 60, 0.2);
}

.scaler-value {
  font-size: 20px;
  font-weight: 700;
  color: #e6a23c;
  font-family: 'Courier New', monospace;
}

.scaler-unit {
  font-size: 13px;
  color: #999;
}

.scaler-hint {
  font-size: 12px;
  color: #c0c4cc;
  white-space: nowrap;
}
</style>
