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
                type="success"
                @click="openTrialReceiptDialog"
              >
                <el-icon style="margin-right: 4px;"><DocumentAdd /></el-icon>
                提交试做回执
              </el-button>
              <el-button
                :type="cookingMode ? 'warning' : 'default'"
                @click="toggleCookingMode"
              >
                <el-icon style="margin-right: 4px;"><Sunny /></el-icon>
                {{ cookingMode ? '退出烹饪模式' : '烹饪模式' }}
              </el-button>
              <el-button
                type="primary"
                @click="enterExecutionMode"
                :disabled="!steps.length"
              >
                <el-icon style="margin-right: 4px;"><VideoPlay /></el-icon>
                执行模式
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
            <div class="stat-item">
              <span class="stat-value">{{ recipe.trialReceiptCount || 0 }}</span>
              <span class="stat-label">试做</span>
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
          <div class="section-header">
            <h2 class="section-title">食材清单</h2>
            <div class="progress-actions">
              <IngredientScaler
                :scale-factor="scaleFactor"
                @increase="increaseScale"
                @decrease="decreaseScale"
                @reset="resetScale"
              />
              <span class="progress-text">已备齐 {{ checkedIngredientCount }}/{{ ingredients.length }} 项</span>
              <el-button size="small" text type="danger" @click="handleResetIngredients" v-if="checkedIngredientCount > 0">
                重置进度
              </el-button>
            </div>
          </div>
          <div class="ingredients-list">
            <div
              v-for="(item, idx) in scaledIngredients"
              :key="idx"
              class="ingredient-item"
              :class="{ 'ingredient-checked': isIngredientChecked(idx) }"
              @click="toggleIngredient(idx)"
            >
              <div class="ingredient-checkbox">
                <el-checkbox :model-value="isIngredientChecked(idx)" @click.stop="toggleIngredient(idx)" />
              </div>
              <span class="ingredient-name">{{ item.name }}</span>
              <div class="ingredient-amount-wrapper">
                <span
                  v-if="item.originalAmount && item.originalAmount !== item.amount"
                  class="ingredient-amount-original"
                >{{ item.originalAmount }}</span>
                <span class="ingredient-amount">{{ item.amount }}</span>
              </div>
            </div>
          </div>
          <div class="ingredients-summary" v-if="checkedIngredientCount === ingredients.length && ingredients.length > 0">
            <el-icon style="color: #67c23a; margin-right: 6px;"><CircleCheckFilled /></el-icon>
            <span>太棒了！所有食材已准备完毕，可以开始制作啦~</span>
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

        <div class="content-section">
          <h2 class="section-title">
            试做回执 ({{ recipe.trialReceiptCount || 0 }})
            <el-button
              type="success"
              size="small"
              style="margin-left: 12px;"
              @click="openTrialReceiptDialog"
            >
              我也试做了
            </el-button>
          </h2>

          <div class="trial-receipts-list" v-loading="trialReceiptsLoading">
            <div v-if="!trialReceipts.length && !trialReceiptsLoading" class="empty-trial-receipts">
              <p>还没有试做回执，快来分享你的试做体验吧！</p>
            </div>
            <div v-for="receipt in trialReceipts" :key="receipt.id" class="trial-receipt-item">
              <div class="trial-receipt-header">
                <el-avatar :size="36" :src="receipt.userAvatar">
                  {{ receipt.userName?.charAt(0) }}
                </el-avatar>
                <div class="trial-receipt-user-info">
                  <span class="trial-receipt-user">{{ receipt.userName }}</span>
                  <span class="trial-receipt-time">{{ receipt.createdAt }}</span>
                </div>
                <el-tag
                  :type="receipt.success === 1 ? 'success' : 'danger'"
                  effect="light"
                  size="small"
                >
                  {{ receipt.success === 1 ? '试做成功' : '试做失败' }}
                </el-tag>
              </div>

              <div class="trial-receipt-rating" v-if="receipt.tasteRating">
                <span class="rating-label">口感评分：</span>
                <el-rate :model-value="receipt.tasteRating" disabled size="small" />
              </div>

              <div class="trial-receipt-fields">
                <div class="trial-receipt-field" v-if="receipt.tasteComment">
                  <span class="field-label">口感评价：</span>
                  <span class="field-value">{{ receipt.tasteComment }}</span>
                </div>
                <div class="trial-receipt-field" v-if="receipt.tempAdjustment">
                  <span class="field-label">温度调整：</span>
                  <span class="field-value">{{ receipt.tempAdjustment }}</span>
                </div>
                <div class="trial-receipt-field" v-if="receipt.moldDifference">
                  <span class="field-label">模具差异：</span>
                  <span class="field-value">{{ receipt.moldDifference }}</span>
                </div>
                <div class="trial-receipt-field" v-if="receipt.notes">
                  <span class="field-label">其他备注：</span>
                  <span class="field-value">{{ receipt.notes }}</span>
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

    <el-dialog
      v-model="showTrialReceiptDialog"
      title="提交试做回执"
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form :model="trialReceiptForm" label-width="100px">
        <el-form-item label="是否成功">
          <el-radio-group v-model="trialReceiptForm.success">
            <el-radio :label="1">成功</el-radio>
            <el-radio :label="0">失败</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="口感评分">
          <el-rate v-model="trialReceiptForm.tasteRating" />
        </el-form-item>

        <el-form-item label="口感评价">
          <el-input
            v-model="trialReceiptForm.tasteComment"
            type="textarea"
            :rows="2"
            placeholder="分享一下口感如何..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="温度调整">
          <el-input
            v-model="trialReceiptForm.tempAdjustment"
            type="textarea"
            :rows="2"
            placeholder="温度做了什么调整？比如降低了10度..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="模具差异">
          <el-input
            v-model="trialReceiptForm.moldDifference"
            type="textarea"
            :rows="2"
            placeholder="用了什么模具？和配方有何不同..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="其他备注">
          <el-input
            v-model="trialReceiptForm.notes"
            type="textarea"
            :rows="3"
            placeholder="其他想分享的内容..."
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showTrialReceiptDialog = false">取消</el-button>
        <el-button type="primary" :loading="submittingTrialReceipt" @click="submitTrialReceipt">
          提交
        </el-button>
      </template>
    </el-dialog>

    <AchievementUnlock v-model:visible="showAchievementUnlock" :achievements="newlyUnlocked" />

    <ExecutionModeOverlay
      :active="executionActive"
      :recipe-title="recipe?.title || ''"
      :step-index="executionStepIndex"
      :total-steps="totalSteps"
      :current-step="currentExecStep"
      :remaining-steps="remainingSteps"
      :progress-percent="progressPercent"
      :is-last-step="isLastStep"
      :is-all-done="isAllDone"
      :bake-temp="recipe?.bakeTemp"
      :bake-time="recipe?.bakeTime"
      @next="goNext"
      @prev="goPrev"
      @exit="exitExecutionMode"
      @go-to="goToStep"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Star, StarFilled, Sunny, DocumentAdd, CircleCheckFilled, VideoPlay } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useIngredientScaler } from '@/composables/useIngredientScaler'
