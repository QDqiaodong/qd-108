<template>
  <div class="calendar-view">
    <div class="calendar-header">
      <div class="header-left">
        <span class="streak-badge">
          <span class="flame-icon">🔥</span>
          <span class="streak-text">连续打卡 {{ streakDays }} 天</span>
        </span>
      </div>
      <div class="header-center">
        <button class="nav-btn" @click="prevMonth">
          <el-icon><ArrowLeft /></el-icon>
        </button>
        <span class="month-title">{{ currentYear }}年 {{ currentMonth }}月</span>
        <button class="nav-btn" @click="nextMonth">
          <el-icon><ArrowRight /></el-icon>
        </button>
      </div>
      <div class="header-right">
        <button class="today-btn" @click="goToToday">今天</button>
      </div>
    </div>

    <div class="calendar-weekdays">
      <div v-for="day in weekDays" :key="day" class="weekday">{{ day }}</div>
    </div>

    <div class="calendar-days">
      <div
        v-for="(day, index) in calendarDays"
        :key="index"
        class="day-cell"
        :class="{
          'other-month': !day.isCurrentMonth,
          'today': day.isToday,
          'checked-in': day.hasCheckIn,
          'streak-day': day.isInStreak,
          'has-plan': day.hasPlan,
          'future-day': day.isFuture
        }"
        @click="handleDayClick(day)"
      >
        <span class="day-number">{{ day.date.getDate() }}</span>
        <div class="day-marks">
          <span v-if="day.hasCheckIn" class="mark checkin-mark">✓</span>
          <span v-if="day.hasPlan" class="mark plan-mark">📌</span>
        </div>
        <div v-if="day.isToday" class="today-ring"></div>
      </div>
    </div>

    <div class="calendar-legend">
      <div class="legend-item">
        <span class="legend-dot checkin-dot"></span>
        <span class="legend-text">已打卡</span>
      </div>
      <div class="legend-item">
        <span class="legend-dot plan-dot"></span>
        <span class="legend-text">计划烘焙</span>
      </div>
      <div class="legend-item">
        <span class="legend-dot streak-dot"></span>
        <span class="legend-text">连续打卡</span>
      </div>
    </div>

    <el-dialog
      v-model="dayDetailVisible"
      :title="selectedDateStr"
      width="500px"
      class="day-detail-dialog"
    >
      <div v-loading="dayDetailLoading" class="day-detail-content">
        <div class="detail-status">
          <div class="status-item">
            <span class="status-label">打卡状态：</span>
            <span class="status-value" :class="{ checked: dayDetail.hasCheckedIn }">
              {{ dayDetail.hasCheckedIn ? '已打卡 ✓' : '未打卡' }}
            </span>
          </div>
        </div>

        <div class="detail-section">
          <h4 class="section-title">
            <span class="section-icon">📝</span>
            发布的配方 ({{ dayDetail.recipes?.length || 0 }})
          </h4>
          <div v-if="dayDetail.recipes && dayDetail.recipes.length > 0" class="recipe-list">
            <div
              v-for="recipe in dayDetail.recipes"
              :key="recipe.id"
              class="recipe-item"
              @click="goToRecipe(recipe.id)"
            >
              <div class="recipe-cover">
                <img v-if="recipe.coverImage" :src="resolveImageUrl(recipe.coverImage)" alt="" />
                <span v-else class="recipe-placeholder">🍰</span>
              </div>
              <div class="recipe-info">
                <div class="recipe-title">{{ recipe.title }}</div>
                <div class="recipe-meta">
                  <span>🔥 {{ recipe.viewCount || 0 }}</span>
                  <span>💬 {{ recipe.commentCount || 0 }}</span>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="empty-section">
            <el-empty description="当天没有发布配方" :image-size="60" />
          </div>
        </div>

        <div class="detail-section">
          <h4 class="section-title">
            <span class="section-icon">💬</span>
            发布的评论 ({{ dayDetail.comments?.length || 0 }})
          </h4>
          <div v-if="dayDetail.comments && dayDetail.comments.length > 0" class="comment-list">
            <div
              v-for="comment in dayDetail.comments"
              :key="comment.id"
              class="comment-item"
            >
              <div class="comment-content">{{ comment.content }}</div>
              <div class="comment-meta">
                <span>👍 {{ comment.likeCount || 0 }}</span>
              </div>
            </div>
          </div>
          <div v-else class="empty-section">
            <el-empty description="当天没有发布评论" :image-size="60" />
          </div>
        </div>

        <div class="detail-section plan-section">
          <h4 class="section-title">
            <span class="section-icon">📌</span>
            烘焙计划
          </h4>
          <div v-if="dayDetail.plan" class="plan-info">
            <div class="plan-item">
              <span class="plan-label">配方名称：</span>
              <span class="plan-value">{{ dayDetail.plan.recipeName || '未设置' }}</span>
            </div>
            <div class="plan-item" v-if="dayDetail.plan.remark">
              <span class="plan-label">备注：</span>
              <span class="plan-value">{{ dayDetail.plan.remark }}</span>
            </div>
            <div class="plan-item">
              <span class="plan-label">提醒：</span>
              <span class="plan-value">
                {{ dayDetail.plan.reminderEnabled === 1 ? '已开启' : '已关闭' }}
              </span>
            </div>
            <div class="plan-actions">
              <el-button size="small" type="primary" @click="openPlanDialog">
                编辑计划
              </el-button>
              <el-button size="small" type="danger" @click="handleDeletePlan">
                删除计划
              </el-button>
            </div>
          </div>
          <div v-else class="no-plan">
            <p>暂无烘焙计划</p>
            <el-button size="small" type="primary" @click="openPlanDialog">
              添加计划
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog
      v-model="planDialogVisible"
      :title="isEditingPlan ? '编辑烘焙计划' : '添加烘焙计划'"
      width="420px"
    >
      <el-form :model="planForm" label-width="80px" class="plan-form">
        <el-form-item label="配方名称">
          <el-input v-model="planForm.recipeName" placeholder="请输入配方名称" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="planForm.remark"
            type="textarea"
            :rows="3"
            placeholder="添加备注信息"
          />
        </el-form-item>
        <el-form-item label="开启提醒">
          <el-switch v-model="planForm.reminderEnabled" />
        </el-form-item>
        <el-form-item label="提醒时间" v-if="planForm.reminderEnabled">
          <el-date-picker
            v-model="planForm.reminderTime"
            type="datetime"
            placeholder="选择提醒时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="planDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePlan" :loading="planSaving">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

