<template>
  <div class="badge-wall">
    <div class="wall-header">
      <h3 class="wall-title">徽章墙</h3>
      <span class="wall-stats">
        已获得 {{ unlockedCount }} / {{ achievements.length }}
      </span>
    </div>

    <div class="badge-grid">
      <el-tooltip
        v-for="item in achievements"
        :key="item.achievement?.id || item.id"
        placement="top"
        :show-after="200"
      >
        <template #content>
          <div class="tooltip-content">
            <div class="tooltip-title">{{ item.achievement?.name }}</div>
            <div class="tooltip-desc">{{ item.achievement?.description }}</div>
            <div class="tooltip-progress">
              进度: {{ item.progress || 0 }} / {{ item.achievement?.target }}
            </div>
            <div v-if="item.unlocked === 1 && item.unlockedAt" class="tooltip-time">
              解锁时间: {{ formatDate(item.unlockedAt) }}
            </div>
          </div>
        </template>
        <div
          class="badge-item"
          :class="{ unlocked: item.unlocked === 1 }"
        >
          <div class="badge-icon">
            {{ item.achievement?.icon || '🏅' }}
          </div>
          <div class="badge-name">{{ item.achievement?.name }}</div>
          <div class="badge-progress" v-if="item.unlocked !== 1">
            <el-progress
              :percentage="getProgressPercent(item)"
              :show-text="false"
              :stroke-width="4"
            />
          </div>
          <div class="badge-check" v-else>
            <el-icon><Check /></el-icon>
          </div>
        </div>
      </el-tooltip>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Check } from '@element-plus/icons-vue'

const props = defineProps({
  achievements: {
    type: Array,
    default: () => []
  }
})

const unlockedCount = computed(() => {
  return props.achievements.filter(a => a.unlocked === 1).length
})

const getProgressPercent = (item) => {
  const progress = item.progress || 0
  const target = item.achievement?.target || 1
  return Math.min(100, Math.floor((progress / target) * 100))
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.split(' ')[0]
}
</script>

<style scoped>
.badge-wall {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}

.wall-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.wall-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.wall-stats {
  font-size: 14px;
  color: #999;
}

.badge-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.badge-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 12px;
  border-radius: 10px;
  background: #f8f9fa;
  cursor: pointer;
  transition: all 0.3s;
  position: relative;
  opacity: 0.6;
  filter: grayscale(0.5);
}

.badge-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.badge-item.unlocked {
  opacity: 1;
  filter: none;
  background: linear-gradient(135deg, #fff5f5 0%, #ffe8e8 100%);
}

.badge-icon {
  font-size: 40px;
  margin-bottom: 8px;
}

.badge-name {
  font-size: 13px;
  color: #333;
  text-align: center;
  margin-bottom: 8px;
  font-weight: 500;
}

.badge-progress {
  width: 100%;
  padding: 0 8px;
}

.badge-check {
  position: absolute;
  top: 8px;
  right: 8px;
  color: #ff6b6b;
  font-size: 16px;
}

.tooltip-content {
  max-width: 200px;
}

.tooltip-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.tooltip-desc {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
  line-height: 1.5;
}

.tooltip-progress {
  font-size: 12px;
  color: #ff6b6b;
  font-weight: 500;
}

.tooltip-time {
  font-size: 11px;
  color: #999;
  margin-top: 6px;
}
</style>