import { useExecutionMode } from '@/composables/useExecutionMode'
import RecipeTimer from '@/components/RecipeTimer.vue'
import AchievementUnlock from '@/components/AchievementUnlock.vue'
import IngredientScaler from '@/components/IngredientScaler.vue'
import ExecutionModeOverlay from '@/components/ExecutionModeOverlay.vue'
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
  resetRecipeProgress,
  getTrialReceipts,
  addTrialReceipt
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

const showTrialReceiptDialog = ref(false)
const trialReceipts = ref([])
const trialReceiptsLoading = ref(false)
const trialReceiptForm = ref({
  success: 1,
  tasteRating: 0,
  tasteComment: '',
  tempAdjustment: '',
  moldDifference: '',
  notes: ''
})
const submittingTrialReceipt = ref(false)

const completedSteps = ref(new Set())
const stepRefs = ref([])
const isProgressLoaded = ref(false)

const checkedIngredients = ref(new Set())
const isIngredientsLoaded = ref(false)

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

const {
  scaleFactor,
  scaledIngredients,
  increaseScale,
  decreaseScale,
  resetScale
} = useIngredientScaler(() => ingredients.value)

const {
  executionActive,
  executionStepIndex,
  currentStep: currentExecStep,
  totalSteps,
  remainingSteps,
  progressPercent,
  isLastStep,
  isAllDone,
  enterExecutionMode,
  exitExecutionMode,
  goNext,
  goPrev,
  goToStep
} = useExecutionMode(() => steps.value, completedSteps)

watch(executionActive, (val) => {
  if (val) {
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
    saveProgress()
  }
})

const completedStepCount = computed(() => completedSteps.value.size)

const checkedIngredientCount = computed(() => checkedIngredients.value.size)

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

const getIngredientStorageKey = () => {
  return `recipe_ingredients_${route.params.id}`
}

const isIngredientChecked = (idx) => {
  return checkedIngredients.value.has(idx)
}

const toggleIngredient = (idx) => {
  if (checkedIngredients.value.has(idx)) {
    checkedIngredients.value.delete(idx)
  } else {
    checkedIngredients.value.add(idx)
  }
  checkedIngredients.value = new Set(checkedIngredients.value)
  saveIngredientsProgress()
}

const saveIngredientsProgress = () => {
  if (!recipe.value) return
  const ingredientArray = Array.from(checkedIngredients.value).sort((a, b) => a - b)
  try {
    localStorage.setItem(getIngredientStorageKey(), JSON.stringify(ingredientArray))
  } catch (e) {
    console.error('本地保存食材进度失败', e)
  }
}

const loadIngredientsProgress = () => {
  if (!recipe.value) return
  try {
    const localData = localStorage.getItem(getIngredientStorageKey())
    if (localData) {
      const ingredientArray = JSON.parse(localData)
      checkedIngredients.value = new Set(ingredientArray)
    }
    isIngredientsLoaded.value = true
  } catch (e) {
    console.error('加载食材进度失败', e)
    isIngredientsLoaded.value = true
  }
}

