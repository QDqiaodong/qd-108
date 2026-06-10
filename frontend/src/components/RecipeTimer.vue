<template>
  <div class="recipe-timer" :class="{ 'cooking-mode-timer': cookingMode }">
    <div class="timer-header">
      <h3 class="section-title">烘焙计时器</h3>
      <el-button type="primary" size="small" @click="showAddDialog = true" :icon="Plus">
        添加阶段
      </el-button>
    </div>

    <div class="timer-display" :class="{ 'cooking-timer-display': cookingMode }" v-if="stages.length">
      <div class="current-stage" v-if="!isAllCompleted">
        <div class="stage-name">
          {{ hasStarted ? (currentStage?.name || '等待开始') : `${currentStage?.name || '第一阶段'} (待开始)` }}
        </div>
        <div class="countdown" :class="{ warning: isTimeWarning, finished: isCurrentFinished, 'cooking-countdown': cookingMode }">
          {{ formatTime(displayRemainingTime) }}
        </div>
        <div class="stage-progress">
          <el-progress
            :percentage="stageProgress"
            :stroke-width="cookingMode ? 10 : 6"
            :color="progressColor"
            show-text="false"
          />
        </div>
      </div>
      <div class="current-stage" v-else>
        <div class="stage-name">全部完成</div>
        <div class="countdown finished" :class="{ 'cooking-countdown': cookingMode }">00:00:00</div>
      </div>

      <div class="timer-controls">
        <el-button
          v-if="!isRunning"
          type="primary"
          size="large"
          @click="startTimer"
          :disabled="isAllCompleted"
          :icon="VideoPlay"
        >
          {{ hasStarted ? '继续' : '开始' }}
        </el-button>
        <el-button
          v-else
          type="warning"
          size="large"
          @click="pauseTimer"
          :icon="VideoPause"
        >
          暂停
        </el-button>
        <el-button
          size="large"
          @click="resetTimer"
          :icon="RefreshLeft"
        >
          重置
        </el-button>
        <el-button
          size="large"
          @click="skipStage"
          :disabled="isAllCompleted || !hasStarted || stages.length === 0"
          :icon="DArrowRight"
        >
          跳过
        </el-button>
      </div>
    </div>

    <div class="empty-tip" v-else>
      <el-empty description="暂无计时阶段，点击上方按钮添加" :image-size="80" />
    </div>

    <div class="stages-list" v-if="stages.length">
      <div class="stages-header">
        <span>阶段列表</span>
        <span class="total-time">总时长: {{ formatTime(totalDuration) }}</span>
      </div>
      <div class="stage-items">
        <div
          v-for="(stage, index) in stages"
          :key="stage.id"
          class="stage-item"
          :class="{
            active: index === currentStageIndex && isRunning,
            paused: index === currentStageIndex && !isRunning && hasStarted,
            pending: index === displayStageIndex && !hasStarted,
            completed: hasStarted && index < currentStageIndex,
            'current-stage-item': index === displayStageIndex,
            'cooking-active-stage': cookingMode && index === currentStageIndex && isRunning
          }"
        >
          <div class="stage-index">{{ index + 1 }}</div>
          <div class="stage-info">
            <div class="stage-title">{{ stage.name }}</div>
            <div class="stage-duration">{{ formatTime(stage.duration) }}</div>
          </div>
          <div class="stage-sound">
            <el-icon v-if="stage.soundEnabled" class="sound-icon" :size="18">
              <Bell />
            </el-icon>
            <el-icon v-else class="sound-icon muted" :size="18">
              <BellFilled />
            </el-icon>
          </div>
          <div class="stage-actions">
            <el-button text size="small" @click="editStage(stage)" :icon="Edit" />
            <el-button text size="small" type="danger" @click="deleteStage(index)" :icon="Delete" />
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="showAddDialog" :title="editingStage ? '编辑阶段' : '添加阶段'" width="400px">
      <el-form :model="stageForm" label-width="80px">
        <el-form-item label="阶段名称">
          <el-input v-model="stageForm.name" placeholder="如：第一次发酵" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="时长">
          <div class="duration-inputs">
            <el-input-number v-model="stageForm.hours" :min="0" :max="23" size="default" />
            <span class="duration-label">时</span>
            <el-input-number v-model="stageForm.minutes" :min="0" :max="59" size="default" />
            <span class="duration-label">分</span>
            <el-input-number v-model="stageForm.seconds" :min="0" :max="59" size="default" />
            <span class="duration-label">秒</span>
          </div>
        </el-form-item>
        <el-form-item label="提示音">
          <el-switch v-model="stageForm.soundEnabled" />
        </el-form-item>
        <el-form-item label="震动提醒">
          <el-switch v-model="stageForm.vibrationEnabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="saveStage">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showCompleteDialog"
      title="阶段完成"
      width="360px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      class="complete-dialog"
    >
      <div class="complete-content">
        <div class="complete-icon">
          <el-icon :size="64" color="#67c23a">
            <CircleCheck />
          </el-icon>
        </div>
        <div class="complete-stage-name">{{ completedStageName }}</div>
        <div class="complete-tip">计时已结束，请及时查看！</div>
      </div>
      <template #footer>
        <el-button type="primary" @click="handleCompleteConfirm">知道了</el-button>
      </template>
    </el-dialog>

    <div class="preset-section" v-if="stages.length === 0">
      <div class="preset-title">快速添加预设阶段</div>
      <div class="preset-buttons">
        <el-button size="small" @click="addPreset('fermentation')">第一次发酵</el-button>
        <el-button size="small" @click="addPreset('relax')">中间松弛</el-button>
        <el-button size="small" @click="addPreset('secondFermentation')">第二次发酵</el-button>
        <el-button size="small" @click="addPreset('bake')">烘烤</el-button>
        <el-button size="small" @click="addPreset('cool')">冷却</el-button>
        <el-button type="primary" size="small" @click="addAllPresets">全部添加</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import {
  Plus,
  VideoPlay,
  VideoPause,
  RefreshLeft,
  DArrowRight,
  Edit,
  Delete,
  Bell,
  BellFilled,
  CircleCheck
} from '@element-plus/icons-vue'

