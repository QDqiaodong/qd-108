<template>
  <div class="profile">
    <div class="container">
      <div v-if="!userStore.isLogin" class="login-prompt">
        <el-empty description="请先登录">
          <el-button type="primary" @click="showLogin = true">去登录</el-button>
        </el-empty>
      </div>

      <div v-else>
        <div class="profile-header">
          <div class="user-card">
            <el-avatar :size="80" :src="userStore.userInfo?.avatar">
              {{ userStore.userInfo?.nickname?.charAt(0) }}
            </el-avatar>
            <div class="user-info">
              <h2 class="user-name">{{ userStore.userInfo?.nickname }}</h2>
              <p class="user-bio" v-if="userStore.userInfo?.bio">
                {{ userStore.userInfo?.bio }}
              </p>
              <p class="user-join-time">
                加入于 {{ userStore.userInfo?.createdAt?.split(' ')[0] }}
              </p>
            </div>
            <div class="checkin-section">
              <div class="streak-info">
                <span class="streak-icon">🔥</span>
                <span class="streak-days">{{ streakDays }}天</span>
                <span class="streak-label">连续打卡</span>
              </div>
              <el-button
                type="primary"
                size="large"
                :disabled="hasCheckedInToday"
                :class="{ 'btn-checked': hasCheckedInToday }"
                @click="handleCheckIn"
              >
                {{ hasCheckedInToday ? '今日已打卡' : '立即打卡' }}
              </el-button>
            </div>
          </div>
        </div>

        <div class="badge-section">
          <BadgeWall :achievements="userAchievements" />
        </div>

        <div class="weekly-review-section">
          <WeeklyReview
            v-if="userStore.userInfo?.id"
            ref="weeklyReviewRef"
            :userId="userStore.userInfo.id"
          />
        </div>

        <div class="calendar-section">
          <CalendarView
            v-if="userStore.userInfo?.id"
            ref="calendarRef"
            :userId="userStore.userInfo.id"
          />
        </div>

        <div class="profile-tabs">
          <div
            class="tab-item"
            :class="{ active: activeTab === 'recipes' }"
            @click="activeTab = 'recipes'"
          >
            我的发布
          </div>
          <div
            class="tab-item"
            :class="{ active: activeTab === 'favorites' }"
            @click="activeTab = 'favorites'"
          >
            我的收藏
          </div>
        </div>

        <div class="tab-content">
          <div
            v-if="activeTab === 'favorites' && recipeList.length > 0"
            class="favorites-toolbar"
          >
            <div class="toolbar-left">
              <el-button
                v-if="!selectMode"
                type="primary"
                plain
                size="small"
                @click="enterSelectMode"
              >
                <el-icon style="margin-right: 4px;"><List /></el-icon>
                批量选择
              </el-button>
              <template v-else>
                <el-button size="small" @click="exitSelectMode">
                  取消
                </el-button>
                <el-checkbox
                  :model-value="isAllSelected"
                  :indeterminate="isIndeterminate"
                  @change="toggleSelectAll"
                >
                  全选
                </el-checkbox>
                <span class="selected-count">
                  已选 <b>{{ selectedRecipeIds.length }}</b> / {{ total }} 项
                </span>
              </template>
            </div>
            <div class="toolbar-right">
              <el-button
                v-if="selectMode"
                type="primary"
                :disabled="selectedRecipeIds.length === 0"
                @click="generatePrepareList"
              >
                <el-icon style="margin-right: 4px;"><ShoppingCart /></el-icon>
                生成备料清单
              </el-button>
            </div>
          </div>

          <div v-loading="loading" class="recipe-grid" :class="{ 'select-mode-grid': selectMode }">
            <div
              v-for="recipe in recipeList"
              :key="recipe.id"
              class="recipe-card-wrapper"
              :class="{ 'card-selected': isSelected(recipe.id) }"
            >
              <div
                v-if="selectMode && activeTab === 'favorites'"
                class="card-checkbox"
                @click.stop="toggleSelect(recipe.id)"
              >
                <el-checkbox :model-value="isSelected(recipe.id)" @click.stop />
              </div>
              <RecipeCard :recipe="recipe" />
            </div>
          </div>

          <div class="empty" v-if="!loading && recipeList.length === 0">
            <el-empty :description="activeTab === 'recipes' ? '还没有发布配方' : '还没有收藏配方'" />
          </div>

          <div class="pagination" v-if="total > 0 && !selectMode">
            <el-pagination
              v-model:current-page="pageNum"
              v-model:page-size="pageSize"
              :total="total"
              layout="prev, pager, next, total"
              @current-change="loadData"
            />
          </div>

          <div v-if="selectMode && activeTab === 'favorites' && total > pageSize" class="select-mode-hint">
            <el-alert
              title="提示：批量选择模式下暂不支持翻页，请在当前页选择"
              type="info"
              :closable="false"
              show-icon
            />
          </div>
        </div>
      </div>
    </div>

    <LoginDialog v-model:visible="showLogin" @login-success="handleLoginSuccess" />
    <AchievementUnlock v-model:visible="showAchievementUnlock" :achievements="newlyUnlocked" />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getUserRecipes, getFavoriteRecipes, getUserAchievements, checkIn, getCheckInStatus } from '@/api'
