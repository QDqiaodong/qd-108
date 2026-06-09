<template>
  <header class="header">
    <div class="container header-inner">
      <div class="logo" @click="$router.push('/')">
        <span class="logo-icon">🍰</span>
        <span class="logo-text">居家手工美食烘焙园地</span>
      </div>

      <nav class="nav">
        <router-link to="/" class="nav-link">首页</router-link>
        <div class="nav-categories">
          <span class="nav-link">分类</span>
          <div class="dropdown">
            <router-link
              v-for="cat in categories"
              :key="cat.id"
              :to="`/category/${cat.id}`"
              class="dropdown-item"
            >
              {{ cat.name }}
            </router-link>
          </div>
        </div>
        <router-link to="/publish" class="nav-link publish-btn">
          <el-icon><Plus /></el-icon>
          发布配方
        </router-link>
      </nav>

      <div class="header-right">
        <div class="search-box">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索配方..."
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="user-area">
          <template v-if="userStore.isLogin">
            <el-dropdown @command="handleCommand">
              <div class="user-info">
                <el-avatar :size="32" :src="userStore.userInfo?.avatar">
                  {{ userStore.userInfo?.nickname?.charAt(0) }}
                </el-avatar>
                <span class="username">{{ userStore.userInfo?.nickname }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button type="primary" link @click="showLogin = true">登录</el-button>
          </template>
        </div>
      </div>
    </div>

    <LoginDialog v-model:visible="showLogin" @login-success="handleLoginSuccess" />
  </header>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getCategories } from '@/api'
import LoginDialog from './LoginDialog.vue'

const router = useRouter()
const userStore = useUserStore()
const searchKeyword = ref('')
const categories = ref([])
const showLogin = ref(false)

onMounted(() => {
  loadCategories()
})

const loadCategories = async () => {
  try {
    categories.value = await getCategories()
  } catch (e) {
    console.error(e)
  }
}

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/search', query: { keyword: searchKeyword.value } })
  }
}

const handleCommand = (command) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    userStore.logout()
    ElMessage.success('已退出登录')
  }
}

const handleLoginSuccess = () => {
  showLogin.value = false
}
</script>

<style scoped>
.header {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 70px;
  padding: 0 20px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.logo-icon {
  font-size: 28px;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  background: linear-gradient(135deg, #ff9a56 0%, #ff6b6b 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.nav {
  display: flex;
  align-items: center;
  gap: 30px;
}

.nav-link {
  color: #666;
  text-decoration: none;
  font-size: 15px;
  transition: color 0.3s;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-link:hover {
  color: #ff6b6b;
}

.publish-btn {
  background: linear-gradient(135deg, #ff9a56 0%, #ff6b6b 100%);
  color: #fff;
  padding: 8px 20px;
  border-radius: 20px;
}

.publish-btn:hover {
  color: #fff;
  opacity: 0.9;
}

.nav-categories {
  position: relative;
}

.nav-categories:hover .dropdown {
  display: block;
}

.dropdown {
  display: none;
  position: absolute;
  top: 100%;
  left: 0;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  padding: 8px 0;
  min-width: 120px;
}

.dropdown-item {
  display: block;
  padding: 10px 20px;
  color: #666;
  text-decoration: none;
  transition: all 0.3s;
}

.dropdown-item:hover {
  background: #f5f5f5;
  color: #ff6b6b;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.search-box {
  width: 240px;
}

.user-area {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  font-size: 14px;
  color: #333;
}
</style>
