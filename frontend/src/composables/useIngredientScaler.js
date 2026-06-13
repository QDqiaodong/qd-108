import { ref, computed, watch } from 'vue'

const AMOUNT_REGEX = /^([\d.\/]+)\s*(.*)$/

function parseNumericValue(str) {
  if (!str) return null
  if (str.includes('/')) {
    const parts = str.split('/')
    if (parts.length === 2) {
      const num = parseFloat(parts[0])
      const den = parseFloat(parts[1])
      if (!isNaN(num) && !isNaN(den) && den !== 0) return num / den
    }
  }
  const val = parseFloat(str)
  return isNaN(val) ? null : val
}

function formatScaledValue(value) {
  if (value === Math.floor(value)) return String(value)
  const rounded = Math.round(value * 100) / 100
  if (rounded === Math.floor(rounded)) return String(Math.floor(rounded))
  return String(rounded)
}

function scaleAmount(amountStr, multiplier) {
  if (multiplier === 1) return amountStr
  const match = amountStr.trim().match(AMOUNT_REGEX)
  if (!match) return amountStr

  const numericPart = parseNumericValue(match[1])
  if (numericPart === null) return amountStr

  const unit = match[2] || ''
  const scaled = numericPart * multiplier
  return `${formatScaledValue(scaled)}${unit ? ' ' + unit : ''}`
}

export function useIngredientScaler(ingredientsGetter, options = {}) {
  const { storageKey = null } = options

  const loadFromStorage = () => {
    if (!storageKey) return 1
    try {
      const stored = localStorage.getItem(`scale_factor_${storageKey}`)
      if (stored) {
        const val = parseFloat(stored)
        if (!isNaN(val) && val >= 0.25 && val <= 10) return val
      }
    } catch (e) {
      console.error('加载缩放因子失败', e)
    }
    return 1
  }

  const scaleFactor = ref(loadFromStorage())

  const saveToStorage = (val) => {
    if (!storageKey) return
    try {
      localStorage.setItem(`scale_factor_${storageKey}`, String(val))
    } catch (e) {
      console.error('保存缩放因子失败', e)
    }
  }

  watch(scaleFactor, (newVal) => {
    saveToStorage(newVal)
  })

  const scaledIngredients = computed(() => {
    const source = ingredientsGetter()
    if (!source || !source.length) return []
    if (scaleFactor.value === 1) return source
    return source.map(item => ({
      ...item,
      amount: scaleAmount(item.amount, scaleFactor.value),
      originalAmount: item.amount
    }))
  })

  const setScale = (factor) => {
    scaleFactor.value = Math.max(0.25, Math.min(10, factor))
  }

  const increaseScale = (step = 0.5) => {
    setScale(scaleFactor.value + step)
  }

  const decreaseScale = (step = 0.5) => {
    setScale(scaleFactor.value - step)
  }

  const resetScale = () => {
    scaleFactor.value = 1
  }

  return {
    scaleFactor,
    scaledIngredients,
    setScale,
    increaseScale,
    decreaseScale,
    resetScale
  }
}
