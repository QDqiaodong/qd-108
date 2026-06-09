<template>
  <div class="home">
    <div class="banner">
      <div class="container banner-content">
        <h1>发现美食的乐趣</h1>
        <p>分享烘焙配方，记录美味时刻</p>
        <div class="search-bar">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索你想做的美食..."
            size="large"
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button type="primary" @click="handleSearch">搜索</el-button>
            </template>
          </el-input>
        </div>
      </div>
    </div>

    <div class="container">
      <section class="section">
        <h2 class="section-title">热门分类</h2>
        <div class="category-list">
          <div
            v-for="cat in categories"
            :key="cat.id"
            class="category-item"
            @click="$router.push(`/category/${cat.id}`)"
          >
            <div class="category-icon">{{ getCategoryIcon(cat.name) }}</div>
            <span class="category-name">{{ cat.name }}</span>
          </div>
        </div>
      </section>

      <section class="section">
        <div class="section-header">
          <h2 class="section-title">热门配方</h2>
          <router-link to="/search" class="see-more">查看更多</router-link>
        </div>
        <div v-loading="loading" class="recipe-grid">
          <RecipeCard v-for="recipe in hotRecipes" :key="recipe.id" :recipe="recipe" />
        </div>
      </section>

      <section class="section">
        <div class="section-header">
          <h2 class="section-title">最新发布</h2>
        </div>
        <div v-loading="loading" class="recipe-grid">
          <RecipeCard v-for="recipe in latestRecipes" :key="recipe.id" :recipe="recipe" />
        </div>
        <div class="load-more">
          <el-pagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="loadLatestRecipes"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCategories, getHotRecipes, getRecipes } from '@/api'
import RecipeCard from '@/components/RecipeCard.vue'

const router = useRouter()
const searchKeyword = ref('')
const categories = ref([])
const hotRecipes = ref([])
const latestRecipes = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(8)
const total = ref(0)

const categoryIcons = {
  '面包': '🍞',
  '蛋糕': '🎂',
  '饼干': '🍪',
  '甜点': '🍮',
  '披萨': '🍕',
  '中式面点': '🥟'
}

const getCategoryIcon = (name) => categoryIcons[name] || '🍰'

onMounted(() => {
  loadCategories()
  loadHotRecipes()
  loadLatestRecipes()
})

const loadCategories = async () => {
  try {
    categories.value = await getCategories()
  } catch (e) {
    console.error(e)
  }
}

const loadHotRecipes = async () => {
  try {
    hotRecipes.value = await getHotRecipes(8)
  } catch (e) {
    console.error(e)
  }
}

const loadLatestRecipes = async () => {
  loading.value = true
  try {
    const res = await getRecipes({
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    latestRecipes.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/search', query: { keyword: searchKeyword.value } })
  }
}
</script>

<style scoped>
.banner {
  background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);
  padding: 60px 0;
  margin-bottom: 40px;
}

.banner-content {
  text-align: center;
}

.banner h1 {
  font-size: 36px;
  color: #333;
  margin-bottom: 12px;
}

.banner p {
  font-size: 18px;
  color: #666;
  margin-bottom: 30px;
}

.search-bar {
  max-width: 600px;
  margin: 0 auto;
}

.section {
  margin-bottom: 50px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.see-more {
  color: #ff6b6b;
  text-decoration: none;
  font-size: 14px;
}

.see-more:hover {
  text-decoration: underline;
}

.category-list {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 20px;
}

.category-item {
  background: #fff;
  border-radius: 12px;
  padding: 24px 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.category-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.category-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.category-name {
  font-size: 15px;
  color: #333;
  font-weight: 500;
}

.recipe-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.load-more {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}
</style>
