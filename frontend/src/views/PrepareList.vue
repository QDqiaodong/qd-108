<template>
  <div class="prepare-list" v-loading="loading">
    <div class="container" v-if="!loading">
      <div class="page-header">
        <div class="header-left">
          <el-button :icon="ArrowLeft" text @click="goBack">
            返回
          </el-button>
          <h1 class="page-title">
            <el-icon style="margin-right: 8px; color: #ff6b6b;"><ShoppingCart /></el-icon>
            备料清单
          </h1>
        </div>
        <div class="header-right">
          <div class="recipe-count-badge">
            <el-icon><Document /></el-icon>
            <span>{{ recipes.length }} 个配方</span>
          </div>
          <div class="progress-badge">
            <el-icon><CircleCheck /></el-icon>
            <span>已备齐 {{ checkedCount }} / {{ totalIngredientCount }}</span>
          </div>
          <el-button
            size="small"
            text
            type="danger"
            :disabled="checkedCount === 0"
            @click="handleResetChecked"
          >
            重置进度
          </el-button>
        </div>
      </div>

      <div class="recipes-strip" v-if="recipes.length">
        <div
          v-for="recipe in recipes"
          :key="recipe.id"
          class="recipe-chip"
          @click="$router.push(`/recipe/${recipe.id}`)"
        >
          <img v-if="recipe.coverImage" :src="resolveImageUrl(recipe.coverImage)" />
          <div v-else class="chip-placeholder"></div>
          <span class="chip-title">{{ recipe.title }}</span>
        </div>
      </div>

      <div class="scale-toolbar" v-if="categorySections.length">
        <IngredientScaler
          :scale-factor="scaleFactor"
          @increase="increaseScale"
          @decrease="decreaseScale"
          @reset="resetScale"
        />
      </div>

      <div class="summary-card" v-if="categorySections.length">
        <div class="summary-title">食材概览</div>
        <div class="summary-categories">
          <div
            v-for="section in categorySections"
            :key="section.name"
            class="summary-item"
            :style="{ '--cat-color': section.color }"
          >
            <div class="summary-icon">{{ section.icon }}</div>
            <div class="summary-info">
              <div class="summary-name">{{ section.name }}</div>
              <div class="summary-count">{{ section.items.length }} 项</div>
            </div>
          </div>
        </div>
      </div>

      <div class="empty" v-if="!loading && !categorySections.length">
        <el-empty description="没有找到食材数据，请返回重新选择配方">
          <el-button type="primary" @click="goBack">返回选择</el-button>
        </el-empty>
      </div>

      <div class="ingredient-sections" v-if="categorySections.length">
        <div
          v-for="section in categorySections"
          :key="section.name"
          class="ingredient-section"
          :id="`section-${section.name}`"
        >
          <div class="section-header" :style="{ borderColor: section.color }">
            <div class="section-title-wrap">
              <span class="section-icon" :style="{ background: section.color }">
                {{ section.icon }}
              </span>
              <h2 class="section-title" :style="{ color: section.color }">
                {{ section.name }}
              </h2>
              <el-tag size="small" type="info" effect="plain">
                {{ section.items.length }} 项
              </el-tag>
            </div>
            <div class="section-progress">
              <el-progress
                :percentage="getSectionProgress(section)"
                :stroke-width="6"
                :color="section.color"
                :show-text="false"
                style="width: 100px;"
              />
              <span class="progress-text">
                {{ getSectionCheckedCount(section) }}/{{ section.items.length }}
              </span>
            </div>
          </div>

          <div class="ingredient-list">
            <div
              v-for="(item, idx) in section.items"
              :key="item.key"
              class="ingredient-row"
              :class="{ 'row-checked': isChecked(item.key) }"
              @click="toggleCheck(item.key)"
            >
              <div class="row-checkbox">
                <el-checkbox :model-value="isChecked(item.key)" @click.stop="toggleCheck(item.key)" />
              </div>
              <div class="row-main">
                <div class="row-name">
                  {{ item.name }}
                  <el-tag
                    v-if="item.aliasCount > 0"
                    size="small"
                    type="warning"
                    effect="light"
                    class="alias-tag"
                  >
                    合并 {{ item.aliasCount + 1 }} 处
                  </el-tag>
                </div>
                <div class="row-sources">
                  <span
                    v-for="src in item.sources"
                    :key="src.recipeId"
                    class="source-chip"
                  >
                    {{ src.recipeTitle }}: {{ src.scaledAmount || src.originalAmount }}
                  </span>
                </div>
              </div>
              <div class="row-amount">
                <div class="amount-main" :class="{ 'amount-merged': item.isMerged }">
                  {{ item.displayAmount }}
                </div>
                <div v-if="scaleFactor !== 1 && item.originalDisplayAmount" class="amount-original">
                  原始：{{ item.originalDisplayAmount }}
                </div>
                <div v-if="item.isMerged && item.mergedNote" class="amount-note">
                  {{ item.mergedNote }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div
        v-if="categorySections.length && uncheckedCount === 0"
        class="completion-banner"
      >
        <el-icon style="font-size: 28px; color: #67c23a;"><CircleCheckFilled /></el-icon>
        <div class="completion-text">
          <div class="completion-title">太棒啦！所有食材已准备完毕~</div>
          <div class="completion-subtitle">可以开始你的烘焙之旅啦 🎂</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const resolveImageUrl = (url) => {
  if (!url) return url
  if (url.startsWith('/uploads/') && !url.startsWith('/api/uploads/')) {
    return '/api' + url
  }
  return url
}

import {
  ArrowLeft,
  ShoppingCart,
  Document,
  CircleCheck,
  CircleCheckFilled
} from '@element-plus/icons-vue'
import { getRecipesByIds, getIngredientAliasMap } from '@/api'
import IngredientScaler from '@/components/IngredientScaler.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const recipes = ref([])
const aliasMap = ref({})
const checkedKeys = ref(new Set())
const storageKey = 'prepare_list_checked'

const scaleFactor = ref(1)
const scaleStorageKey = computed(() => {
  const idsStr = route.query.ids || ''
  return `prepare_scale_${idsStr}`
})

const loadScaleFromStorage = () => {
  try {
    const stored = localStorage.getItem(scaleStorageKey.value)
    if (stored) {
      const val = parseFloat(stored)
      if (!isNaN(val) && val >= 0.25 && val <= 10) {
        scaleFactor.value = val
      }
    }
  } catch (e) {
    console.error('加载缩放因子失败', e)
  }
}

const saveScaleToStorage = () => {
  try {
    localStorage.setItem(scaleStorageKey.value, String(scaleFactor.value))
  } catch (e) {
    console.error('保存缩放因子失败', e)
  }
}

watch(scaleFactor, () => {
  saveScaleToStorage()
})

const increaseScale = (step = 0.5) => {
  scaleFactor.value = Math.min(10, scaleFactor.value + step)
}

const decreaseScale = (step = 0.5) => {
  scaleFactor.value = Math.max(0.25, scaleFactor.value - step)
}

const resetScale = () => {
  scaleFactor.value = 1
}

const scaleAmount = (amountStr, multiplier) => {
  if (multiplier === 1) return amountStr
  const match = String(amountStr).trim().match(AMOUNT_REGEX)
  if (!match) return amountStr

  const numPart = match[1]
  let numeric = null
  if (numPart.includes('/')) {
    const parts = numPart.split('/')
    if (parts.length === 2) {
      const n = parseFloat(parts[0])
      const d = parseFloat(parts[1])
      if (!isNaN(n) && !isNaN(d) && d !== 0) numeric = n / d
    }
  } else {
    const val = parseFloat(numPart)
    if (!isNaN(val)) numeric = val
  }
  if (numeric === null) return amountStr

  const unit = match[2] || ''
  const scaled = numeric * multiplier
  return `${formatAmount(scaled)}${unit ? ' ' + unit : ''}`
}

const AMOUNT_REGEX = /^([\d.\/]+)\s*(.*)$/

const CATEGORY_RULES = [
  {
    name: '粉类',
    icon: '🌾',
    color: '#e6a23c',
    keywords: ['面粉', '粉', '淀粉', '可可粉', '抹茶粉', '杏仁粉', '椰丝', '椰蓉']
  },
  {
    name: '乳制品',
    icon: '🥛',
    color: '#409eff',
    keywords: ['牛奶', '奶油', '奶酪', '芝士', '马斯卡彭', '黄油', '牛油', '炼乳', '酸奶']
  },
  {
    name: '糖类',
    icon: '🍬',
    color: '#f56c6c',
    keywords: ['糖', '蜂蜜', '枫糖浆', '焦糖']
  },
  {
    name: '油脂类',
    icon: '🫒',
    color: '#b88230',
    keywords: ['油', '黄油', '牛油', '玉米油', '橄榄油', '色拉油', '椰子油']
  },
  {
    name: '蛋类',
    icon: '🥚',
    color: '#f0c78a',
    keywords: ['鸡蛋', '蛋', '蛋清', '蛋黄', '蛋白', '全蛋']
  },
  {
    name: '坚果果干',
    icon: '🥜',
    color: '#a0522d',
    keywords: ['核桃', '腰果', '杏仁', '花生', '芝麻', '蔓越莓', '葡萄干', '蓝莓干', '巧克力']
  },
  {
    name: '添加剂',
    icon: '🧪',
    color: '#909399',
    keywords: ['酵母', '泡打粉', '小苏打', '发粉', '吉利丁', '鱼胶', '明胶', '香草精', '香草荚', '盐']
  },
  {
    name: '调味与其他',
    icon: '🍋',
    color: '#67c23a',
    keywords: []
  }
]

const loadAliasMap = async () => {
  try {
    const map = await getIngredientAliasMap()
    aliasMap.value = map || {}
  } catch (e) {
    console.error('加载别名映射失败', e)
    aliasMap.value = {}
  }
}

const loadRecipes = async () => {
  const idsStr = route.query.ids
  if (!idsStr) {
    ElMessage.warning('未选择配方')
    return
  }
  const ids = String(idsStr).split(',').map(s => parseInt(s.trim())).filter(n => !isNaN(n))
  if (!ids.length) {
    ElMessage.warning('配方ID无效')
    return
  }
  try {
    const list = await getRecipesByIds(ids)
    recipes.value = list || []
  } catch (e) {
    console.error('加载配方失败', e)
    ElMessage.error('加载配方失败')
  }
}

const normalizeName = (name) => {
  if (!name) return ''
  const trimmed = name.trim()
  return aliasMap.value[trimmed] || trimmed
}

const parseAmount = (amountStr) => {
  if (!amountStr) return { numeric: null, unit: '', original: amountStr }
  const match = String(amountStr).trim().match(AMOUNT_REGEX)
  if (!match) return { numeric: null, unit: '', original: amountStr }

  const numPart = match[1]
  let numeric = null
  if (numPart.includes('/')) {
    const parts = numPart.split('/')
    if (parts.length === 2) {
      const n = parseFloat(parts[0])
      const d = parseFloat(parts[1])
      if (!isNaN(n) && !isNaN(d) && d !== 0) numeric = n / d
    }
  } else {
    const val = parseFloat(numPart)
    if (!isNaN(val)) numeric = val
  }
  return { numeric, unit: (match[2] || '').trim(), original: amountStr }
}

const normalizeUnit = (unit) => {
  if (!unit) return ''
  const u = unit.toLowerCase().trim()
  const unitMap = {
    'g': 'g', '克': 'g', 'gm': 'g', 'gram': 'g', 'grams': 'g',
    'kg': 'kg', '千克': 'kg', '公斤': 'kg',
    'ml': 'ml', '毫升': 'ml', 'cc': 'ml',
    'l': 'l', '升': 'l',
    '个': '个', '只': '个', '颗': '个', '粒': '个',
    '小勺': '小勺', '茶匙': '小勺', 'tsp': '小勺',
    '大勺': '大勺', '汤匙': '大勺', 'tbsp': '大勺',
    '杯': '杯', 'cup': '杯', 'cups': '杯'
  }
  return unitMap[u] || unit
}

const formatAmount = (value) => {
  if (value === null || value === undefined || isNaN(value)) return ''
  if (value === Math.floor(value)) return String(value)
  const rounded = Math.round(value * 100) / 100
  if (rounded === Math.floor(rounded)) return String(Math.floor(rounded))
  return String(rounded)
}

const parseIngredients = (recipe) => {
  if (!recipe || !recipe.ingredients) return []
  try {
    const arr = JSON.parse(recipe.ingredients)
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}

const categorize = (name) => {
  for (const rule of CATEGORY_RULES) {
    if (rule.name === '调味与其他') continue
    for (const kw of rule.keywords) {
      if (name.includes(kw)) {
        return rule.name
      }
    }
  }
  return '调味与其他'
}

const loadCheckedFromStorage = () => {
  const idsStr = route.query.ids || ''
  try {
    const raw = localStorage.getItem(`${storageKey}_${idsStr}`)
    if (raw) {
      const arr = JSON.parse(raw)
      checkedKeys.value = new Set(arr)
    }
  } catch (e) {
    console.error('加载勾选进度失败', e)
  }
}

const saveCheckedToStorage = () => {
  const idsStr = route.query.ids || ''
  try {
    localStorage.setItem(
      `${storageKey}_${idsStr}`,
      JSON.stringify(Array.from(checkedKeys.value))
    )
  } catch (e) {
    console.error('保存勾选进度失败', e)
  }
}

const mergedIngredients = computed(() => {
  const merged = {}

  for (const recipe of recipes.value) {
    const ingredients = parseIngredients(recipe)
    for (const ing of ingredients) {
      const rawName = ing.name || ''
      const canonicalName = ing.canonicalName || normalizeName(rawName)
      const displayName = rawName
      const parsed = parseAmount(ing.amount)
      const normalizedUnit = normalizeUnit(parsed.unit)

      const key = `${canonicalName}__${normalizedUnit || 'no_unit'}`

      if (!merged[key]) {
        merged[key] = {
          key,
          name: canonicalName,
          displayName: displayName,
          unit: normalizedUnit,
          originalUnit: parsed.unit || '',
          totalNumeric: null,
          isMerged: false,
          aliasCount: 0,
          mergedNote: '',
          sources: [],
          sourceNames: new Set(),
          displayNames: new Set()
        }
      }

      if (rawName !== canonicalName && !merged[key].sourceNames.has(rawName)) {
        merged[key].aliasCount += 1
      }
      merged[key].sourceNames.add(rawName)
      merged[key].displayNames.add(displayName)

      merged[key].sources.push({
        recipeId: recipe.id,
        recipeTitle: recipe.title,
        originalName: displayName,
        originalAmount: ing.amount || '',
        parsed
      })

      if (parsed.numeric !== null && merged[key].unit === normalizedUnit) {
        if (merged[key].totalNumeric === null) {
          merged[key].totalNumeric = parsed.numeric
        } else {
          merged[key].totalNumeric += parsed.numeric
          merged[key].isMerged = true
        }
      }
    }
  }

  for (const key of Object.keys(merged)) {
    const item = merged[key]
    if (item.displayNames.size === 1) {
      item.name = [...item.displayNames][0]
    }

    const scale = scaleFactor.value
    const scaledTotalNumeric = item.totalNumeric !== null ? item.totalNumeric * scale : null

    if (scaledTotalNumeric !== null && item.unit) {
      item.displayAmount = `${formatAmount(scaledTotalNumeric)}${item.unit === '个' || item.unit === '杯' || item.unit === '小勺' || item.unit === '大勺' ? item.unit : ' ' + item.unit}`
      item.originalDisplayAmount = `${formatAmount(item.totalNumeric)}${item.unit === '个' || item.unit === '杯' || item.unit === '小勺' || item.unit === '大勺' ? item.unit : ' ' + item.unit}`
      if (item.isMerged && item.sources.length > 1) {
        item.mergedNote = item.sources.map(s => scaleAmount(s.originalAmount, scale)).join(' + ')
      }
      item.sources.forEach(s => {
        s.scaledAmount = scaleAmount(s.originalAmount, scale)
      })
    } else if (item.sources.length === 1) {
      item.displayAmount = scaleAmount(item.sources[0].originalAmount, scale)
      item.originalDisplayAmount = item.sources[0].originalAmount
      item.sources[0].scaledAmount = item.displayAmount
    } else {
      const units = [...new Set(item.sources.map(s => s.parsed.unit || '无单位'))]
      item.displayAmount = item.sources.map(s => scaleAmount(s.originalAmount, scale)).join(' + ')
      item.originalDisplayAmount = item.sources.map(s => s.originalAmount).join(' + ')
      if (units.length > 1) {
        item.mergedNote = `注意：单位不一致（${units.join('、')}）`
      }
      item.isMerged = item.sources.length > 1
      item.sources.forEach(s => {
        s.scaledAmount = scaleAmount(s.originalAmount, scale)
      })
    }
    delete item.sourceNames
    delete item.displayNames
  }

  return merged
})

const categorySections = computed(() => {
  const byCategory = {}
  for (const rule of CATEGORY_RULES) {
    byCategory[rule.name] = { ...rule, items: [] }
  }

  for (const key of Object.keys(mergedIngredients.value)) {
    const item = mergedIngredients.value[key]
    const catName = categorize(item.name)
    if (byCategory[catName]) {
      byCategory[catName].items.push(item)
    }
  }

  const sections = []
  for (const rule of CATEGORY_RULES) {
    if (byCategory[rule.name].items.length > 0) {
      sections.push(byCategory[rule.name])
    }
  }
  return sections
})

const totalIngredientCount = computed(() => {
  return Object.keys(mergedIngredients.value).length
})

const checkedCount = computed(() => checkedKeys.value.size)
const uncheckedCount = computed(() => totalIngredientCount.value - checkedCount.value)

const isChecked = (key) => checkedKeys.value.has(key)

const toggleCheck = (key) => {
  if (checkedKeys.value.has(key)) {
    checkedKeys.value.delete(key)
  } else {
    checkedKeys.value.add(key)
  }
  checkedKeys.value = new Set(checkedKeys.value)
  saveCheckedToStorage()
}

const getSectionCheckedCount = (section) => {
  return section.items.filter(it => checkedKeys.value.has(it.key)).length
}

const getSectionProgress = (section) => {
  if (!section.items.length) return 0
  return Math.round((getSectionCheckedCount(section) / section.items.length) * 100)
}

const handleResetChecked = async () => {
  try {
    await ElMessageBox.confirm('确定要重置所有食材的勾选进度吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    checkedKeys.value = new Set()
    saveCheckedToStorage()
    ElMessage.success('已重置')
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/profile')
  }
}

onMounted(async () => {
  loading.value = true
  try {
    await loadAliasMap()
    await loadRecipes()
    loadScaleFromStorage()
    loadCheckedFromStorage()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.prepare-list {
  min-height: 60vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 12px;
  padding: 20px 28px;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin: 0;
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.recipe-count-badge,
.progress-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: #faf7f2;
  border-radius: 20px;
  font-size: 14px;
  color: #666;
}

.progress-badge {
  background: linear-gradient(135deg, #f0f9eb 0%, #e8f7de 100%);
  color: #67c23a;
}

.recipes-strip {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 20px;
}

.scale-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.recipe-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px 6px 6px;
  background: #faf7f2;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.2s ease;
  flex-shrink: 0;
  border: 1px solid #f0ebe3;
}

.recipe-chip:hover {
  background: #fff5ed;
  border-color: #ffd4b8;
  transform: translateY(-1px);
}

.recipe-chip img,
.chip-placeholder {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  background: #ffe4d1;
}

.chip-title {
  font-size: 13px;
  color: #555;
  white-space: nowrap;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.summary-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
}

.summary-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
  padding-left: 10px;
  border-left: 3px solid #ff6b6b;
}

.summary-categories {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fafafa;
  border-radius: 10px;
  border-left: 3px solid var(--cat-color);
  transition: all 0.2s ease;
  cursor: pointer;
}

.summary-item:hover {
  background: #fff9f3;
  transform: translateY(-1px);
}

.summary-icon {
  font-size: 28px;
  flex-shrink: 0;
}

.summary-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 2px;
}

.summary-count {
  font-size: 12px;
  color: #999;
}

.empty {
  padding: 80px 0;
}

.ingredient-sections {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.ingredient-section {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-left: 6px solid;
  background: linear-gradient(90deg, #fafafa 0%, #ffffff 100%);
}

.section-title-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
}

.section-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #fff;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.section-progress {
  display: flex;
  align-items: center;
  gap: 10px;
}

.progress-text {
  font-size: 13px;
  color: #999;
  min-width: 50px;
  text-align: right;
}

.ingredient-list {
  padding: 8px 0;
}

.ingredient-row {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px 24px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-bottom: 1px solid #fafafa;
}

.ingredient-row:last-child {
  border-bottom: none;
}

.ingredient-row:hover {
  background: #faf9f7;
}

.ingredient-row.row-checked {
  background: linear-gradient(90deg, #f0f9eb 0%, #f6fbf0 100%);
}

.ingredient-row.row-checked .row-name {
  text-decoration: line-through;
  color: #a8a8a8;
}

.row-checkbox {
  padding-top: 2px;
  flex-shrink: 0;
}

.row-main {
  flex: 1;
  min-width: 0;
}

.row-name {
  font-size: 15px;
  font-weight: 500;
  color: #333;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s ease;
}

.alias-tag {
  font-weight: normal;
}

.row-sources {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.source-chip {
  display: inline-block;
  padding: 3px 10px;
  background: #f4f4f5;
  color: #909399;
  border-radius: 12px;
  font-size: 12px;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-amount {
  text-align: right;
  flex-shrink: 0;
  min-width: 100px;
}

.amount-main {
  font-size: 18px;
  font-weight: 600;
  color: #ff6b6b;
}

.amount-main.amount-merged {
  color: #e6a23c;
}

.row-checked .amount-main {
  color: #67c23a;
}

.amount-original {
  font-size: 12px;
  color: #c0c4cc;
  text-decoration: line-through;
  margin-top: 2px;
}

.amount-note {
  font-size: 11px;
  color: #c0c4cc;
  margin-top: 2px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.completion-banner {
  margin-top: 24px;
  padding: 28px;
  background: linear-gradient(135deg, #f0f9eb 0%, #e8f7de 50%, #f5fbf0 100%);
  border-radius: 16px;
  display: flex;
  align-items: center;
  gap: 20px;
  justify-content: center;
  animation: banner-in 0.5s ease;
  border: 1px solid #d4ecc4;
}

@keyframes banner-in {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.completion-text {
  text-align: left;
}

.completion-title {
  font-size: 18px;
  font-weight: 600;
  color: #67c23a;
  margin-bottom: 4px;
}

.completion-subtitle {
  font-size: 14px;
  color: #95c77c;
}

@media (max-width: 768px) {
  .page-header {
    flex-wrap: wrap;
    gap: 12px;
    padding: 16px;
  }

  .header-right {
    width: 100%;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 10px;
  }

  .summary-categories {
    grid-template-columns: repeat(2, 1fr);
  }

  .ingredient-row {
    flex-wrap: wrap;
    padding: 12px 16px;
  }

  .row-amount {
    width: 100%;
    text-align: left;
    padding-left: 34px;
  }

  .section-header {
    padding: 14px 16px;
    flex-wrap: wrap;
    gap: 10px;
  }
}
</style>
