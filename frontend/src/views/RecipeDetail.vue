<template>
  <div class="recipe-detail" :class="{ 'cooking-mode': cookingMode }" v-loading="loading">
    <div class="container" v-if="recipe">
      <div class="detail-header">
        <div class="recipe-gallery">
          <div class="main-image">
            <img v-if="recipe.coverImage" :src="recipe.coverImage" :alt="recipe.title" />
            <div v-else class="image-placeholder" style="height: 400px;"></div>
          </div>
          <div class="thumb-list" v-if="recipe.images && recipe.images.length">
            <div
              v-for="(img, idx) in recipe.images"
              :key="img.id"
              class="thumb-item"
              :class="{ active: currentImage === idx }"
              @click="currentImage = idx"
            >
              <img :src="img.imageUrl" />
            </div>
          </div>
        </div>

        <div class="recipe-info">
          <h1 class="recipe-title">{{ recipe.title }}</h1>
          <div class="recipe-meta-top">
            <div class="author">
              <el-avatar :size="40" :src="recipe.authorAvatar">
                {{ recipe.authorName?.charAt(0) }}
              </el-avatar>
              <div class="author-info">
                <span class="author-name">{{ recipe.authorName }}</span>
                <span class="publish-time">{{ recipe.createdAt }}</span>
              </div>
            </div>
            <div class="actions">
              <el-button
                :type="cookingMode ? 'warning' : 'default'"
                @click="toggleCookingMode"
              >
                <el-icon style="margin-right: 4px;"><Sunny /></el-icon>
                {{ cookingMode ? '退出烹饪模式' : '烹饪模式' }}
              </el-button>
              <el-button
                :type="isFavorited ? 'danger' : 'default'"
                @click="toggleFavorite"
                :icon="isFavorited ? StarFilled : Star"
              >
                {{ isFavorited ? '已收藏' : '收藏' }}
              </el-button>
            </div>
          </div>

          <p class="recipe-description" v-if="recipe.description">{{ recipe.description }}</p>

          <div class="recipe-stats">
            <div class="stat-item">
              <span class="stat-value">{{ recipe.viewCount || 0 }}</span>
              <span class="stat-label">浏览</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ recipe.favoriteCount || 0 }}</span>
              <span class="stat-label">收藏</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ recipe.commentCount || 0 }}</span>
              <span class="stat-label">评论</span>
            </div>
          </div>

          <div class="recipe-tags">
            <el-tag v-if="recipe.categoryName" type="warning" effect="light">{{ recipe.categoryName }}</el-tag>
            <el-tag v-if="recipe.difficulty" type="info" effect="light">
              {{ getDifficultyText(recipe.difficulty) }}
            </el-tag>
            <el-tag v-if="recipe.bakeTime" type="success" effect="light" :class="{ 'cooking-highlight-tag': cookingMode }">
              {{ recipe.bakeTime }}分钟
            </el-tag>
            <el-tag v-if="recipe.bakeTemp" type="danger" effect="light" :class="{ 'cooking-highlight-tag': cookingMode }">
              {{ recipe.bakeTemp }}℃
            </el-tag>
            <el-tag v-if="recipe.servings" type="primary" effect="light">
              {{ recipe.servings }}人份
            </el-tag>
          </div>
        </div>
      </div>

      <div class="detail-content">
        <div class="content-section" v-if="ingredients.length">
          <h2 class="section-title">食材清单</h2>
          <div class="ingredients-list">
            <div v-for="(item, idx) in ingredients" :key="idx" class="ingredient-item">
              <span class="ingredient-name">{{ item.name }}</span>
              <span class="ingredient-amount">{{ item.amount }}</span>
            </div>
          </div>
        </div>

        <RecipeTimer :recipe-id="recipe.id" :cooking-mode="cookingMode" />

        <div class="content-section" v-if="steps.length">
          <div class="section-header">
            <h2 class="section-title">制作步骤</h2>
            <div class="progress-actions">
              <span class="progress-text">已完成 {{ completedStepCount }}/{{ steps.length }} 步</span>
              <el-button size="small" text type="danger" @click="handleResetProgress">
                重置进度
              </el-button>
            </div>
          </div>
          <div class="steps-list">
            <div
              v-for="(step, idx) in steps"
              :key="idx"
              :ref="el => setStepRef(el, idx)"
              class="step-item"
              :class="{
                'step-completed': isStepCompleted(idx),
                'step-current': isCurrentStep(idx),
                'cooking-step-current': cookingMode && isCurrentStep(idx)
              }"
              @click="toggleStep(idx)"
            >
              <div class="step-checkbox">
                <el-checkbox :model-value="isStepCompleted(idx)" @click.stop="toggleStep(idx)" />
              </div>
              <div class="step-number">{{ idx + 1 }}</div>
              <div class="step-content">
                <p>{{ step.description }}</p>
                <img v-if="step.image" :src="step.image" />
              </div>
            </div>
          </div>
        </div>

        <div class="content-section">
          <h2 class="section-title">制作心得 ({{ recipe.commentCount || 0 }})</h2>
          <div class="comment-input">
            <el-input
              v-model="commentContent"
              type="textarea"
              :rows="3"
              placeholder="分享你的制作心得..."
            />
            <div class="comment-actions">
              <el-button type="primary" @click="submitComment" :disabled="!commentContent.trim()">
                发布评论
              </el-button>
            </div>
          </div>

          <div class="comments-list" v-loading="commentsLoading">
            <div v-for="comment in comments" :key="comment.id" class="comment-item">
              <div class="comment-header">
                <el-avatar :size="36" :src="comment.userAvatar">
                  {{ comment.userName?.charAt(0) }}
                </el-avatar>
                <div class="comment-info">
                  <span class="comment-user">{{ comment.userName }}</span>
                  <span class="comment-time">{{ comment.createdAt }}</span>
                </div>
                <el-button
                  text
                  size="small"
                  @click="likeComment(comment.id)"
                >
                  {{ comment.likeCount || 0 }}
                </el-button>
              </div>
              <p class="comment-content">{{ comment.content }}</p>
              <div class="comment-replies" v-if="comment.replies && comment.replies.length">
                <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                  <el-avatar :size="28" :src="reply.userAvatar">
                    {{ reply.userName?.charAt(0) }}
                  </el-avatar>
                  <div class="reply-content">
                    <span class="reply-user">{{ reply.userName }}</span>
                    <span>{{ reply.content }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <Transition name="cooking-bar">
      <div class="cooking-info-bar" v-if="cookingMode && recipe">
        <div class="cooking-bar-inner">
          <div class="cooking-bar-item cooking-bar-step" v-if="currentStepText">
            <span class="cooking-bar-label">当前步骤</span>
            <span class="cooking-bar-value">{{ currentStepText }}</span>
          </div>
          <div class="cooking-bar-item cooking-bar-temp" v-if="recipe.bakeTemp">
            <span class="cooking-bar-label">烘烤温度</span>
            <span class="cooking-bar-value">{{ recipe.bakeTemp }}℃</span>
          </div>
          <div class="cooking-bar-item cooking-bar-timer" v-if="timerRemaining >= 0">
            <span class="cooking-bar-label">剩余计时</span>
            <span class="cooking-bar-value" :class="{ 'timer-warning': timerRemaining <= 60 && timerRemaining > 0 }">{{ formatTimerDisplay(timerRemaining) }}</span>
          </div>
          <el-button
            type="warning"
            size="small"
            class="cooking-bar-close"
            @click="toggleCookingMode"
          >
            退出烹饪
          </el-button>
        </div>
      </div>
    </Transition>

    <AchievementUnlock v-model:visible="showAchievementUnlock" :achievements="newlyUnlocked" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, StarFilled, Sunny } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import RecipeTimer from '@/components/RecipeTimer.vue'
