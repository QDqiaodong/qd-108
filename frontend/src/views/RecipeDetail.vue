<template>
  <div class="recipe-detail" :class="{ 'cooking-mode': cookingMode }" v-loading="loading">
    <div class="error-placeholder" v-if="!recipe && !loading" :style="{ padding: '80px 24px', textAlign: 'center' }">
      <el-empty :description="recipeError || '配方不存在或加载失败'">
        <el-button type="primary" @click="loadRecipe">重新加载</el-button>
      </el-empty>
    </div>
    <div class="container" v-if="recipe">
      <div class="detail-header">
        <div class="recipe-gallery">
          <div class="main-image">
            <img v-if="recipe.coverImage" :src="resolveImageUrl(recipe.coverImage)" :alt="recipe.title" />
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
              <img :src="resolveImageUrl(img.imageUrl)" />
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
                :loading="favoriteLoading"
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

          <div class="bake-stats-reference" v-if="bakeStats || bakeStatsLoading || bakeStatsError" v-loading="bakeStatsLoading">
            <div v-if="bakeStatsError && !bakeStatsLoading" class="section-error">
              <el-empty :description="'同品类参考加载失败：' + bakeStatsError" :image-size="60">
                <el-button size="small" type="primary" @click="loadBakeStats">重试</el-button>
              </el-empty>
            </div>
            <template v-else-if="bakeStats">
            <div class="stats-header">
              <el-icon style="margin-right: 6px;"><DataAnalysis /></el-icon>
              <span class="stats-title">同品类参数参考</span>
              <span class="stats-sample">（基于 {{ bakeStats.sampleCount }} 个配方）</span>
            </div>

            <div class="deviation-warnings" v-if="hasDeviation">
              <el-alert v-if="tempDeviationNote" :title="tempDeviationNote" type="warning" :closable="false" show-icon size="small" style="margin-bottom: 8px;" />
              <el-alert v-if="timeDeviationNote" :title="timeDeviationNote" type="warning" :closable="false" show-icon size="small" />
            </div>

            <div class="stats-grid">
              <div class="stats-item">
                <div class="stats-item-label">
                  <el-icon style="color: #f56c6c; margin-right: 4px;"><HotWater /></el-icon>
                  常见温度
                </div>
                <div class="stats-item-value">
                  <span :class="{ 'value-normal': !isTempDeviated, 'value-deviated': isTempDeviated }">{{ bakeStats.tempP25 }}~{{ bakeStats.tempP75 }}℃</span>
                </div>
                <div class="stats-item-range">范围 {{ bakeStats.tempMin }}~{{ bakeStats.tempMax }}℃</div>
              </div>
              <div class="stats-item">
                <div class="stats-item-label">
                  <el-icon style="color: #67c23a; margin-right: 4px;"><Timer /></el-icon>
                  常见时长
                </div>
                <div class="stats-item-value">
                  <span :class="{ 'value-normal': !isTimeDeviated, 'value-deviated': isTimeDeviated }">{{ bakeStats.timeP25 }}~{{ bakeStats.timeP75 }}分钟</span>
                </div>
                <div class="stats-item-range">范围 {{ bakeStats.timeMin }}~{{ bakeStats.timeMax }}分钟</div>
              </div>
              <div class="stats-item">
                <div class="stats-item-label">
                  <el-icon style="color: #e6a23c; margin-right: 4px;"><TrendCharts /></el-icon>
                  平均温度
                </div>
                <div class="stats-item-value">{{ Math.round(bakeStats.tempAvg) }}℃</div>
                <div class="stats-item-range">平均时长 {{ Math.round(bakeStats.timeAvg) }}分钟</div>
              </div>
            </div>
            </template>
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
                <img v-if="step.image" :src="resolveImageUrl(step.image)" />
              </div>
            </div>
          </div>
        </div>

        <div class="content-section">
          <div class="section-header">
            <h2 class="section-title">做法变体</h2>
            <el-button
              type="success"
              size="small"
              @click="openVariationNoteDialog"
            >
              分享变体经验
            </el-button>
          </div>
          <p class="variation-desc" v-if="!variationTopicKeys.length">试做者可围绕"换面粉""减糖""空气炸锅替代"等变体提交简短经验，同一配方自然沉淀出多个家庭场景下的做法分支。</p>

          <div v-loading="variationNotesLoading">
            <div v-if="variationNotesError && !variationNotesLoading" class="section-error" style="padding: 30px 0;">
              <el-empty :description="'做法变体加载失败：' + variationNotesError" :image-size="60">
                <el-button size="small" type="primary" @click="loadVariationNotes">重试</el-button>
              </el-empty>
            </div>
            <template v-else>
              <div v-for="topic in variationTopicKeys" :key="topic" class="variation-topic-group">
                <div class="variation-topic-header">
                  <el-tag effect="dark" size="large" class="variation-topic-tag">{{ topic }}</el-tag>
                  <span class="variation-topic-count">{{ variationNotesGrouped[topic].length }} 条经验</span>
                </div>
                <div class="variation-notes-list">
                  <div v-for="note in variationNotesGrouped[topic]" :key="note.id" class="variation-note-item">
                    <div class="variation-note-header">
                      <el-avatar :size="32" :src="note.userAvatar">
                        {{ note.userName?.charAt(0) }}
                      </el-avatar>
                      <div class="variation-note-user-info">
                        <span class="variation-note-user">{{ note.userName }}</span>
                        <span class="variation-note-time">{{ note.createdAt }}</span>
                      </div>
                      <el-button text size="small" @click="likeVariationNoteAction(note.id)">
                        👍 {{ note.likeCount || 0 }}
                      </el-button>
                    </div>
                    <p class="variation-note-content">{{ note.content }}</p>
                  </div>
                </div>
              </div>
            </template>
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
            <div v-if="commentsError && !commentsLoading" class="section-error" style="padding: 30px 0;">
              <el-empty :description="'评论加载失败：' + commentsError" :image-size="60">
                <el-button size="small" type="primary" @click="loadComments">重试</el-button>
              </el-empty>
            </div>
            <template v-else>
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
            </template>
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
            <div v-if="trialReceiptsError && !trialReceiptsLoading" class="section-error" style="padding: 30px 0;">
              <el-empty :description="'试做回执加载失败：' + trialReceiptsError" :image-size="60">
                <el-button size="small" type="primary" @click="loadTrialReceipts">重试</el-button>
              </el-empty>
            </div>
            <template v-else>
              <div v-if="!trialReceipts.length" class="empty-trial-receipts">
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

              <div class="trial-receipt-images" v-if="receipt.resultImageList && receipt.resultImageList.length">
                <div
                  v-for="(img, imgIdx) in receipt.resultImageList"
                  :key="imgIdx"
                  class="trial-receipt-image-item"
                >
                  <img :src="resolveImageUrl(img)" @click="previewImage(resolveImageUrl(img))" />
                </div>
              </div>
              </div>
            </template>
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

        <el-form-item label="成品展示">
          <div>
            <el-upload
              :action="resultUploadUrl"
              :headers="uploadHeaders"
              list-type="picture-card"
              :limit="6"
              :on-success="handleResultImageSuccess"
              :before-upload="beforeResultImageUpload"
              :on-remove="handleResultImageRemove"
              accept="image/*"
              :data="{ imageType: 'result' }"
              :file-list="trialReceiptForm.resultImages.map((url, idx) => ({ url, name: `image-${idx}`, uid: idx }))"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div class="upload-tip">建议上传清晰的成品图，最多6张，突出烘焙细节</div>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showTrialReceiptDialog = false">取消</el-button>
        <el-button type="primary" :loading="submittingTrialReceipt" @click="submitTrialReceipt">
          提交
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showVariationNoteDialog"
      title="分享做法变体经验"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-form :model="variationNoteForm" label-width="80px">
        <el-form-item label="变体主题">
          <el-select
            v-model="variationNoteForm.topic"
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入变体主题"
            style="width: 100%;"
          >
            <el-option
              v-for="topic in presetTopics"
              :key="topic"
              :label="topic"
              :value="topic"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="经验内容">
          <el-input
            v-model="variationNoteForm.content"
            type="textarea"
            :rows="4"
            placeholder="分享一下你在这种变体做法下的经验，比如换了一种面粉后口感有什么变化..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showVariationNoteDialog = false">取消</el-button>
        <el-button type="primary" :loading="submittingVariationNote" :disabled="!variationNoteForm.topic || !variationNoteForm.content.trim()" @click="submitVariationNote">
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
import { Star, StarFilled, Sunny, DocumentAdd, CircleCheckFilled, VideoPlay, Plus, DataAnalysis, HotWater, Timer, TrendCharts } from '@element-plus/icons-vue'
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
  addTrialReceipt,
  uploadImage,
  getRecipeBakeStats,
  getVariationNotes,
  getVariationTopics,
  addVariationNote as addVariationNoteApi,
  likeVariationNote as likeVariationNoteApi
} from '@/api'

