<template>
  <div class="category-ribbon">
    <div class="ribbon-scroll">
      <div class="ribbon-track">
        <div
          class="ribbon-item all"
          :class="{ active: !activeCategoryId }"
          @click="handleSelect(null)"
        >
          <span class="ribbon-label">全部</span>
        </div>
        <div
          v-for="cat in mergedCategories"
          :key="cat.id"
          class="ribbon-item"
          :class="{ active: activeCategoryId == cat.id }"
          :style="getCategoryStyle(cat.name)"
          @click="handleSelect(cat.id)"
        >
          <span class="ribbon-icon">{{ getCategoryIcon(cat.name) }}</span>
          <span class="ribbon-label">{{ getShortLabel(cat.name) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  categories: {
    type: Array,
    default: () => []
  },
  activeCategoryId: {
    type: [Number, String, null],
    default: null
  },
  mode: {
    type: String,
    default: 'select'
  }
})

const emit = defineEmits(['select', 'change'])

const categoryPresets = {
  '面包': {
    short: '面包',
    icon: '🍞',
    colors: {
      bg: '#F5E6D3',
      border: '#D4A574',
      text: '#8B5A2B',
      gradient: 'linear-gradient(135deg, #F5E6D3 0%, #E8D0A9 100%)',
      activeGradient: 'linear-gradient(135deg, #D4A574 0%, #B8864E 100%)'
    }
  },
  '蛋糕': {
    short: '蛋糕',
    icon: '🎂',
    colors: {
      bg: '#FCE4EC',
      border: '#F48FB1',
      text: '#AD1457',
      gradient: 'linear-gradient(135deg, #FCE4EC 0%, #F8BBD9 100%)',
      activeGradient: 'linear-gradient(135deg, #F48FB1 0%, #EC407A 100%)'
    }
  },
  '饼干': {
    short: '饼干',
    icon: '🍪',
    colors: {
      bg: '#FFF3E0',
      border: '#FFB74D',
      text: '#E65100',
      gradient: 'linear-gradient(135deg, #FFF3E0 0%, #FFE0B2 100%)',
      activeGradient: 'linear-gradient(135deg, #FFB74D 0%, #FF9800 100%)'
    }
  },
  '甜点': {
    short: '甜点',
    icon: '🍮',
    colors: {
      bg: '#F3E5F5',
      border: '#CE93D8',
      text: '#6A1B9A',
      gradient: 'linear-gradient(135deg, #F3E5F5 0%, #E1BEE7 100%)',
      activeGradient: 'linear-gradient(135deg, #CE93D8 0%, #AB47BC 100%)'
    }
  },
  '披萨': {
    short: '披萨',
    icon: '🍕',
    colors: {
      bg: '#FFEBEE',
      border: '#EF5350',
      text: '#C62828',
      gradient: 'linear-gradient(135deg, #FFEBEE 0%, #FFCDD2 100%)',
      activeGradient: 'linear-gradient(135deg, #EF5350 0%, #E53935 100%)'
    }
  },
  '中式面点': {
    short: '面点',
    icon: '🥟',
    colors: {
      bg: '#E8F5E9',
      border: '#66BB6A',
      text: '#2E7D32',
      gradient: 'linear-gradient(135deg, #E8F5E9 0%, #C8E6C9 100%)',
      activeGradient: 'linear-gradient(135deg, #66BB6A 0%, #43A047 100%)'
    }
  }
}

const defaultPreset = {
  colors: {
    bg: '#ECEFF1',
    border: '#90A4AE',
    text: '#37474F',
    gradient: 'linear-gradient(135deg, #ECEFF1 0%, #CFD8DC 100%)',
    activeGradient: 'linear-gradient(135deg, #90A4AE 0%, #607D8B 100%)'
  }
}

const mergedCategories = computed(() => props.categories)

const getPreset = (name) => categoryPresets[name] || defaultPreset

const getShortLabel = (name) => getPreset(name).short || name

const getCategoryIcon = (name) => getPreset(name).icon || '🍰'

const getCategoryStyle = (name) => {
  const preset = getPreset(name)
  const isActive = props.activeCategoryId == (
    mergedCategories.value.find(c => c.name === name)?.id
  )
  return {
    '--ribbon-bg': preset.colors.bg,
    '--ribbon-border': preset.colors.border,
    '--ribbon-text': preset.colors.text,
    '--ribbon-gradient': isActive ? preset.colors.activeGradient : preset.colors.gradient,
    color: isActive ? '#fff' : preset.colors.text,
    borderColor: isActive ? preset.colors.border : 'transparent'
  }
}

const handleSelect = (id) => {
  emit('select', id)
  emit('change', id)
}
</script>

<style scoped>
.category-ribbon {
  width: 100%;
  margin-bottom: 12px;
}

.ribbon-scroll {
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  margin: 0 -24px;
  padding: 0 24px;
}

.ribbon-scroll::-webkit-scrollbar {
  display: none;
}

.ribbon-track {
  display: flex;
  gap: 12px;
  padding: 8px 0 12px 0;
}

.ribbon-item {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  border-radius: 14px;
  background: var(--ribbon-gradient, linear-gradient(135deg, #ECEFF1 0%, #CFD8DC 100%));
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  user-select: none;
}

.ribbon-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.12);
}

.ribbon-item.active {
  border-color: var(--ribbon-border, #90A4AE);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.15);
  transform: translateY(-1px);
}

.ribbon-item.all {
  --ribbon-bg: #ECEFF1;
  --ribbon-border: #78909C;
  --ribbon-text: #455A64;
  --ribbon-gradient: linear-gradient(135deg, #ECEFF1 0%, #B0BEC5 100%);
}

.ribbon-item.all.active {
  --ribbon-gradient: linear-gradient(135deg, #78909C 0%, #546E7A 100%);
  color: #fff;
}

.ribbon-icon {
  font-size: 22px;
  line-height: 1;
  display: inline-flex;
  align-items: center;
}

.ribbon-label {
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
  letter-spacing: 0.3px;
}
</style>