const props = defineProps({
  recipeId: {
    type: [String, Number],
    default: ''
  },
  cookingMode: {
    type: Boolean,
    default: false
  }
})

const stages = ref([])
const currentStageIndex = ref(-1)
const isRunning = ref(false)
const hasStarted = ref(false)
const remainingTime = ref(0)
const showAddDialog = ref(false)
const showCompleteDialog = ref(false)
const completedStageName = ref('')
const editingStage = ref(null)

const stageForm = ref({
  name: '',
  hours: 0,
  minutes: 30,
  seconds: 0,
  soundEnabled: true,
  vibrationEnabled: true
})

let timerInterval = null
let endTime = null
let audioContext = null

const storageKey = computed(() => `recipe-timer-${props.recipeId || 'default'}`)

const isAllCompleted = computed(() => {
  return hasStarted.value && currentStageIndex.value >= stages.value.length
})

const displayStageIndex = computed(() => {
  if (stages.value.length === 0) return -1
  if (currentStageIndex.value >= stages.value.length) return -1
  if (currentStageIndex.value < 0) return 0
  return currentStageIndex.value
})

const currentStage = computed(() => {
  const idx = displayStageIndex.value
  if (idx < 0 || idx >= stages.value.length) return null
  return stages.value[idx]
})

const displayRemainingTime = computed(() => {
  if (stages.value.length === 0) return 0
  if (!hasStarted.value) return stages.value[0]?.duration || 0
  return remainingTime.value
})

const isCurrentFinished = computed(() => {
  return isAllCompleted.value
})

const isTimeWarning = computed(() => {
  if (!hasStarted.value) return false
  return remainingTime.value > 0 && remainingTime.value <= 60
})

const stageProgress = computed(() => {
  if (!hasStarted.value) return 0
  if (!currentStage.value || currentStage.value.duration === 0) return 0
  const elapsed = currentStage.value.duration - remainingTime.value
  return Math.min(100, Math.max(0, (elapsed / currentStage.value.duration) * 100))
})

const progressColor = computed(() => {
  if (remainingTime.value <= 0) return '#67c23a'
  if (remainingTime.value <= 60) return '#f56c6c'
  return '#ff6b6b'
})

const totalDuration = computed(() => {
  return stages.value.reduce((sum, stage) => sum + stage.duration, 0)
})