const resolveImageUrl = (url) => {
  if (!url) return url
  if (url.startsWith('/uploads/') && !url.startsWith('/api/uploads/')) {
    return '/api' + url
  }
  return url
}

import {
  getCalendarMonthData,
  getCalendarDayDetail,
  createBakePlan,
  updateBakePlan,
  deleteBakePlan
} from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  userId: {
    type: [Number, String],
    required: true
  }
})

const emit = defineEmits(['check-in'])

const router = useRouter()

const weekDays = ['日', '一', '二', '三', '四', '五', '六']

const currentYear = ref(new Date().getFullYear())
const currentMonth = ref(new Date().getMonth() + 1)
const streakDays = ref(0)
const checkInDates = ref([])
const planDates = ref([])

const dayDetailVisible = ref(false)
const dayDetailLoading = ref(false)
const selectedDate = ref(null)
const dayDetail = ref({})

const planDialogVisible = ref(false)
const planSaving = ref(false)
const isEditingPlan = ref(false)
const planForm = ref({
  recipeName: '',
  remark: '',
  reminderEnabled: false,
  reminderTime: null
})

const notificationPermission = ref('default')
const notifiedPlans = ref(new Set())
let reminderTimer = null

const requestNotificationPermission = async () => {
  if (!('Notification' in window)) {
    console.log('浏览器不支持通知')
    return false
  }
  if (Notification.permission === 'granted') {
    notificationPermission.value = 'granted'
    return true
  }
  if (Notification.permission !== 'denied') {
    const permission = await Notification.requestPermission()
    notificationPermission.value = permission
    return permission === 'granted'
  }
  return false
}

const showNotification = (title, body, planId) => {
  if (Notification.permission === 'granted') {
    const notification = new Notification(title, {
      body,
      icon: '🍰',
      badge: '🍰'
    })
    notification.onclick = () => {
      window.focus()
      notification.close()
    }
    setTimeout(() => {
      notification.close()
    }, 10000)
  }
  ElMessage({
    message: title,
    type: 'warning',
    duration: 10000,
    showClose: true
  })
}

