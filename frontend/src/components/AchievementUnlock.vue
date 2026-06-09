<template>
  <teleport to="body">
    <transition name="fade">
      <div v-if="visible" class="achievement-overlay" @click="handleClose">
        <div class="confetti-container">
          <div
            v-for="i in 50"
            :key="i"
            class="confetti"
            :style="getConfettiStyle(i)"
          ></div>
        </div>

        <transition name="bounce">
          <div class="achievement-modal" v-if="showBadge" @click.stop>
            <div class="badge-glow"></div>
            <div class="badge-large">
              {{ currentAchievement?.achievement?.icon || '🏆' }}
            </div>
            <div class="achievement-title">
              恭喜解锁新徽章！
            </div>
            <div class="achievement-name">
              {{ currentAchievement?.achievement?.name }}
            </div>
            <div class="achievement-desc">
              {{ currentAchievement?.achievement?.description }}
            </div>
            <el-button type="primary" size="large" class="confirm-btn" @click="handleNext">
              {{ hasNext ? '下一个' : '太棒了' }}
            </el-button>
          </div>
        </transition>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { ref, watch, computed } from 'vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  achievements: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:visible', 'close'])

const currentIndex = ref(0)
const showBadge = ref(false)

const currentAchievement = computed(() => {
  return props.achievements[currentIndex.value] || null
})

const hasNext = computed(() => {
  return currentIndex.value < props.achievements.length - 1
})

watch(() => props.visible, (val) => {
  if (val) {
    currentIndex.value = 0
    setTimeout(() => {
      showBadge.value = true
    }, 100)
  } else {
    showBadge.value = false
  }
})

watch(() => props.achievements, (val) => {
  if (val && val.length > 0) {
    currentIndex.value = 0
  }
})

const handleNext = () => {
  if (hasNext.value) {
    showBadge.value = false
    setTimeout(() => {
      currentIndex.value++
      showBadge.value = true
    }, 200)
  } else {
    handleClose()
  }
}

const handleClose = () => {
  showBadge.value = false
  setTimeout(() => {
    emit('update:visible', false)
    emit('close')
  }, 200)
}

const getConfettiStyle = (index) => {
  const colors = ['#ff6b6b', '#ffd93d', '#6bcb77', '#4d96ff', '#ff9a56', '#c56cf0']
  const left = Math.random() * 100
  const delay = Math.random() * 3
  const duration = 3 + Math.random() * 2
  const color = colors[index % colors.length]
  const size = 6 + Math.random() * 8

  return {
    left: `${left}%`,
    backgroundColor: color,
    width: `${size}px`,
    height: `${size}px`,
    animationDelay: `${delay}s`,
    animationDuration: `${duration}s`,
    borderRadius: Math.random() > 0.5 ? '50%' : '2px'
  }
}
</script>

<style scoped>
.achievement-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  overflow: hidden;
}

.confetti-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  overflow: hidden;
}

.confetti {
  position: absolute;
  top: -10px;
  animation: confetti-fall linear infinite;
}

@keyframes confetti-fall {
  0% {
    transform: translateY(0) rotate(0deg);
    opacity: 1;
  }
  100% {
    transform: translateY(100vh) rotate(720deg);
    opacity: 0;
  }
}

.achievement-modal {
  background: #fff;
  border-radius: 20px;
  padding: 40px 50px;
  text-align: center;
  position: relative;
  min-width: 320px;
}

.badge-glow {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -60%);
  width: 160px;
  height: 160px;
  background: radial-gradient(circle, rgba(255, 217, 61, 0.4) 0%, transparent 70%);
  border-radius: 50%;
  animation: glow-pulse 2s ease-in-out infinite;
}

@keyframes glow-pulse {
  0%, 100% {
    transform: translate(-50%, -60%) scale(1);
    opacity: 0.5;
  }
  50% {
    transform: translate(-50%, -60%) scale(1.2);
    opacity: 0.8;
  }
}

.badge-large {
  font-size: 80px;
  margin-bottom: 20px;
  position: relative;
  z-index: 1;
  animation: badge-bounce 0.6s ease-out;
}

@keyframes badge-bounce {
  0% {
    transform: scale(0);
  }
  50% {
    transform: scale(1.2);
  }
  100% {
    transform: scale(1);
  }
}

.achievement-title {
  font-size: 20px;
  font-weight: 600;
  color: #ff6b6b;
  margin-bottom: 12px;
}

.achievement-name {
  font-size: 24px;
  font-weight: 700;
  color: #333;
  margin-bottom: 8px;
}

.achievement-desc {
  font-size: 14px;
  color: #999;
  margin-bottom: 30px;
}

.confirm-btn {
  width: 100%;
  background: linear-gradient(135deg, #ff9a56 0%, #ff6b6b 100%);
  border: none;
  border-radius: 25px;
  font-size: 16px;
  font-weight: 500;
}

.confirm-btn:hover {
  background: linear-gradient(135deg, #ff8a3d 0%, #ff5252 100%);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.bounce-enter-active {
  animation: bounce-in 0.5s;
}

.bounce-leave-active {
  animation: bounce-out 0.3s;
}

@keyframes bounce-in {
  0% {
    transform: scale(0.3);
    opacity: 0;
  }
  50% {
    transform: scale(1.05);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes bounce-out {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  100% {
    transform: scale(0.8);
    opacity: 0;
  }
}
</style>