import RecipeCard from '@/components/RecipeCard.vue'
import LoginDialog from '@/components/LoginDialog.vue'
import BadgeWall from '@/components/BadgeWall.vue'
import AchievementUnlock from '@/components/AchievementUnlock.vue'
import CalendarView from '@/components/CalendarView.vue'
import WeeklyReview from '@/components/WeeklyReview.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { List, ShoppingCart } from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const showLogin = ref(false)
const activeTab = ref('recipes')
const recipeList = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)
const userAchievements = ref([])
const streakDays = ref(0)
const hasCheckedInToday = ref(false)
const showAchievementUnlock = ref(false)
const newlyUnlocked = ref([])
const calendarRef = ref(null)
const weeklyReviewRef = ref(null)

const selectMode = ref(false)
const selectedRecipeIds = ref([])
const allFavorites = ref([])

const isAllSelected = computed(() => {
  if (activeTab.value !== 'favorites') return false
  const pageIds = recipeList.value.map(r => r.id)
  return pageIds.length > 0 && pageIds.every(id => selectedRecipeIds.value.includes(id))
})

const isIndeterminate = computed(() => {
  if (activeTab.value !== 'favorites') return false
  const pageIds = recipeList.value.map(r => r.id)
  const selectedInPage = pageIds.filter(id => selectedRecipeIds.value.includes(id)).length
  return selectedInPage > 0 && selectedInPage < pageIds.length
})

const isSelected = (id) => selectedRecipeIds.value.includes(id)

const enterSelectMode = async () => {
  try {
    const res = await getFavoriteRecipes(userStore.userInfo.id, {
      pageNum: 1,
      pageSize: 1000
    })
    allFavorites.value = res.records || []
    recipeList.value = allFavorites.value
    total.value = res.total || 0
    pageSize.value = allFavorites.value.length || 12
    selectMode.value = true
    selectedRecipeIds.value = []
  } catch (e) {
    console.error(e)
    ElMessage.error('加载收藏列表失败')
  }
}

const exitSelectMode = () => {
  selectMode.value = false
  selectedRecipeIds.value = []
  pageNum.value = 1
  pageSize.value = 12
  loadData()
}

const toggleSelect = (id) => {
  const idx = selectedRecipeIds.value.indexOf(id)
  if (idx > -1) {
    selectedRecipeIds.value.splice(idx, 1)
  } else {
    selectedRecipeIds.value.push(id)
  }
}

const toggleSelectAll = (val) => {
  const pageIds = recipeList.value.map(r => r.id)
  if (val) {
    for (const id of pageIds) {
      if (!selectedRecipeIds.value.includes(id)) {
        selectedRecipeIds.value.push(id)
      }
    }
  } else {
    selectedRecipeIds.value = selectedRecipeIds.value.filter(id => !pageIds.includes(id))
  }
}