const handleResetIngredients = async () => {
  try {
    await ElMessageBox.confirm('确定要重置食材准备进度吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    localStorage.removeItem(getIngredientStorageKey())
    checkedIngredients.value = new Set()
    ElMessage.success('食材进度已重置')
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
    }
  }
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
  loadTrialReceipts()
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
    loadIngredientsProgress()
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

const loadTrialReceipts = async () => {
  trialReceiptsLoading.value = true
  try {
    const res = await getTrialReceipts({ recipeId: route.params.id, pageNum: 1, pageSize: 20 })
    trialReceipts.value = res.records || []
  } catch (e) {
    console.error(e)
  } finally {
    trialReceiptsLoading.value = false
  }
}

const openTrialReceiptDialog = () => {
  if (!userStore.isLogin) {
    showLogin.value = true
    return
  }
  trialReceiptForm.value = {
    success: 1,
    tasteRating: 0,
    tasteComment: '',
    tempAdjustment: '',
    moldDifference: '',
    notes: ''
  }
  showTrialReceiptDialog.value = true
}

const submitTrialReceipt = async () => {
  submittingTrialReceipt.value = true
  try {
    const form = trialReceiptForm.value
    await addTrialReceipt({
      recipeId: route.params.id,
      userId: userStore.userInfo.id,
      success: form.success,
      tasteRating: form.tasteRating || null,
      tasteComment: form.tasteComment.trim() || null,
      tempAdjustment: form.tempAdjustment.trim() || null,
      moldDifference: form.moldDifference.trim() || null,
      notes: form.notes.trim() || null
    })
    recipe.value.trialReceiptCount = (recipe.value.trialReceiptCount || 0) + 1
    showTrialReceiptDialog.value = false
    ElMessage.success('试做回执提交成功')
    loadTrialReceipts()
  } catch (e) {
    console.error(e)
    ElMessage.error('提交失败，请重试')
  } finally {
    submittingTrialReceipt.value = false
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
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 8px;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: all 0.25s ease;
  border: 2px solid transparent;
}

.ingredient-item:last-child {
  margin-bottom: 0;
}

.ingredient-item:hover {
  background: #fff9f3;
  border-color: #ffe0cc;
}

.ingredient-item.ingredient-checked {
  background: #f0f9eb;
  border-color: #e1f3d8;
}

.ingredient-item.ingredient-checked .ingredient-name {
  text-decoration: line-through;
  color: #999;
}

.ingredient-item.ingredient-checked .ingredient-amount {
  color: #67c23a;
}

.ingredient-checkbox {
  display: flex;
  align-items: center;
  margin-right: 12px;
  flex-shrink: 0;
}

.ingredient-name {
  color: #333;
  flex: 1;
  font-size: 15px;
  transition: color 0.25s ease;
}

.ingredient-amount {
  color: #ff6b6b;
  font-weight: 500;
  flex-shrink: 0;
  margin-left: 16px;
  transition: color 0.25s ease;
}

.ingredient-amount-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  margin-left: auto;
}

.ingredient-amount-original {
  color: #c0c4cc;
  text-decoration: line-through;
  font-size: 13px;
  font-weight: 400;
}

.ingredients-summary {
  margin-top: 16px;
  padding: 14px 20px;
  background: linear-gradient(135deg, #f0f9eb 0%, #e8f7de 100%);
  border-radius: 8px;
  color: #67c23a;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: summary-appear 0.4s ease;
}

@keyframes summary-appear {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
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

.trial-receipts-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.empty-trial-receipts {
  text-align: center;
  padding: 40px 0;
  color: #999;
}

.empty-trial-receipts p {
  font-size: 14px;
}

.trial-receipt-item {
  padding: 20px;
  background: #faf7f2;
  border-radius: 12px;
  border: 1px solid #f0ebe3;
}

.trial-receipt-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.trial-receipt-user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.trial-receipt-user {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.trial-receipt-time {
  font-size: 12px;
  color: #999;
}

.trial-receipt-rating {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  padding-left: 48px;
}

.rating-label {
  font-size: 13px;
  color: #666;
  margin-right: 8px;
}

.trial-receipt-fields {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-left: 48px;
}

.trial-receipt-field {
  display: flex;
  gap: 8px;
  font-size: 14px;
  line-height: 1.7;
}

.field-label {
  color: #666;
  flex-shrink: 0;
  font-weight: 500;
}

.field-value {
  color: #333;
  flex: 1;
  word-break: break-word;
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

  .detail-header {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .recipe-stats {
    gap: 20px;
    flex-wrap: wrap;
  }

  .actions {
    flex-wrap: wrap;
  }

  .trial-receipt-rating,
  .trial-receipt-fields {
    padding-left: 0;
    margin-top: 12px;
  }

  .trial-receipt-item {
    padding: 16px;
  }
}
</style>
