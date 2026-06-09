<template>
  <div class="category-page">
    <div class="container">
      <div class="category-header">
        <h1 class="page-title">{{ currentCategory?.name || '全部配方' }}</h1>
        <p class="page-desc">发现更多美味的烘焙配方</p>
      </div>

      <div class="category-tabs">
        <div
          class="tab-item"
          :class="{ active: !currentCategoryId }"
          @click="$router.push('/search')"
        >
          全部
        </div>
        <div
          v-for="cat in categories"
          :key="cat.id"
          class="tab-item"
          :class="{ active: currentCategoryId == cat.id }"
          @click="$router.push(`/category/${cat.id}`)"
        >
          {{ cat.name }}
        </div>
      </div>

      <div v-loading="loading" class="recipe-grid">
        <RecipeCard v-for="recipe in recipes" :key="recipe.id" :recipe="recipe" />
      </div>

      <div class="empty" v-if="!loading && recipes.length === 0">
        <el-empty description="暂无配方" />
      </div>

      <div class="pagination">
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
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getCategories, getRecipes } from '@/api'
import RecipeCard from '@/components/RecipeCard.vue'

const route = useRoute()
const categories = ref([])
const recipes = ref([])
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(12)
const total = ref(0)

const currentCategoryId = computed(() => route.params.id)
const currentCategory = computed(() =>
  categories.value.find(c => c.id == currentCategoryId.value)
)

onMounted(() => {
  loadCategories()
  loadRecipes()
})

watch(() => route.params.id, () => {
  pageNum.value = 1
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
      categoryId: currentCategoryId.value || null
    })
    recipes.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.category-page {
  min-height: 60vh;
}

.category-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-title {
  font-size: 32px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
}

.page-desc {
  font-size: 16px;
  color: #999;
}

.category-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 30px;
  flex-wrap: wrap;
  justify-content: center;
}

.tab-item {
  padding: 8px 24px;
  background: #fff;
  border-radius: 20px;
  color: #666;
  cursor: pointer;
  transition: all 0.3s;
  font-size: 14px;
}

.tab-item:hover,
.tab-item.active {
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