const formatTime = (seconds) => {
  if (seconds < 0) seconds = 0
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const generateId = () => {
  return Date.now().toString(36) + Math.random().toString(36).substr(2)
}

const saveToStorage = () => {
  const data = {
    stages: stages.value,
    currentStageIndex: currentStageIndex.value,
    isRunning: isRunning.value,
    hasStarted: hasStarted.value,
    endTime: endTime,
    remainingTime: remainingTime.value,
    lastUpdate: Date.now()
  }
  localStorage.setItem(storageKey.value, JSON.stringify(data))
}

const loadFromStorage = () => {
  try {
    const data = localStorage.getItem(storageKey.value)
    if (!data) return false
    const parsed = JSON.parse(data)
    if (!parsed.stages || !parsed.stages.length) return false

    stages.value = parsed.stages
    currentStageIndex.value = parsed.currentStageIndex
    hasStarted.value = parsed.hasStarted
    remainingTime.value = parsed.remainingTime || 0

    if (parsed.isRunning && parsed.endTime) {
      const now = Date.now()
      const timeLeft = Math.floor((parsed.endTime - now) / 1000)

      if (timeLeft > 0) {
        endTime = parsed.endTime
        remainingTime.value = timeLeft
        isRunning.value = true
        startTimerInterval()
      } else {
        isRunning.value = false
        if (currentStageIndex.value < stages.value.length) {
          remainingTime.value = 0
          handleStageComplete()
        }
      }
    }

    return true
  } catch (e) {
    console.error('Failed to load timer from storage:', e)
    return false
  }
}

const startTimerInterval = () => {
  if (timerInterval) clearInterval(timerInterval)
  timerInterval = setInterval(() => {
    if (!endTime) return
    const now = Date.now()
    const timeLeft = Math.floor((endTime - now) / 1000)

    if (timeLeft <= 0) {
      remainingTime.value = 0
      stopTimerInterval()
      handleStageComplete()
    } else {
      remainingTime.value = timeLeft
    }

    saveToStorage()
  }, 250)
}

const stopTimerInterval = () => {
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
}

const startTimer = () => {
  if (currentStageIndex.value < 0 || currentStageIndex.value >= stages.value.length) {
    if (!hasStarted.value && stages.value.length > 0) {
      currentStageIndex.value = 0
      remainingTime.value = stages.value[0].duration
    } else {
      return
    }
  }

  if (remainingTime.value <= 0) {
    remainingTime.value = currentStage.value.duration
  }

  endTime = Date.now() + remainingTime.value * 1000
  isRunning.value = true
  hasStarted.value = true
  startTimerInterval()
  saveToStorage()
}

const pauseTimer = () => {
  isRunning.value = false
  stopTimerInterval()
  if (endTime) {
    remainingTime.value = Math.max(0, Math.floor((endTime - Date.now()) / 1000))
  }
  endTime = null
  saveToStorage()
}

const resetTimer = () => {
  stopTimerInterval()
  isRunning.value = false
  hasStarted.value = false
  currentStageIndex.value = -1
  remainingTime.value = 0
  endTime = null
  saveToStorage()
}

const skipStage = () => {
  if (stages.value.length === 0 || isAllCompleted.value) return

  stopTimerInterval()
  endTime = null

  if (!hasStarted.value) {
    currentStageIndex.value = 1
    if (currentStageIndex.value >= stages.value.length) {
      currentStageIndex.value = stages.value.length
      remainingTime.value = 0
    } else {
      remainingTime.value = stages.value[currentStageIndex.value].duration
    }
  } else {
    currentStageIndex.value++
    if (currentStageIndex.value < stages.value.length) {
      remainingTime.value = stages.value[currentStageIndex.value].duration
      if (isRunning.value) {
        endTime = Date.now() + remainingTime.value * 1000
        startTimerInterval()
      }
    } else {
      isRunning.value = false
      remainingTime.value = 0
    }
  }

  saveToStorage()
}

const handleStageComplete = () => {
  isRunning.value = false
  endTime = null

  const stage = stages.value[currentStageIndex.value]
  if (stage) {
    completedStageName.value = stage.name

    if (stage.soundEnabled) {
      playNotificationSound()
    }

    if (stage.vibrationEnabled && navigator.vibrate) {
      navigator.vibrate([300, 200, 300, 200, 500])
    }

    showCompleteDialog.value = true

    ElNotification({
      title: '阶段完成',
      message: `${stage.name} 计时结束`,
      type: 'success',
      duration: 5000,
      position: 'top-right'
    })
  }
}

const handleCompleteConfirm = () => {
  showCompleteDialog.value = false

  if (currentStageIndex.value < stages.value.length - 1) {
    currentStageIndex.value++
    remainingTime.value = stages.value[currentStageIndex.value].duration
    isRunning.value = true
    endTime = Date.now() + remainingTime.value * 1000
    startTimerInterval()
    saveToStorage()
    ElMessage.info(`开始: ${stages.value[currentStageIndex.value].name}`)
  } else {
    currentStageIndex.value = stages.value.length
    remainingTime.value = 0
    saveToStorage()
    ElMessage.success('所有阶段已完成！')
  }
}

const playNotificationSound = () => {
  try {
    if (!audioContext) {
      audioContext = new (window.AudioContext || window.webkitAudioContext)()
    }

    const playBeep = (frequency, duration, startTime) => {
      const oscillator = audioContext.createOscillator()
      const gainNode = audioContext.createGain()

      oscillator.connect(gainNode)
      gainNode.connect(audioContext.destination)

      oscillator.frequency.value = frequency
      oscillator.type = 'sine'

      gainNode.gain.setValueAtTime(0.3, startTime)
      gainNode.gain.exponentialRampToValueAtTime(0.01, startTime + duration)

      oscillator.start(startTime)
      oscillator.stop(startTime + duration)
    }

    const now = audioContext.currentTime
    playBeep(880, 0.15, now)
    playBeep(880, 0.15, now + 0.25)
    playBeep(1100, 0.3, now + 0.5)
  } catch (e) {
    console.error('Failed to play notification sound:', e)
  }
}

const openAddDialog = () => {
  editingStage.value = null
  stageForm.value = {
    name: '',
    hours: 0,
    minutes: 30,
    seconds: 0,
    soundEnabled: true,
    vibrationEnabled: true
  }
  showAddDialog.value = true
}

const editStage = (stage) => {
  editingStage.value = stage
  const h = Math.floor(stage.duration / 3600)
  const m = Math.floor((stage.duration % 3600) / 60)
  const s = stage.duration % 60
  stageForm.value = {
    name: stage.name,
    hours: h,
    minutes: m,
    seconds: s,
    soundEnabled: stage.soundEnabled,
    vibrationEnabled: stage.vibrationEnabled
  }
  showAddDialog.value = true
}

const saveStage = () => {
  const duration = stageForm.value.hours * 3600 + stageForm.value.minutes * 60 + stageForm.value.seconds

  if (duration <= 0) {
    ElMessage.warning('请设置时长')
    return
  }

  if (!stageForm.value.name.trim()) {
    ElMessage.warning('请输入阶段名称')
    return
  }

  if (editingStage.value) {
    const index = stages.value.findIndex(s => s.id === editingStage.value.id)
    if (index !== -1) {
      stages.value[index].name = stageForm.value.name.trim()
      stages.value[index].duration = duration
      stages.value[index].soundEnabled = stageForm.value.soundEnabled
      stages.value[index].vibrationEnabled = stageForm.value.vibrationEnabled

      if (index === currentStageIndex.value && !isRunning.value) {
        remainingTime.value = duration
      }
    }
    ElMessage.success('已更新')
  } else {
    stages.value.push({
      id: generateId(),
      name: stageForm.value.name.trim(),
      duration: duration,
      soundEnabled: stageForm.value.soundEnabled,
      vibrationEnabled: stageForm.value.vibrationEnabled
    })
    ElMessage.success('已添加')
  }

  showAddDialog.value = false
  editingStage.value = null
  saveToStorage()
}

const deleteStage = (index) => {
  if (index === currentStageIndex.value && isRunning.value) {
    stopTimerInterval()
    isRunning.value = false
    endTime = null
  }

  if (index < currentStageIndex.value) {
    currentStageIndex.value--
  } else if (index === currentStageIndex.value) {
    if (currentStageIndex.value >= stages.value.length - 1) {
      currentStageIndex.value = -1
      remainingTime.value = 0
      hasStarted.value = false
    } else {
      remainingTime.value = stages.value[index + 1]?.duration || 0
    }
  }

  stages.value.splice(index, 1)
  saveToStorage()
  ElMessage.success('已删除')
}

const presets = {
  fermentation: { name: '第一次发酵', duration: 60 * 60, soundEnabled: true, vibrationEnabled: true },
  relax: { name: '中间松弛', duration: 20 * 60, soundEnabled: true, vibrationEnabled: true },
  secondFermentation: { name: '第二次发酵', duration: 45 * 60, soundEnabled: true, vibrationEnabled: true },
  bake: { name: '烘烤', duration: 35 * 60, soundEnabled: true, vibrationEnabled: true },
  cool: { name: '冷却', duration: 30 * 60, soundEnabled: true, vibrationEnabled: true }
}

const addPreset = (type) => {
  const preset = presets[type]
  if (!preset) return

  stages.value.push({
    id: generateId(),
    ...preset
  })

  saveToStorage()
  ElMessage.success(`已添加: ${preset.name}`)
}

const addAllPresets = () => {
  Object.values(presets).forEach(preset => {
    stages.value.push({
      id: generateId(),
      ...preset
    })
  })
  saveToStorage()
  ElMessage.success('已添加全部预设阶段')
}

watch(stages, () => {
  saveToStorage()
}, { deep: true })

onMounted(() => {
  const loaded = loadFromStorage()
  if (!loaded) {
    stages.value = []
  }
})

onUnmounted(() => {
  stopTimerInterval()
  if (audioContext) {
    audioContext.close()
    audioContext = null
  }
})
</script>

<style scoped>
.recipe-timer {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 40px;
}

.timer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0;
  padding-left: 12px;
  border-left: 4px solid #ff6b6b;
}

