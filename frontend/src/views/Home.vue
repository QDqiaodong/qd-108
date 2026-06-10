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
      <section class="section ribbon-section">
        <div class="section-header-inline">
          <h2 class="section-title">品类导航</h2>
          <span class="section-tip">快速切换烘焙品类</span>
        </div>
        <CategoryRibbon
          :categories="categories"
          :active-category-id="activeCategoryId"
          @change="handleCategoryChange"
        />
      </section>

      <section class="section">
        <div class="section-header">
          <h2 class="section-title">
            {{ activeCategoryId ? getCurrentCategoryName() + ' · ' : '' }}热门配方
          </h2>
          <router-link :to="getCategoryLink()" class="see-more">查看更多</router-link>
        </div>
        <div v-loading="hotLoading" class="recipe-grid">
          <RecipeCard v-for="recipe in hotRecipes" :key="recipe.id" :recipe="recipe" />
        </div>
        <div class="empty-tip" v-if="!hotLoading && hotRecipes.length === 0">
          <el-empty :description="activeCategoryId ? '该品类暂无热门配方' : '暂无热门配方'" />
        </div>
      </section>

      <section class="section">
        <div class="section-header">
          <h2 class="section-title">
            {{ activeCategoryId ? getCurrentCategoryName() + ' · ' : '' }}最新发布
          </h2>
        </div>
        <div v-loading="latestLoading" class="recipe-grid">
          <RecipeCard v-for="recipe in latestRecipes" :key="recipe.id" :recipe="recipe" />
        </div>
        <div class="empty-tip" v-if="!latestLoading && latestRecipes.length === 0">
          <el-empty :description="activeCategoryId ? '该品类暂无新发布' : '暂无配方'" />
        </div>
        <div class="load-more" v-if="latestRecipes.length > 0">
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
import CategoryRibbon from '@/components/CategoryRibbon.vue'

const router = useRouter()
const searchKeyword = ref('')
const categories = ref([])
const activeCategoryId = ref(null)
const hotRecipes = ref([])
const latestRecipes = ref([])
const hotLoading = ref(false)
const latestLoading = ref(false)
const pageNum = ref(1)
const pageSize = ref(8)
const total = ref(0)

const getCurrentCategoryName = () => {
  const cat = categories.value.find(c => c.id == activeCategoryId.value)
  return cat ? cat.name : ''
}

const getCategoryLink = () => {
  return activeCategoryId.value ? `/category/${activeCategoryId.value}` : '/search'
}

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

const handleCategoryChange = (id) => {
  activeCategoryId.value = id
  pageNum.value = 1
  loadHotRecipes()
  loadLatestRecipes()
}

const loadHotRecipes = async () => {
  hotLoading.value = true
  try {
    const res = await getRecipes({
      pageNum: 1,
      pageSize: 8,
      categoryId: activeCategoryId.value || null,
      sortBy: 'hot'
    })
    hotRecipes.value = res.records || []
  } catch (e) {
    try {
      hotRecipes.value = (await getHotRecipes(8)) || []
      if (activeCategoryId.value) {
        hotRecipes.value = hotRecipes.value.filter(r => r.categoryId == activeCategoryId.value)
      }
    } catch (e2) {
      console.error(e2)
    }
  } finally {
    hotLoading.value = false
  }
}

const loadLatestRecipes = async () => {
  latestLoading.value = true
  try {
    const res = await getRecipes({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      categoryId: activeCategoryId.value || null
    })
    latestRecipes.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    latestLoading.value = false
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

.ribbon-section {
  background: #fff;
  border-radius: 16px;
  padding: 20px 24px 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  margin-bottom: 40px;
}

.section-header-inline {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 16px;
}

.section-tip {
  font-size: 13px;
  color: #999;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  font-size: 22px;
  font-weight: 600;
  color: #222;
  margin: 0;
  position: relative;
  padding-left: 14px;
}

.section-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 22px;
  border-radius: 2px;
  background: linear-gradient(180deg, #ff9a56 0%, #ff6b6b 100%);
}

.see-more {
  color: #ff6b6b;
  text-decoration: none;
  font-size: 14px;
  transition: color 0.2s;
}

.see-more:hover {
  color: #ff5252;
  text-decoration: underline;
}

.recipe-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.empty-tip {
  padding: 40px 0;
}

.load-more {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

@media (max-width: 1024px) {
  .recipe-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 768px) {
  .banner {
    padding: 40px 0;
  }
  .banner h1 {
    font-size: 28px;
  }
  .banner p {
    font-size: 15px;
  }
  .recipe-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 14px;
  }
  .section-title {
    font-size: 18px;
  }
}
</style>