const REQUEST_TIMEOUT = 8000

const resolveImageUrl = (url) => {
  if (!url) return url
  if (url.startsWith('/uploads/') && !url.startsWith('/api/uploads/')) {
    return '/api' + url
  }
  return url
}

const withTimeout = (promise, ms = REQUEST_TIMEOUT) => {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error('请求超时')), ms)
    Promise.resolve(promise)
      .then((res) => {
        clearTimeout(timer)
        resolve(res)
      })
      .catch((err) => {
        clearTimeout(timer)
        reject(err)
      })
  })
}

const route = useRoute()
const userStore = useUserStore()
const recipe = ref(null)
const loading = ref(false)
const recipeError = ref(null)
const currentImage = ref(0)
const isFavorited = ref(false)
const commentContent = ref('')
const comments = ref([])
const commentsLoading = ref(false)
const commentsError = ref(null)
const showLogin = ref(false)
const showAchievementUnlock = ref(false)
const newlyUnlocked = ref([])
const bakeStats = ref(null)
const bakeStatsLoading = ref(false)
const bakeStatsError = ref(null)

const showTrialReceiptDialog = ref(false)
const trialReceipts = ref([])
const trialReceiptsLoading = ref(false)
const trialReceiptsError = ref(null)
const resultUploadUrl = '/api/upload/image'
const uploadHeaders = {}
const trialReceiptForm = ref({
  success: 1,
  tasteRating: 0,
  tasteComment: '',
  tempAdjustment: '',
  moldDifference: '',
  notes: '',
  resultImages: []
})
const submittingTrialReceipt = ref(false)