import AchievementUnlock from '@/components/AchievementUnlock.vue'
import {
  getRecipeDetail,
  checkFavorite,
  addFavorite,
  removeFavorite,
  getComments,
  addComment,
  likeComment as likeCommentApi,
  getRecipeProgress,
  saveRecipeProgress,
  resetRecipeProgress
} from '@/api'

const route = useRoute()
const userStore = useUserStore()
const recipe = ref(null)
const loading = ref(false)
const currentImage = ref(0)
const isFavorited = ref(false)
const commentContent = ref('')
const comments = ref([])
const commentsLoading = ref(false)
const showLogin = ref(false)
const showAchievementUnlock = ref(false)
const newlyUnlocked = ref([])

const completedSteps = ref(new Set())
const stepRefs = ref([])
const isProgressLoaded = ref(false)

const cookingMode = ref(false)
let wakeLock = null
const timerRemaining = ref(-1)
let timerSyncInterval = null

const ingredients = computed(() => {
  if (!recipe.value?.ingredients) return []
  try {
    return JSON.parse(recipe.value.ingredients)
  } catch {
    return []
  }
})

const steps = computed(() => {
  if (!recipe.value?.steps) return []
  try {
    return JSON.parse(recipe.value.steps)
  } catch {
    return []
  }
})