.timer-display {
  background: linear-gradient(135deg, #fff5f5 0%, #fff0f0 100%);
  border-radius: 12px;
  padding: 32px;
  text-align: center;
  margin-bottom: 24px;
}

.current-stage {
  margin-bottom: 20px;
}

.stage-name {
  font-size: 18px;
  color: #666;
  margin-bottom: 12px;
  font-weight: 500;
}

.countdown {
  font-size: 56px;
  font-weight: 700;
  color: #ff6b6b;
  font-family: 'Courier New', monospace;
  letter-spacing: 2px;
  margin-bottom: 16px;
  line-height: 1.2;
}

.countdown.warning {
  color: #f56c6c;
  animation: pulse 1s ease-in-out infinite;
}

.countdown.finished {
  color: #67c23a;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.stage-progress {
  max-width: 300px;
  margin: 0 auto;
}

.timer-controls {
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.empty-tip {
  padding: 40px 0;
  text-align: center;
  margin-bottom: 24px;
}

.stages-list {
  border-top: 1px solid #f0f0f0;
  padding-top: 20px;
}

.stages-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-size: 15px;
  color: #666;
  font-weight: 500;
}

.total-time {
  color: #ff6b6b;
  font-weight: 600;
}

.stage-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.stage-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fafafa;
  border-radius: 8px;
  border: 2px solid transparent;
  transition: all 0.2s;
}