const showVariationNoteDialog = ref(false)
const variationNotesGrouped = ref({})
const variationTopicKeys = ref([])
const variationNotesLoading = ref(false)
const variationNotesError = ref(null)
const submittingVariationNote = ref(false)
const variationNoteForm = ref({
  topic: '',
  content: ''
})
const presetTopics = ['换面粉', '减糖', '空气炸锅替代', '减油', '换模具', '无麸质', '素食版', '减盐']

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

const isTempDeviated = computed(() => {
  if (!recipe.value?.bakeTemp || !bakeStats.value) return false
  const { tempP25, tempP75 } = bakeStats.value
  if (tempP25 == null || tempP75 == null) return false
  return recipe.value.bakeTemp < tempP25 - 10 || recipe.value.bakeTemp > tempP75 + 10
})

const isTimeDeviated = computed(() => {
  if (!recipe.value?.bakeTime || !bakeStats.value) return false
  const { timeP25, timeP75 } = bakeStats.value
  if (timeP25 == null || timeP75 == null) return false
  return recipe.value.bakeTime < timeP25 - 5 || recipe.value.bakeTime > timeP75 + 5
})

const tempDeviationNote = computed(() => {
  if (!isTempDeviated.value || !bakeStats.value) return null
  const { tempP25, tempP75, categoryName } = bakeStats.value
  if (recipe.value.bakeTemp < tempP25 - 10) {
    return `温度偏低，${categoryName}类常见温度区间为 ${tempP25}~${tempP75}℃`
  } else {
    return `温度偏高，${categoryName}类常见温度区间为 ${tempP25}~${tempP75}℃`
  }
})

const timeDeviationNote = computed(() => {
  if (!isTimeDeviated.value || !bakeStats.value) return null
  const { timeP25, timeP75, categoryName } = bakeStats.value
  if (recipe.value.bakeTime < timeP25 - 5) {
    return `时长偏短，${categoryName}类常见时长区间为 ${timeP25}~${timeP75}分钟`
  } else {
    return `时长偏长，${categoryName}类常见时长区间为 ${timeP25}~${timeP75}分钟`
  }
})

const hasDeviation = computed(() => isTempDeviated.value || isTimeDeviated.value)

const {
  scaleFactor,
  scaledIngredients,
  increaseScale,
  decreaseScale,
  resetScale
} = useIngredientScaler(() => ingredients.value, {
  storageKey: route.params.id ? `recipe_${route.params.id}` : null
})

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
  recipeError.value = null
  try {
    recipe.value = await withTimeout(getRecipeDetail(route.params.id), 10000)
    loading.value = false

    if (userStore.isLogin) {
      withTimeout(checkFavorite(userStore.userInfo.id, route.params.id), 5000)
        .then((res) => { isFavorited.value = res })
        .catch((e) => { console.error('获取收藏状态失败', e) })
    }

    loadProgress()
    loadIngredientsProgress()
    loadBakeStats()
    loadComments()
    loadTrialReceipts()
    loadVariationNotes()
  } catch (e) {
    console.error(e)
    recipe.value = null
    recipeError.value = e.message || '加载失败，请稍后重试'
    loading.value = false
  }
}