const completedStepCount = computed(() => completedSteps.value.size)

const firstUncompletedIndex = computed(() => {
  const total = steps.value.length
  for (let i = 0; i < total; i++) {
    if (!completedSteps.value.has(i)) {
      return i
    }
  }
  return -1
})

const setStepRef = (el, idx) => {
  if (el) {
    stepRefs.value[idx] = el
  }
}

const isStepCompleted = (idx) => {
  return completedSteps.value.has(idx)
}

const isCurrentStep = (idx) => {
  return idx === firstUncompletedIndex.value
}

const toggleStep = (idx) => {
  if (completedSteps.value.has(idx)) {
    completedSteps.value.delete(idx)
  } else {
    completedSteps.value.add(idx)
  }
  completedSteps.value = new Set(completedSteps.value)
  saveProgress()
}

const getLocalStorageKey = () => {
  return `recipe_progress_${route.params.id}`
}

const saveProgress = () => {
  if (!recipe.value) return
  const stepArray = Array.from(completedSteps.value).sort((a, b) => a - b)
  const lastStepIndex = stepArray.length > 0 ? Math.max(...stepArray) : 0

  if (userStore.isLogin) {
    saveRecipeProgress({
      userId: userStore.userInfo.id,
      recipeId: recipe.value.id,
      completedSteps: JSON.stringify(stepArray),
      lastStepIndex: lastStepIndex
    }).catch(e => console.error('保存进度失败', e))
  } else {
    try {
      localStorage.setItem(getLocalStorageKey(), JSON.stringify(stepArray))
    } catch (e) {
      console.error('本地保存进度失败', e)
    }
  }
}

const loadProgress = async () => {
  if (!recipe.value) return
  try {
    if (userStore.isLogin) {
      const progress = await getRecipeProgress(userStore.userInfo.id, recipe.value.id)
      if (progress && progress.completedSteps) {
        const stepArray = JSON.parse(progress.completedSteps)
        completedSteps.value = new Set(stepArray)
      }
    } else {
      const localData = localStorage.getItem(getLocalStorageKey())
      if (localData) {
        const stepArray = JSON.parse(localData)
        completedSteps.value = new Set(stepArray)
      }
    }
    isProgressLoaded.value = true
    nextTick(() => {
      scrollToFirstUncompleted()
    })
  } catch (e) {
    console.error('加载进度失败', e)
    isProgressLoaded.value = true
  }
}

const scrollToFirstUncompleted = () => {
  if (firstUncompletedIndex.value >= 0 && stepRefs.value[firstUncompletedIndex.value]) {
    stepRefs.value[firstUncompletedIndex.value].scrollIntoView({
      behavior: 'smooth',
      block: 'center'
    })
  }
}

