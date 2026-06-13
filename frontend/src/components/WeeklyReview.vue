<template>
  <div class="weekly-review">
    <div class="review-header">
      <div class="header-left">
        <h3 class="review-title">
          <span class="title-icon">📊</span>
          本周烘焙复盘
        </h3>
        <p class="review-date">{{ weekStart }} ~ {{ weekEnd }}</p>
      </div>
      <div class="header-right">
        <div class="streak-badge" v-if="reviewData?.streakDays > 0">
          <span class="flame">🔥</span>
          <span class="streak-num">{{ reviewData.streakDays }}</span>
          <span class="streak-text">天连续打卡</span>
        </div>
      </div>
    </div>

    <div class="stats-grid">
      <div class="stat-card recipe-card">
        <div class="stat-icon">📝</div>
        <div class="stat-content">
          <div class="stat-value">{{ reviewData?.recipeCount || 0 }}</div>
          <div class="stat-label">发布配方</div>
        </div>
        <div class="stat-trend" :class="recipeTrend > 0 ? 'up' : recipeTrend < 0 ? 'down' : ''">
          <span v-if="recipeTrend !== 0">{{ recipeTrend > 0 ? '+' : '' }}{{ recipeTrend }}</span>
          <span v-else>--</span>
        </div>
      </div>

      <div class="stat-card trial-card">
        <div class="stat-icon">👩‍🍳</div>
        <div class="stat-content">
          <div class="stat-value">{{ reviewData?.trialCount || 0 }}</div>
          <div class="stat-label">试做次数</div>
        </div>
      </div>

      <div class="stat-card comment-card">
        <div class="stat-icon">💬</div>
        <div class="stat-content">
          <div class="stat-value">{{ reviewData?.commentCount || 0 }}</div>
          <div class="stat-label">评论互动</div>
        </div>
      </div>

      <div class="stat-card checkin-card">
        <div class="stat-icon">✅</div>
        <div class="stat-content">
          <div class="stat-value">{{ reviewData?.checkInCount || 0 }} / 7</div>
          <div class="stat-label">本周打卡</div>
        </div>
      </div>
    </div>

    <div class="activity-section">
      <h4 class="section-title">
        <span class="section-icon">📈</span>
        每日活跃度
      </h4>
      <div class="activity-chart">
        <div
          v-for="day in reviewData?.dailyActivity || []"
          :key="day.date"
          class="activity-bar-wrapper"
        >
          <div class="bar-container">
            <div
              class="activity-bar recipe-bar"
              :style="{ height: getBarHeight(day.recipeCount, maxRecipeCount) }"
              :title="`发布 ${day.recipeCount} 个配方`"
            ></div>
            <div
              class="activity-bar trial-bar"
              :style="{ height: getBarHeight(day.trialCount, maxTrialCount) }"
              :title="`试做 ${day.trialCount} 次`"
            ></div>
          </div>
          <div class="bar-label" :class="{ 'today': day.isToday, 'future': day.isFuture }">
            {{ day.dayLabel }}
          </div>
          <div class="checkin-dot" :class="{ 'checked': day.hasCheckIn }">
            {{ day.hasCheckIn ? '✓' : '' }}
          </div>
        </div>
      </div>
      <div class="chart-legend">
        <span class="legend-item">
          <span class="legend-dot recipe-dot"></span>
          发布配方
        </span>
        <span class="legend-item">
          <span class="legend-dot trial-dot"></span>
          试做次数
        </span>
        <span class="legend-item">
          <span class="legend-dot checkin-dot"></span>
          已打卡
        </span>
      </div>
    </div>

    <div class="bottom-sections">
      <div class="category-section">
        <h4 class="section-title">
          <span class="section-icon">🏷️</span>
          分类活跃度
        </h4>
        <div class="category-list" v-if="reviewData?.categoryStats?.length > 0">
          <div
            v-for="(cat, index) in reviewData.categoryStats"
            :key="cat.categoryId"
            class="category-item"
          >
            <div class="category-rank" :class="'rank-' + (index + 1)">
              {{ index + 1 }}
            </div>
            <div class="category-info">
              <div class="category-name">{{ cat.categoryName }}</div>
              <div class="category-bar">
                <div
                  class="category-fill"
                  :style="{ width: getCategoryPercent(cat.count) + '%' }"
                ></div>
              </div>
            </div>
            <div class="category-count">{{ cat.count }} 篇</div>
          </div>
        </div>
        <div class="empty-category" v-else>
          <el-empty description="本周暂无发布" :image-size="60" />
        </div>
      </div>

      <div class="top-recipes-section">
        <h4 class="section-title">
          <span class="section-icon">🔥</span>
          本周热门配方
        </h4>
        <div class="recipe-list" v-if="reviewData?.topRecipes?.length > 0">
          <div
            v-for="recipe in reviewData.topRecipes"
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
                <span class="meta-tag">{{ recipe.categoryName }}</span>
                <span class="meta-item">
                  <span class="meta-icon">👁️</span>
                  {{ recipe.viewCount || 0 }}
                </span>
                <span class="meta-item">
                  <span class="meta-icon">💬</span>
                  {{ recipe.commentCount || 0 }}
                </span>
              </div>
            </div>
          </div>
        </div>
        <div class="empty-recipes" v-else>
          <el-empty description="本周暂无发布" :image-size="60" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getWeeklyReview } from '@/api'
