<template>
  <div class="search-page">
    <div class="container">
      <div class="search-header">
        <h1 class="page-title">搜索配方</h1>
        <div class="search-bar">
          <el-input
            v-model="keyword"
            placeholder="输入关键词搜索配方..."
            size="large"
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button type="primary" @click="handleSearch">搜索</el-button>
            </template>
          </el-input>
        </div>
      </div>

      <div class="category-filter">
        <span class="filter-label">分类：</span>
        <div
          class="filter-item"
          :class="{ active: !selectedCategory }"
          @click="selectCategory(null)"
        >
          全部
        </div>
        <div
          v-for="cat in categories"
          :key="cat.id"
          class="filter-item"
          :class="{ active: selectedCategory == cat.id }"
          @click="selectCategory(cat.id)"
        >
          {{ cat.name }}
        </div>
      </div>

      <div v-loading="loading" class="recipe-grid">
        <RecipeCard v-for="recipe in recipes" :key="recipe.id" :recipe="recipe" />
      </div>

      <div class="empty" v-if="!loading && recipes.length === 0">
        <el-empty description="没有找到相关配方" />
      </div>

      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="loadRecipes"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCategories, getRecipes } from '@/api'
import RecipeCard from '@/components/RecipeCard.vue'

const route = useRoute()
const router = useRouter()
const keyword = ref(route.query.keyword || '')
const selectedCategory = ref(null)
const categories = ref([])
const recipes = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)

onMounted(() => {
  loadCategories()
  loadRecipes()
})

const loadCategories = async () => {
  try {
    categories.value = await getCategories()
  } catch (e) {
    console.error(e)
  }
}

const loadRecipes = async () => {
  loading.value = true
  try {
    const res = await getRecipes({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      categoryId: selectedCategory.value,
      keyword: keyword.value || null
    })
    recipes.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  router.push({ query: { keyword: keyword.value } })
  loadRecipes()
}

const selectCategory = (id) => {
  selectedCategory.value = id
  pageNum.value = 1
  loadRecipes()
}
</script>

<style scoped>
.search-page {
  min-height: 60vh;
}

.search-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
}

.search-bar {
  max-width: 600px;
  margin: 0 auto;
}

.category-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 30px;
  flex-wrap: wrap;
}

.filter-label {
  color: #666;
  font-size: 14px;
}

.filter-item {
  padding: 6px 16px;
  background: #fff;
  border-radius: 16px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 13px;
}

.filter-item:hover,
.filter-item.active {
  background: linear-gradient(135deg, #ff9a56 0%, #ff6b6b 100%);
  color: #fff;
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