const checkReminders = () => {
  const now = new Date()
  const nowTime = now.getTime()

  planDates.value.forEach(plan => {
    if (plan.reminderEnabled !== 1 || !plan.reminderTime) return
    if (notifiedPlans.value.has(plan.id)) return

    const reminderTime = new Date(plan.reminderTime).getTime()
    const diff = reminderTime - nowTime

    if (diff >= 0 && diff <= 60000) {
      const title = '🍰 烘焙计划提醒'
      const body = plan.recipeName
        ? `计划烘焙：${plan.recipeName}`
        : '您有一个烘焙计划即将开始！'
      showNotification(title, body, plan.id)
      notifiedPlans.value.add(plan.id)
    }
  })
}

const startReminderCheck = () => {
  if (reminderTimer) return
  reminderTimer = setInterval(() => {
    checkReminders()
  }, 30000)
}

const stopReminderCheck = () => {
  if (reminderTimer) {
    clearInterval(reminderTimer)
    reminderTimer = null
  }
}

const selectedDateStr = computed(() => {
  if (!selectedDate.value) return ''
  const d = selectedDate.value
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
})

const calendarDays = computed(() => {
  const year = currentYear.value
  const month = currentMonth.value - 1

  const firstDay = new Date(year, month, 1)
  const lastDay = new Date(year, month + 1, 0)
  const startDayOfWeek = firstDay.getDay()
  const daysInMonth = lastDay.getDate()

  const today = new Date()
  today.setHours(0, 0, 0, 0)

  const parseLocalDate = (dateStr) => {
    const parts = dateStr.split('-')
    return new Date(Number(parts[0]), Number(parts[1]) - 1, Number(parts[2]))
  }

  const checkInSet = new Set(checkInDates.value.map(item => {
    const date = parseLocalDate(item.checkDate)
    return `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`
  }))

  const planSet = new Set(planDates.value.map(d => {
    const date = parseLocalDate(d.planDate)
    return `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`
  }))

  const streakDateSet = new Set()
  if (streakDays.value > 0) {
    let checkDate = new Date(today)
    for (let i = 0; i < streakDays.value; i++) {
      const dateStr = `${checkDate.getFullYear()}-${checkDate.getMonth()}-${checkDate.getDate()}`
      if (checkInSet.has(dateStr)) {
        streakDateSet.add(dateStr)
      }
      checkDate.setDate(checkDate.getDate() - 1)
    }
  }

  const days = []
  const prevMonthLastDay = new Date(year, month, 0).getDate()
  for (let i = startDayOfWeek - 1; i >= 0; i--) {
    const date = new Date(year, month - 1, prevMonthLastDay - i)
    const dateStr = `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`
    days.push({
      date,
      isCurrentMonth: false,
      isToday: false,
      hasCheckIn: checkInSet.has(dateStr),
      hasPlan: planSet.has(dateStr),
      isInStreak: streakDateSet.has(dateStr),
      isFuture: date > today
    })
  }

  for (let i = 1; i <= daysInMonth; i++) {
    const date = new Date(year, month, i)
    const dateStr = `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`
    const isToday = date.getTime() === today.getTime()
    days.push({
      date,
      isCurrentMonth: true,
      isToday,
      hasCheckIn: checkInSet.has(dateStr),
      hasPlan: planSet.has(dateStr),
      isInStreak: streakDateSet.has(dateStr),
      isFuture: date > today
    })
  }

  const remainingDays = 42 - days.length
  for (let i = 1; i <= remainingDays; i++) {
    const date = new Date(year, month + 1, i)
    const dateStr = `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`
    days.push({
      date,
      isCurrentMonth: false,
      isToday: false,
      hasCheckIn: checkInSet.has(dateStr),
      hasPlan: planSet.has(dateStr),
      isInStreak: streakDateSet.has(dateStr),
      isFuture: date > today
    })
  }

  return days
})

