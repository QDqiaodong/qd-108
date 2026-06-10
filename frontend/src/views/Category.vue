<template>
  <div class="category-page">
    <div class="container">
      <div class="category-header">
        <h1 class="page-title">{{ currentCategory?.name || '全部配方' }}</h1>
        <p class="page-desc">发现更多美味的烘焙配方</p>
      </div>

      <div class="ribbon-wrap">
        <CategoryRibbon
          :categories="categories"
          :active-category-id="currentCategoryId || null"
          @change="handleRibbonChange"
        />
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
import { useRoute, useRouter } from 'vue-router'
import { getCategories, getRecipes } from '@/api'
import RecipeCard from '@/components/RecipeCard.vue'
import CategoryRibbon from '@/components/CategoryRibbon.vue'

const route = useRoute()
const router = useRouter()
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

const handleRibbonChange = (id) => {
  if (id) {
    router.push(`/category/${id}`)
  } else {
    router.push('/search')
  }
}

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
  margin-bottom: 24px;
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

.ribbon-wrap {
  background: #fff;
  border-radius: 16px;
  padding: 20px 24px 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  margin-bottom: 30px;
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

@media (max-width: 1024px) {
  .recipe-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .recipe-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 14px;
  }
  .page-title {
    font-size: 24px;
  }
}
</style>