const loadBakeStats = async () => {
  if (!recipe.value?.id) return
  bakeStatsLoading.value = true
  bakeStatsError.value = null
  try {
    bakeStats.value = await withTimeout(getRecipeBakeStats(recipe.value.id))
  } catch (e) {
    console.error('加载烘焙参数统计失败', e)
    bakeStats.value = null
    bakeStatsError.value = e.message || '加载失败'
  } finally {
    bakeStatsLoading.value = false
  }
}

const favoriteLoading = ref(false)

const toggleFavorite = async () => {
  if (!userStore.isLogin) {
    showLogin.value = true
    return
  }
  if (favoriteLoading.value) return
  favoriteLoading.value = true
  try {
    if (isFavorited.value) {
      const result = await removeFavorite(userStore.userInfo.id, route.params.id)
      if (result.removed) {
        isFavorited.value = false
        recipe.value.favoriteCount = Math.max(0, (recipe.value.favoriteCount || 1) - 1)
      }
      ElMessage.success('已取消收藏')
    } else {
      const result = await addFavorite({ userId: userStore.userInfo.id, recipeId: route.params.id })
      if (result.added) {
        isFavorited.value = true
        recipe.value.favoriteCount = (recipe.value.favoriteCount || 0) + 1
      }
      ElMessage.success('收藏成功')

      if (result.newlyUnlocked && result.newlyUnlocked.length > 0) {
        newlyUnlocked.value = result.newlyUnlocked
        showAchievementUnlock.value = true
      }
    }
  } catch (e) {
    console.error(e)
  } finally {
    favoriteLoading.value = false
  }
}

const loadComments = async () => {
  commentsLoading.value = true
  commentsError.value = null
  try {
    const res = await withTimeout(getComments({ recipeId: route.params.id, pageNum: 1, pageSize: 20 }))
    comments.value = res.records || []
  } catch (e) {
    console.error(e)
    comments.value = []
    commentsError.value = e.message || '加载失败'
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
  trialReceiptsError.value = null
  try {
    const res = await withTimeout(getTrialReceipts({ recipeId: route.params.id, pageNum: 1, pageSize: 20 }))
    trialReceipts.value = res.records || []
  } catch (e) {
    console.error(e)
    trialReceipts.value = []
    trialReceiptsError.value = e.message || '加载失败'
  } finally {
    trialReceiptsLoading.value = false
  }
}

const beforeResultImageUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB!')
    return false
  }
  return true
}

const handleResultImageSuccess = (response, uploadFile) => {
  if (response.data && response.data.url) {
    trialReceiptForm.value.resultImages.push(response.data.url)
  }
  ElMessage.success('成品图上传成功')
}

const handleResultImageRemove = (uploadFile) => {
  const idx = trialReceiptForm.value.resultImages.indexOf(uploadFile.url)
  if (idx > -1) {
    trialReceiptForm.value.resultImages.splice(idx, 1)
  }
}