const loadMonthData = async () => {
  try {
    const year = currentYear.value
    const month = currentMonth.value - 1
    const firstDay = new Date(year, month, 1)
    const startDayOfWeek = firstDay.getDay()
    const calStartDate = new Date(year, month, 1 - startDayOfWeek)
    const calEndDate = new Date(year, month, 1 - startDayOfWeek)
    calEndDate.setDate(calEndDate.getDate() + 41)

    const formatCalDate = (d) => {
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${y}-${m}-${day}`
    }

    const startDateStr = formatCalDate(calStartDate)
    const endDateStr = formatCalDate(calEndDate)

    const res = await getCalendarMonthData(props.userId, currentYear.value, currentMonth.value, startDateStr, endDateStr)
    checkInDates.value = res.checkIns || []
    planDates.value = res.plans || []
    streakDays.value = res.streakDays || 0
  } catch (e) {
    console.error(e)
  }
}

const prevMonth = () => {
  if (currentMonth.value === 1) {
    currentMonth.value = 12
    currentYear.value--
  } else {
    currentMonth.value--
  }
}

const nextMonth = () => {
  if (currentMonth.value === 12) {
    currentMonth.value = 1
    currentYear.value++
  } else {
    currentMonth.value++
  }
}

const goToToday = () => {
  const today = new Date()
  currentYear.value = today.getFullYear()
  currentMonth.value = today.getMonth() + 1
}

const handleDayClick = async (day) => {
  selectedDate.value = day.date
  dayDetailVisible.value = true
  dayDetailLoading.value = true
  try {
    const dateStr = formatDate(day.date)
    const res = await getCalendarDayDetail(props.userId, dateStr)
    dayDetail.value = res
  } catch (e) {
    console.error(e)
    ElMessage.error('获取日期详情失败')
  } finally {
    dayDetailLoading.value = false
  }
}

const formatDate = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const goToRecipe = (id) => {
  router.push(`/recipe/${id}`)
  dayDetailVisible.value = false
}

const openPlanDialog = () => {
  isEditingPlan.value = !!dayDetail.value.plan
  if (dayDetail.value.plan) {
    planForm.value = {
      recipeName: dayDetail.value.plan.recipeName || '',
      remark: dayDetail.value.plan.remark || '',
      reminderEnabled: dayDetail.value.plan.reminderEnabled === 1,
      reminderTime: dayDetail.value.plan.reminderTime || null
    }
  } else {
    planForm.value = {
      recipeName: '',
      remark: '',
      reminderEnabled: false,
      reminderTime: null
    }
  }
  planDialogVisible.value = true
}

const handleSavePlan = async () => {
  planSaving.value = true
  try {
    if (planForm.value.reminderEnabled) {
      await requestNotificationPermission()
    }

    const dateStr = formatDate(selectedDate.value)
    const data = {
      userId: props.userId,
      planDate: dateStr,
      recipeName: planForm.value.recipeName,
      remark: planForm.value.remark,
      reminderEnabled: planForm.value.reminderEnabled ? 1 : 0,
      reminderTime: planForm.value.reminderTime || null
    }

    if (isEditingPlan.value && dayDetail.value.plan) {
      data.id = dayDetail.value.plan.id
      await updateBakePlan(data)
      notifiedPlans.value.delete(dayDetail.value.plan.id)
      ElMessage.success('计划更新成功')
    } else {
      await createBakePlan(data)
      ElMessage.success('计划添加成功')
    }

    planDialogVisible.value = false
    loadMonthData()

    const res = await getCalendarDayDetail(props.userId, dateStr)
    dayDetail.value = res
  } catch (e) {
    console.error(e)
    ElMessage.error('保存计划失败')
  } finally {
    planSaving.value = false
  }
}

const handleDeletePlan = async () => {
  if (!dayDetail.value.plan) return
  try {
    await ElMessageBox.confirm('确定要删除这个烘焙计划吗？', '确认删除', {
      type: 'warning'
    })
    await deleteBakePlan(dayDetail.value.plan.id, props.userId)
    ElMessage.success('计划已删除')
    loadMonthData()

    const dateStr = formatDate(selectedDate.value)
    const res = await getCalendarDayDetail(props.userId, dateStr)
    dayDetail.value = res
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
    }
  }
}

watch(() => [currentYear.value, currentMonth.value], () => {
  loadMonthData()
})

onMounted(() => {
  loadMonthData()
  startReminderCheck()
})

onUnmounted(() => {
  stopReminderCheck()
})

const refreshCalendar = () => {
  loadMonthData()
}

defineExpose({
  refreshCalendar
})
</script>

<style scoped>
.calendar-view {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}

.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  flex: 1;
}

.streak-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #fff5f5 0%, #ffe8e8 100%);
  padding: 8px 16px;
  border-radius: 20px;
}

.flame-icon {
  font-size: 18px;
}

.streak-text {
  font-size: 14px;
  font-weight: 600;
  color: #ff6b6b;
}

.header-center {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: 1;
  justify-content: center;
}

.nav-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: #f5f5f5;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
  color: #666;
}

.nav-btn:hover {
  background: #ff6b6b;
  color: #fff;
}

.month-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  min-width: 120px;
  text-align: center;
}

.header-right {
  flex: 1;
  text-align: right;
}

.today-btn {
  padding: 6px 16px;
  border: 1px solid #ff6b6b;
  background: #fff;
  color: #ff6b6b;
  border-radius: 16px;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.3s;
}

.today-btn:hover {
  background: #ff6b6b;
  color: #fff;
}

.calendar-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: 8px;
}

.weekday {
  text-align: center;
  font-size: 14px;
  color: #999;
  padding: 8px 0;
  font-weight: 500;
}

.calendar-days {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.day-cell {
  aspect-ratio: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
  background: #fafafa;
}

.day-cell:hover {
  background: #fff5f5;
  transform: scale(1.05);
}

.day-cell.other-month {
  opacity: 0.3;
}

.day-cell.other-month:hover {
  transform: none;
  background: #fafafa;
}

.day-number {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.day-marks {
  display: flex;
  gap: 2px;
  margin-top: 2px;
}

.mark {
  font-size: 10px;
  line-height: 1;
}

.checkin-mark {
  color: #52c41a;
}

.plan-mark {
  color: #faad14;
}

.day-cell.today .day-number {
  color: #ff6b6b;
  font-weight: 700;
}

.today-ring {
  position: absolute;
  inset: 2px;
  border: 2px solid #ff6b6b;
  border-radius: 8px;
  pointer-events: none;
}

.day-cell.checked-in {
  background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%);
}

.day-cell.checked-in .day-number {
  color: #389e0d;
  font-weight: 600;
}

.day-cell.streak-day {
  background: linear-gradient(135deg, #fff7e6 0%, #ffd591 100%);
  box-shadow: 0 2px 8px rgba(255, 107, 107, 0.15);
}

.day-cell.streak-day .day-number {
  color: #fa8c16;
  font-weight: 700;
}

.day-cell.has-plan {
  position: relative;
}

.day-cell.future-day:hover {
  background: #e6f7ff;
}

.calendar-legend {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.checkin-dot {
  background: #52c41a;
}

.plan-dot {
  background: #faad14;
}

.streak-dot {
  background: #fa8c16;
}

.legend-text {
  font-size: 12px;
  color: #666;
}

.day-detail-content {
  max-height: 500px;
  overflow-y: auto;
}

.detail-status {
  padding: 12px 16px;
  background: #f9f9f9;
  border-radius: 8px;
  margin-bottom: 16px;
}

.status-item {
  display: flex;
  align-items: center;
}

.status-label {
  font-size: 14px;
  color: #666;
}

.status-value {
  font-size: 14px;
  font-weight: 500;
  color: #999;
}

.status-value.checked {
  color: #52c41a;
}

.detail-section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 0 0 12px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-icon {
  font-size: 16px;
}

.recipe-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.recipe-item {
  display: flex;
  gap: 12px;
  padding: 10px;
  background: #fafafa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.recipe-item:hover {
  background: #fff5f5;
}

.recipe-cover {
  width: 50px;
  height: 50px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.recipe-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.recipe-placeholder {
  font-size: 24px;
}

.recipe-info {
  flex: 1;
  min-width: 0;
}

.recipe-title {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recipe-meta {
  font-size: 12px;
  color: #999;
  display: flex;
  gap: 12px;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.comment-item {
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 8px;
}

.comment-content {
  font-size: 13px;
  color: #333;
  line-height: 1.5;
  margin-bottom: 6px;
  word-break: break-all;
}

.comment-meta {
  font-size: 12px;
  color: #999;
}

.empty-section {
  padding: 20px 0;
}

.plan-section {
  background: #fffbe6;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #ffe58f;
}

.plan-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.plan-item {
  display: flex;
  align-items: flex-start;
}

.plan-label {
  font-size: 13px;
  color: #999;
  flex-shrink: 0;
}

.plan-value {
  font-size: 13px;
  color: #333;
  font-weight: 500;
}

.plan-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.no-plan {
  text-align: center;
  color: #999;
}

.no-plan p {
  margin: 0 0 12px 0;
  font-size: 13px;
}

.plan-form {
  padding: 10px 0;
}

:deep(.day-detail-dialog .el-dialog__body) {
  padding: 20px;
}
</style>