import { ElMessage } from 'element-plus'

const resolveImageUrl = (url) => {
  if (!url) return url
  if (url.startsWith('/uploads/') && !url.startsWith('/api/uploads/')) {
    return '/api' + url
  }
  return url
}

const props = defineProps({
  userId: {
    type: [Number, String],
    required: true
  }
})

const router = useRouter()
const reviewData = ref(null)
const loading = ref(false)

const weekStart = computed(() => {
  return reviewData.value?.weekStart || ''
})

const weekEnd = computed(() => {
  return reviewData.value?.weekEnd || ''
})

const recipeTrend = computed(() => {
  return 0
})

const maxRecipeCount = computed(() => {
  if (!reviewData.value?.dailyActivity) return 1
  const max = Math.max(...reviewData.value.dailyActivity.map(d => d.recipeCount || 0))
  return Math.max(max, 1)
})

const maxTrialCount = computed(() => {
  if (!reviewData.value?.dailyActivity) return 1
  const max = Math.max(...reviewData.value.dailyActivity.map(d => d.trialCount || 0))
  return Math.max(max, 1)
})

const maxCategoryCount = computed(() => {
  if (!reviewData.value?.categoryStats?.length) return 1
  return Math.max(...reviewData.value.categoryStats.map(c => c.count || 0))
})

const getBarHeight = (count, max) => {
  if (max === 0) return '0%'
  const percent = (count / max) * 100
  return Math.min(percent, 100) + '%'
}

const getCategoryPercent = (count) => {
  if (maxCategoryCount.value === 0) return 0
  return (count / maxCategoryCount.value) * 100
}

const loadReviewData = async () => {
  if (!props.userId) return
  loading.value = true
  try {
    const res = await getWeeklyReview(props.userId)
    reviewData.value = res
  } catch (e) {
    console.error(e)
    ElMessage.error('加载每周复盘数据失败')
  } finally {
    loading.value = false
  }
}

const goToRecipe = (id) => {
  router.push(`/recipe/${id}`)
}

const refresh = () => {
  loadReviewData()
}

defineExpose({
  refresh
})

watch(() => props.userId, () => {
  loadReviewData()
})

onMounted(() => {
  loadReviewData()
})
</script>