.stage-item.active {
  background: #f0f9eb;
  border-color: #67c23a;
}

.stage-item.paused {
  background: #fdf6ec;
  border-color: #e6a23c;
}

.stage-item.pending {
  background: #ecf5ff;
  border-color: #409eff;
}

.stage-item.completed {
  background: #f4f4f5;
  opacity: 0.6;
}

.stage-item.current-stage-item {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.stage-index {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #e4e7ed;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.stage-item.active .stage-index {
  background: #67c23a;
  color: #fff;
}

.stage-item.paused .stage-index {
  background: #e6a23c;
  color: #fff;
}

.stage-item.pending .stage-index {
  background: #409eff;
  color: #fff;
}

.stage-item.completed .stage-index {
  background: #c0c4cc;
  color: #fff;
}

.stage-info {
  flex: 1;
  min-width: 0;
}

.stage-title {
  font-size: 15px;
  color: #333;
  font-weight: 500;
  margin-bottom: 2px;
}

.stage-duration {
  font-size: 13px;
  color: #999;
}

.stage-sound {
  flex-shrink: 0;
}

.sound-icon {
  color: #67c23a;
}

.sound-icon.muted {
  color: #c0c4cc;
}

.stage-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.duration-inputs {
  display: flex;
  align-items: center;
  gap: 6px;
}

.duration-label {
  font-size: 14px;
  color: #606266;
}

.complete-dialog :deep(.el-dialog__body) {
  padding-top: 10px;
}

.complete-content {
  text-align: center;
  padding: 20px 0;
}

.complete-icon {
  margin-bottom: 20px;
}

.complete-stage-name {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.complete-tip {
  font-size: 14px;
  color: #999;
}

.preset-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px dashed #e4e7ed;
}

.preset-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 12px;
}

.preset-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 768px) {
  .countdown {
    font-size: 40px;
  }

  .timer-display {
    padding: 24px 16px;
  }

  .timer-controls {
    gap: 8px;
  }

  .timer-controls .el-button {
    padding: 10px 16px;
  }
}

.cooking-mode-timer {
  border: 3px solid #ff6b6b;
  box-shadow: 0 0 20px rgba(255, 107, 107, 0.3);
}

.cooking-timer-display {
  background: linear-gradient(135deg, #fff0f0 0%, #ffe8e8 100%);
  padding: 40px 32px;
}

.cooking-countdown {
  font-size: 72px;
  text-shadow: 0 2px 12px rgba(255, 107, 107, 0.3);
}

.cooking-active-stage {
  border-color: #ff6b6b !important;
  background: #fff5f5 !important;
  box-shadow: 0 0 16px rgba(255, 107, 107, 0.4) !important;
  transform: scale(1.02);
}

@media (max-width: 768px) {
  .cooking-countdown {
    font-size: 48px;
  }
}
</style>
