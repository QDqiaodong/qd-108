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
          </div>
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
          <div v-loading="loading" class="recipe-grid">
            <RecipeCard v-for="recipe in recipeList" :key="recipe.id" :recipe="recipe" />
          </div>

          <div class="empty" v-if="!loading && recipeList.length === 0">
            <el-empty :description="activeTab === 'recipes' ? '还没有发布配方' : '还没有收藏配方'" />
          </div>

          <div class="pagination" v-if="total > 0">
            <el-pagination
              v-model:current-page="pageNum"
              v-model:page-size="pageSize"
              :total="total"
              layout="prev, pager, next, total"
              @current-change="loadData"
            />
          </div>
        </div>
      </div>
    </div>

    <LoginDialog v-model:visible="showLogin" @login-success="handleLoginSuccess" />
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { getUserRecipes, getFavoriteRecipes } from '@/api'
import RecipeCard from '@/components/RecipeCard.vue'
import LoginDialog from '@/components/LoginDialog.vue'

const userStore = useUserStore()
const showLogin = ref(false)
const activeTab = ref('recipes')
const recipeList = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)

onMounted(() => {
  if (userStore.isLogin) {
    loadData()
  }
})

watch(activeTab, () => {
  pageNum.value = 1
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

const handleLoginSuccess = () => {
  showLogin.value = false
  loadData()
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
</style>