const handleResetProgress = async () => {
  try {
    await ElMessageBox.confirm('确定要重置制作进度吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    if (userStore.isLogin) {
      await resetRecipeProgress(userStore.userInfo.id, recipe.value.id)
    } else {
      localStorage.removeItem(getLocalStorageKey())
    }
    completedSteps.value = new Set()
    ElMessage.success('进度已重置')
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
    }
  }
}

const getDifficultyText = (level) => {
  const map = { 1: '简单', 2: '中等', 3: '困难' }
  return map[level] || '简单'
}

const currentStepText = computed(() => {
  if (!steps.value.length) return ''
  const idx = firstUncompletedIndex.value
  if (idx < 0) return '全部完成'
  const desc = steps.value[idx]?.description || ''
  return `第${idx + 1}步: ${desc.length > 20 ? desc.slice(0, 20) + '...' : desc}`
})

const formatTimerDisplay = (seconds) => {
  if (seconds < 0) return '--:--:--'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

const requestWakeLock = async () => {
  try {
    if ('wakeLock' in navigator) {
      wakeLock = await navigator.wakeLock.request('screen')
      wakeLock.addEventListener('release', () => {
        if (cookingMode.value) {
          cookingMode.value = false
        }
      })
    }
  } catch (e) {
    console.error('Wake Lock request failed:', e)
  }
}

const releaseWakeLock = async () => {
  try {
    if (wakeLock) {
      await wakeLock.release()
      wakeLock = null
    }
  } catch (e) {
    console.error('Wake Lock release failed:', e)
  }
}

const syncTimerFromStorage = () => {
  try {
    const key = `recipe-timer-${route.params.id || 'default'}`
    const data = localStorage.getItem(key)
    if (!data) {
      timerRemaining.value = -1
      return
    }
    const parsed = JSON.parse(data)
    if (!parsed.hasStarted || !parsed.stages?.length) {
      timerRemaining.value = -1
      return
    }
    if (parsed.isRunning && parsed.endTime) {
      const timeLeft = Math.floor((parsed.endTime - Date.now()) / 1000)
      timerRemaining.value = Math.max(0, timeLeft)
    } else {
      timerRemaining.value = parsed.remainingTime || 0
    }
  } catch {
    timerRemaining.value = -1
  }
}

const toggleCookingMode = async () => {
  if (cookingMode.value) {
    cookingMode.value = false
    await releaseWakeLock()
    if (timerSyncInterval) {
      clearInterval(timerSyncInterval)
      timerSyncInterval = null
    }
    timerRemaining.value = -1
    ElMessage.success('已退出烹饪模式')
  } else {
    cookingMode.value = true
    await requestWakeLock()
    syncTimerFromStorage()
    timerSyncInterval = setInterval(syncTimerFromStorage, 1000)
    ElMessage.success('已开启烹饪模式，屏幕将保持常亮')
  }
}

const handleVisibilityChange = async () => {
  if (document.visibilityState === 'visible' && cookingMode.value && !wakeLock) {
    await requestWakeLock()
  }
}

onMounted(() => {
  loadRecipe()
  loadComments()
  document.addEventListener('visibilitychange', handleVisibilityChange)
})

onUnmounted(() => {
  if (cookingMode.value) {
    releaseWakeLock()
  }
  if (timerSyncInterval) {
    clearInterval(timerSyncInterval)
    timerSyncInterval = null
  }
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})

const loadRecipe = async () => {
  loading.value = true
  try {
    recipe.value = await getRecipeDetail(route.params.id)
    if (userStore.isLogin) {
      isFavorited.value = await checkFavorite(userStore.userInfo.id, route.params.id)
    }
    loadProgress()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const toggleFavorite = async () => {
  if (!userStore.isLogin) {
    showLogin.value = true
    return
  }
  try {
    if (isFavorited.value) {
      await removeFavorite(userStore.userInfo.id, route.params.id)
      isFavorited.value = false
      recipe.value.favoriteCount--
      ElMessage.success('已取消收藏')
    } else {
      const result = await addFavorite({ userId: userStore.userInfo.id, recipeId: route.params.id })
      isFavorited.value = true
      recipe.value.favoriteCount++
      ElMessage.success('收藏成功')

      if (result.newlyUnlocked && result.newlyUnlocked.length > 0) {
        newlyUnlocked.value = result.newlyUnlocked
        showAchievementUnlock.value = true
      }
    }
  } catch (e) {
    console.error(e)
  }
}

const loadComments = async () => {
  commentsLoading.value = true
  try {
    const res = await getComments({ recipeId: route.params.id, pageNum: 1, pageSize: 20 })
    comments.value = res.records || []
  } catch (e) {
    console.error(e)
  } finally {
    commentsLoading.value = false
  }
}

const submitComment = async () => {
  if (!userStore.isLogin) {
    showLogin.value = true
    return
  }
  if (!commentContent.value.trim()) return
  try {
    await addComment({
      recipeId: route.params.id,
      userId: userStore.userInfo.id,
      content: commentContent.value.trim()
    })
    commentContent.value = ''
    recipe.value.commentCount++
    ElMessage.success('评论成功')
    loadComments()
  } catch (e) {
    console.error(e)
  }
}

const likeComment = async (id) => {
  try {
    await likeCommentApi(id)
    const comment = comments.value.find(c => c.id === id)
    if (comment) comment.likeCount++
  } catch (e) {
    console.error(e)
  }
}
</script>

<style scoped>
.recipe-detail {
  min-height: 60vh;
}

.detail-header {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  margin-bottom: 40px;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}

.recipe-gallery {
  position: sticky;
  top: 100px;
}

.main-image {
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 12px;
}

.main-image img {
  width: 100%;
  height: 400px;
  object-fit: cover;
}

.thumb-list {
  display: flex;
  gap: 8px;
  overflow-x: auto;
}

.thumb-item {
  width: 80px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  flex-shrink: 0;
}

.thumb-item.active {
  border-color: #ff6b6b;
}

.thumb-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.recipe-title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
}

.recipe-meta-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.author {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-info {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-size: 15px;
  font-weight: 500;
  color: #333;
}

.publish-time {
  font-size: 13px;
  color: #999;
}

.recipe-description {
  font-size: 15px;
  color: #666;
  line-height: 1.8;
  margin-bottom: 24px;
}

.recipe-stats {
  display: flex;
  gap: 40px;
  margin-bottom: 24px;
  padding: 20px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.stat-label {
  font-size: 13px;
  color: #999;
}

.recipe-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-content {
  background: #fff;
  border-radius: 12px;
  padding: 30px;
}

.content-section {
  margin-bottom: 40px;
}

.content-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
  padding-left: 12px;
  border-left: 4px solid #ff6b6b;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header .section-title {
  margin-bottom: 0;
}

.progress-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-text {
  font-size: 14px;
  color: #666;
}

.ingredients-list {
  background: #faf7f2;
  border-radius: 8px;
  padding: 20px;
}

.ingredient-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px dashed #eee;
}

.ingredient-item:last-child {
  border-bottom: none;
}

.ingredient-name {
  color: #333;
}

.ingredient-amount {
  color: #ff6b6b;
  font-weight: 500;
}

.steps-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.step-item {
  display: flex;
  gap: 12px;
  padding: 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
  background: #fafafa;
}

.step-item:hover {
  background: #f5f5f5;
}

.step-item.step-completed {
  background: #f0f9eb;
  opacity: 0.7;
}

.step-item.step-completed .step-content p {
  text-decoration: line-through;
  color: #999;
}

.step-item.step-current {
  border-color: #ff6b6b;
  background: #fff5f5;
  box-shadow: 0 2px 12px rgba(255, 107, 107, 0.15);
}

.step-checkbox {
  display: flex;
  align-items: flex-start;
  padding-top: 6px;
  flex-shrink: 0;
}

.step-number {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff9a56 0%, #ff6b6b 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  flex-shrink: 0;
}

.step-completed .step-number {
  background: #67c23a;
}

.step-content {
  flex: 1;
  min-width: 0;
}

.step-content p {
  font-size: 15px;
  color: #333;
  line-height: 1.8;
  margin-bottom: 12px;
}

.step-content img {
  max-width: 400px;
  border-radius: 8px;
}

.comment-input {
  margin-bottom: 30px;
}

.comment-actions {
  margin-top: 12px;
  text-align: right;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comment-item {
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.comment-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.comment-user {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-content {
  font-size: 14px;
  color: #666;
  line-height: 1.8;
  margin-left: 48px;
}

.comment-replies {
  margin-top: 16px;
  margin-left: 48px;
  padding: 16px;
  background: #faf7f2;
  border-radius: 8px;
}

.reply-item {
  display: flex;
  gap: 10px;
  padding: 8px 0;
}

.reply-content {
  flex: 1;
  font-size: 13px;
  color: #666;
}

.reply-user {
  font-weight: 500;
  color: #333;
  margin-right: 8px;
}

.cooking-mode .detail-content {
  transition: background 0.3s;
}

.cooking-highlight-tag {
  transform: scale(1.2);
  font-size: 15px;
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(255, 107, 107, 0.4);
  transition: all 0.3s ease;
}

.cooking-step-current {
  border-color: #ff6b6b !important;
  background: #fff0f0 !important;
  box-shadow: 0 4px 20px rgba(255, 107, 107, 0.35) !important;
  transform: scale(1.02);
  position: relative;
}

.cooking-step-current::before {
  content: '当前步骤';
  position: absolute;
  top: -10px;
  left: 16px;
  background: #ff6b6b;
  color: #fff;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
  font-weight: 500;
}

.cooking-step-current .step-number {
  animation: cooking-pulse 1.5s ease-in-out infinite;
}

@keyframes cooking-pulse {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 0 0 0 rgba(255, 107, 107, 0.4);
  }
  50% {
    transform: scale(1.1);
    box-shadow: 0 0 0 8px rgba(255, 107, 107, 0);
  }
}

.cooking-info-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 999;
  background: linear-gradient(135deg, #ff6b6b 0%, #ff9a56 100%);
  color: #fff;
  box-shadow: 0 -4px 20px rgba(255, 107, 107, 0.4);
}

.cooking-bar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 14px 24px;
  display: flex;
  align-items: center;
  gap: 32px;
}

.cooking-bar-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.cooking-bar-label {
  font-size: 12px;
  opacity: 0.85;
  letter-spacing: 1px;
}

.cooking-bar-value {
  font-size: 18px;
  font-weight: 700;
  font-family: 'Courier New', monospace;
}

.cooking-bar-step .cooking-bar-value {
  font-family: inherit;
  font-size: 16px;
  max-width: 260px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cooking-bar-timer .timer-warning {
  animation: bar-timer-blink 0.8s ease-in-out infinite;
}

@keyframes bar-timer-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.cooking-bar-close {
  margin-left: auto;
  background: rgba(255, 255, 255, 0.2) !important;
  border-color: rgba(255, 255, 255, 0.4) !important;
  color: #fff !important;
}

.cooking-bar-close:hover {
  background: rgba(255, 255, 255, 0.35) !important;
}

.cooking-bar-enter-active,
.cooking-bar-leave-active {
  transition: transform 0.35s ease, opacity 0.35s ease;
}

.cooking-bar-enter-from,
.cooking-bar-leave-to {
  transform: translateY(100%);
  opacity: 0;
}

.cooking-mode .container {
  padding-bottom: 80px;
}

@media (max-width: 768px) {
  .cooking-bar-inner {
    gap: 16px;
    padding: 10px 16px;
    flex-wrap: wrap;
  }

  .cooking-bar-value {
    font-size: 15px;
  }

  .cooking-bar-step .cooking-bar-value {
    max-width: 160px;
  }

  .cooking-bar-close {
    margin-left: 0;
  }
}
</style>