const previewImage = (url) => {
  window.open(url, '_blank')
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
    notes: '',
    resultImages: []
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
      notes: form.notes.trim() || null,
      resultImages: form.resultImages
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

const loadVariationNotes = async () => {
  variationNotesLoading.value = true
  variationNotesError.value = null
  try {
    const data = await withTimeout(getVariationNotes(route.params.id))
    variationNotesGrouped.value = data || {}
    variationTopicKeys.value = Object.keys(data || {})
  } catch (e) {
    console.error(e)
    variationNotesGrouped.value = {}
    variationTopicKeys.value = []
    variationNotesError.value = e.message || '加载失败'
  } finally {
    variationNotesLoading.value = false
  }
}

const openVariationNoteDialog = async () => {
  if (!userStore.isLogin) {
    showLogin.value = true
    return
  }
  try {
    const existingTopics = await withTimeout(getVariationTopics(route.params.id), 5000)
    const allTopics = [...new Set([...presetTopics, ...(existingTopics || [])])]
    presetTopics.splice(0, presetTopics.length, ...allTopics)
  } catch (e) {
    console.error(e)
  }
  variationNoteForm.value = { topic: '', content: '' }
  showVariationNoteDialog.value = true
}

const submitVariationNote = async () => {
  if (!userStore.isLogin) {
    showLogin.value = true
    return
  }
  if (!variationNoteForm.value.topic || !variationNoteForm.value.content.trim()) return
  submittingVariationNote.value = true
  try {
    await addVariationNoteApi({
      recipeId: route.params.id,
      userId: userStore.userInfo.id,
      topic: variationNoteForm.value.topic,
      content: variationNoteForm.value.content.trim()
    })
    showVariationNoteDialog.value = false
    ElMessage.success('变体经验提交成功')
    loadVariationNotes()
  } catch (e) {
    console.error(e)
    ElMessage.error('提交失败，请重试')
  } finally {
    submittingVariationNote.value = false
  }
}

const likeVariationNoteAction = async (id) => {
  try {
    await likeVariationNoteApi(id)
    for (const topic of variationTopicKeys.value) {
      const note = variationNotesGrouped.value[topic]?.find(n => n.id === id)
      if (note) {
        note.likeCount = (note.likeCount || 0) + 1
        break
      }
    }
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

.bake-stats-reference {
  margin-top: 20px;
  padding: 20px;
  background: linear-gradient(135deg, #fdfbf7 0%, #f8f4eb 100%);
  border-radius: 12px;
  border: 1px solid #e8e0d0;
}

.stats-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  font-weight: 600;
  color: #5a4a3a;
}

.stats-title {
  font-size: 16px;
}

.stats-sample {
  margin-left: 8px;
  font-size: 13px;
  color: #999;
  font-weight: normal;
}

.deviation-warnings {
  margin-bottom: 16px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.stats-item {
  background: #fff;
  padding: 14px;
  border-radius: 8px;
  border: 1px solid #f0ebe3;
  text-align: center;
}

.stats-item-label {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
}

.stats-item-value {
  font-size: 20px;
  font-weight: 700;
  color: #333;
  margin-bottom: 4px;
}

.stats-item-value .value-normal {
  color: #67c23a;
}

.stats-item-value .value-deviated {
  color: #e6a23c;
  animation: deviate-pulse 2s ease-in-out infinite;
}

@keyframes deviate-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.stats-item-range {
  font-size: 12px;
  color: #999;
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

  .bake-stats-reference {
    padding: 16px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .stats-header {
    flex-wrap: wrap;
  }
}

.trial-receipt-images {
  margin-top: 16px;
  padding-left: 48px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.trial-receipt-image-item {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s;
  border: 1px solid #f0ebe3;
}

.trial-receipt-image-item:hover {
  transform: scale(1.05);
}

.trial-receipt-image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}

.variation-desc {
  font-size: 14px;
  color: #999;
  line-height: 1.8;
  margin-bottom: 16px;
  padding: 16px 20px;
  background: #faf7f2;
  border-radius: 8px;
  border: 1px dashed #e8e0d0;
}

.variation-topic-group {
  margin-bottom: 24px;
}

.variation-topic-group:last-child {
  margin-bottom: 0;
}

.variation-topic-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.variation-topic-tag {
  background: linear-gradient(135deg, #ff9a56 0%, #ff6b6b 100%);
  border-color: transparent;
  font-size: 14px;
  font-weight: 600;
  padding: 0 16px;
  height: 32px;
  line-height: 32px;
}

.variation-topic-count {
  font-size: 13px;
  color: #999;
}

.variation-notes-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-left: 8px;
  border-left: 3px solid #f0ebe3;
  margin-left: 8px;
}

.variation-note-item {
  padding: 14px 16px;
  background: #faf7f2;
  border-radius: 8px;
  border: 1px solid #f0ebe3;
  transition: border-color 0.2s ease;
}

.variation-note-item:hover {
  border-color: #ffe0cc;
}

.variation-note-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.variation-note-user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.variation-note-user {
  font-size: 13px;
  font-weight: 500;
  color: #333;
}

.variation-note-time {
  font-size: 11px;
  color: #999;
}

.variation-note-content {
  font-size: 14px;
  color: #555;
  line-height: 1.8;
  margin-left: 42px;
  word-break: break-word;
}

@media (max-width: 768px) {
  .trial-receipt-images {
    padding-left: 0;
    margin-top: 12px;
  }

  .trial-receipt-image-item {
    width: 100px;
    height: 100px;
  }

  .variation-note-content {
    margin-left: 0;
    margin-top: 8px;
  }

  .variation-notes-list {
    margin-left: 0;
    padding-left: 12px;
  }

  .variation-topic-header {
    flex-wrap: wrap;
  }
}
</style>