const generatePrepareList = async () => {
  if (selectedRecipeIds.value.length === 0) {
    ElMessage.warning('请先选择配方')
    return
  }
  try {
    await ElMessageBox.confirm(
      `已选择 ${selectedRecipeIds.value.length} 个配方，是否生成备料清单？`,
      '生成备料清单',
      {
        confirmButtonText: '确定生成',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    router.push({
      path: '/prepare-list',
      query: { ids: selectedRecipeIds.value.join(',') }
    })
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
    }
  }
}

watch(activeTab, () => {
  pageNum.value = 1
  selectMode.value = false
  selectedRecipeIds.value = []
  loadData()
})

const loadData = async () => {
  if (!userStore.isLogin) return
  loading.value = true
  try {
    let res
    if (activeTab.value === 'recipes') {
      res = await getUserRecipes(userStore.userInfo.id, {
        pageNum: pageNum.value,
        pageSize: pageSize.value
      })
    } else {
      res = await getFavoriteRecipes(userStore.userInfo.id, {
        pageNum: pageNum.value,
        pageSize: pageSize.value
      })
    }
    recipeList.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const loadAchievements = async () => {
  try {
    const res = await getUserAchievements(userStore.userInfo.id)
    userAchievements.value = res || []
  } catch (e) {
    console.error(e)
  }
}

const loadCheckInStatus = async () => {
  try {
    const res = await getCheckInStatus(userStore.userInfo.id)
    hasCheckedInToday.value = res.hasCheckedInToday || false
    streakDays.value = res.streakDays || 0
  } catch (e) {
    console.error(e)
  }
}

const handleCheckIn = async () => {
  if (hasCheckedInToday.value) return
  try {
    const res = await checkIn(userStore.userInfo.id)
    hasCheckedInToday.value = true
    streakDays.value = res.streakDays || streakDays.value
    ElMessage.success('打卡成功！')

    if (calendarRef.value) {
      calendarRef.value.refreshCalendar()
    }

    if (weeklyReviewRef.value) {
      weeklyReviewRef.value.refresh()
    }

    if (res.newlyUnlocked && res.newlyUnlocked.length > 0) {
      newlyUnlocked.value = res.newlyUnlocked
      showAchievementUnlock.value = true
      loadAchievements()
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('打卡失败，请重试')
  }
}

const handleLoginSuccess = () => {
  showLogin.value = false
  loadData()
  loadAchievements()
  loadCheckInStatus()
}
</script>

<style scoped>
.profile {
  min-height: 60vh;
}

.login-prompt {
  padding: 80px 0;
}

.profile-header {
  background: #fff;
  border-radius: 12px;
  padding: 30px;
  margin-bottom: 20px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 24px;
}

.user-info {
  flex: 1;
}

.user-name {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.user-bio {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.user-join-time {
  font-size: 13px;
  color: #999;
}

.checkin-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding-left: 30px;
  border-left: 1px solid #f0f0f0;
}

.streak-info {
  display: flex;
  align-items: center;
  gap: 6px;
}

.streak-icon {
  font-size: 24px;
}

.streak-days {
  font-size: 24px;
  font-weight: 700;
  color: #ff6b6b;
}

.streak-label {
  font-size: 13px;
  color: #999;
}

.checkin-section .el-button {
  background: linear-gradient(135deg, #ff9a56 0%, #ff6b6b 100%);
  border: none;
  border-radius: 25px;
  min-width: 120px;
}

.checkin-section .el-button:hover {
  background: linear-gradient(135deg, #ff8a3d 0%, #ff5252 100%);
}

.checkin-section .el-button.btn-checked {
  background: #f0f0f0;
  color: #999;
}

.badge-section {
  margin-bottom: 20px;
}

.weekly-review-section {
  margin-bottom: 20px;
}

.calendar-section {
  margin-bottom: 20px;
}

.profile-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 20px;
  background: #fff;
  border-radius: 8px;
  padding: 4px;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 12px 24px;
  border-radius: 6px;
  cursor: pointer;
  color: #666;
  transition: all 0.3s;
  font-size: 15px;
}

.tab-item:hover,
.tab-item.active {
  background: linear-gradient(135deg, #ff9a56 0%, #ff6b6b 100%);
  color: #fff;
}

.tab-content {
  min-height: 400px;
}

.recipe-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.empty {
  padding: 60px 0;
}

.pagination {
  margin-top: 40px;
  display: flex;
  justify-content: center;
}

.favorites-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 8px;
  padding: 12px 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.selected-count {
  font-size: 14px;
  color: #666;
}

.selected-count b {
  color: #ff6b6b;
  font-size: 16px;
  margin: 0 2px;
}

.recipe-card-wrapper {
  position: relative;
  transition: all 0.25s ease;
  border-radius: 8px;
  overflow: hidden;
}

.recipe-card-wrapper.card-selected {
  box-shadow: 0 0 0 3px #ff6b6b, 0 4px 16px rgba(255, 107, 107, 0.25);
  transform: translateY(-2px);
}

.card-checkbox {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 50%;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  transition: all 0.2s ease;
}

.card-checkbox:hover {
  background: #fff;
  transform: scale(1.1);
}

.card-selected .card-checkbox {
  background: #ff6b6b;
}

.card-selected .card-checkbox :deep(.el-checkbox__inner) {
  background-color: #ff6b6b;
  border-color: #ff6b6b;
}

.select-mode-grid .recipe-card-wrapper {
  cursor: pointer;
}

.select-mode-hint {
  margin-top: 20px;
}

@media (max-width: 768px) {
  .favorites-toolbar {
    flex-wrap: wrap;
    gap: 12px;
  }

  .toolbar-left,
  .toolbar-right {
    width: 100%;
    justify-content: center;
  }
}
</style>