<style scoped>
.weekly-review {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.header-left {
  flex: 1;
}

.review-title {
  font-size: 20px;
  font-weight: 700;
  color: #333;
  margin: 0 0 6px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-icon {
  font-size: 22px;
}

.review-date {
  font-size: 13px;
  color: #999;
  margin: 0;
}

.header-right {
  flex-shrink: 0;
}

.streak-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: linear-gradient(135deg, #fff5f5 0%, #ffe8e8 100%);
  padding: 8px 16px;
  border-radius: 20px;
}

.flame {
  font-size: 18px;
}

.streak-num {
  font-size: 18px;
  font-weight: 700;
  color: #ff6b6b;
}

.streak-text {
  font-size: 13px;
  color: #ff6b6b;
  font-weight: 500;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 28px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
  border-radius: 12px;
  background: #fafafa;
  position: relative;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  font-size: 32px;
  flex-shrink: 0;
}

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #333;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}

.stat-trend {
  font-size: 12px;
  color: #999;
  position: absolute;
  top: 12px;
  right: 14px;
}

.stat-trend.up {
  color: #52c41a;
}

.stat-trend.down {
  color: #ff4d4f;
}

.recipe-card {
  background: linear-gradient(135deg, #f0f5ff 0%, #e6f0ff 100%);
}

.recipe-card .stat-value {
  color: #1890ff;
}

.trial-card {
  background: linear-gradient(135deg, #fff7e6 0%, #ffefd6 100%);
}

.trial-card .stat-value {
  color: #fa8c16;
}

.comment-card {
  background: linear-gradient(135deg, #f6ffed 0%, #e6f7d6 100%);
}

.comment-card .stat-value {
  color: #52c41a;
}

.checkin-card {
  background: linear-gradient(135deg, #fff0f6 0%, #ffe0ed 100%);
}

.checkin-card .stat-value {
  color: #eb2f96;
}

.activity-section {
  margin-bottom: 28px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 16px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-icon {
  font-size: 18px;
}

.activity-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-around;
  height: 160px;
  padding: 0 8px;
  margin-bottom: 12px;
}

.activity-bar-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  position: relative;
}

.bar-container {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 4px;
  height: 120px;
  width: 100%;
  margin-bottom: 8px;
}

.activity-bar {
  width: 16px;
  border-radius: 4px 4px 0 0;
  transition: height 0.3s ease;
  min-height: 2px;
}

.recipe-bar {
  background: linear-gradient(180deg, #69b1ff 0%, #1890ff 100%);
}

.trial-bar {
  background: linear-gradient(180deg, #ffc069 0%, #fa8c16 100%);
}

.bar-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.bar-label.today {
  color: #ff6b6b;
  font-weight: 600;
}

.bar-label.future {
  color: #ccc;
}

.checkin-dot {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  color: transparent;
  transition: all 0.3s ease;
}

.checkin-dot.checked {
  background: #52c41a;
  color: #fff;
  font-weight: 700;
}

.chart-legend {
  display: flex;
  justify-content: center;
  gap: 24px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #666;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
}

.recipe-dot {
  background: #1890ff;
}

.trial-dot {
  background: #fa8c16;
}

.legend-dot.checkin-dot {
  background: #52c41a;
  border-radius: 50%;
}

.bottom-sections {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.category-section,
.top-recipes-section {
  background: #fafafa;
  border-radius: 12px;
  padding: 20px;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.category-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.category-rank {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #999;
  flex-shrink: 0;
}

.category-rank.rank-1 {
  background: linear-gradient(135deg, #ffd666 0%, #faad14 100%);
  color: #fff;
}

.category-rank.rank-2 {
  background: linear-gradient(135deg, #d9d9d9 0%, #8c8c8c 100%);
  color: #fff;
}

.category-rank.rank-3 {
  background: linear-gradient(135deg, #ffbb96 0%, #d46b08 100%);
  color: #fff;
}

.category-info {
  flex: 1;
  min-width: 0;
}

.category-name {
  font-size: 13px;
  color: #333;
  font-weight: 500;
  margin-bottom: 4px;
}

.category-bar {
  height: 6px;
  background: #e8e8e8;
  border-radius: 3px;
  overflow: hidden;
}

.category-fill {
  height: 100%;
  background: linear-gradient(90deg, #ff9a56 0%, #ff6b6b 100%);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.category-count {
  font-size: 13px;
  color: #666;
  font-weight: 600;
  flex-shrink: 0;
}

.empty-category,
.empty-recipes {
  padding: 20px 0;
}

.recipe-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recipe-item {
  display: flex;
  gap: 12px;
  padding: 10px;
  background: #fff;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.recipe-item:hover {
  background: #fff5f5;
  transform: translateX(4px);
}

.recipe-cover {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
  background: #f5f5f5;
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
  font-size: 22px;
}

.recipe-info {
  flex: 1;
  min-width: 0;
}

.recipe-title {
  font-size: 13px;
  color: #333;
  font-weight: 500;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recipe-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.meta-tag {
  font-size: 11px;
  color: #ff6b6b;
  background: #fff0f0;
  padding: 2px 6px;
  border-radius: 4px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: #999;
}

.meta-icon {
  font-size: 12px;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .bottom-sections {
    grid-template-columns: 1fr;
  }

  .review-header {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
