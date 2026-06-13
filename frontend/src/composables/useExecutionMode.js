import { ref, computed, watch } from 'vue'

export function useExecutionMode(stepsGetter, completedStepsRef, options = {}) {
  const storageKey = options.storageKey || null

  const executionActive = ref(false)
  const executionStepIndex = ref(0)

  const steps = computed(() => stepsGetter())

  const currentStep = computed(() => {
    const list = steps.value
    if (!list.length || executionStepIndex.value >= list.length) return null
    return list[executionStepIndex.value]
  })

  const totalSteps = computed(() => steps.value.length)

  const remainingSteps = computed(() => {
    return Math.max(0, totalSteps.value - executionStepIndex.value - 1)
  })

  const progressPercent = computed(() => {
    if (!totalSteps.value) return 0
    return Math.round(((executionStepIndex.value + 1) / totalSteps.value) * 100)
  })

  const isLastStep = computed(() => {
    return executionStepIndex.value >= totalSteps.value - 1
  })

  const isAllDone = computed(() => {
    return executionStepIndex.value >= totalSteps.value
  })

  const saveExecState = () => {
    if (!storageKey) return
    try {
      localStorage.setItem(storageKey, JSON.stringify({
        executionActive: executionActive.value,
        executionStepIndex: executionStepIndex.value
      }))
    } catch (e) {
      console.error('Failed to save execution state:', e)
    }
  }

  const clearExecState = () => {
    if (!storageKey) return
    try {
      localStorage.removeItem(storageKey)
    } catch (e) {
      // ignore
    }
  }

  const loadExecState = () => {
    if (!storageKey) return false
    try {
      const data = localStorage.getItem(storageKey)
      if (!data) return false
      const parsed = JSON.parse(data)
      if (parsed.executionActive && steps.value.length > 0) {
        executionActive.value = true
        executionStepIndex.value = Math.min(parsed.executionStepIndex || 0, steps.value.length - 1)
        return true
      }
    } catch (e) {
      console.error('Failed to load execution state:', e)
    }
    return false
  }

  const enterExecutionMode = () => {
    const list = steps.value
    if (!list.length) return
    const completed = completedStepsRef.value
    let startIdx = 0
    for (let i = 0; i < list.length; i++) {
      if (!completed.has(i)) {
        startIdx = i
        break
      }
      if (i === list.length - 1) startIdx = 0
    }
    executionStepIndex.value = startIdx
    executionActive.value = true
    saveExecState()
  }

  const exitExecutionMode = () => {
    executionActive.value = false
    clearExecState()
  }

  const goNext = () => {
    if (executionStepIndex.value < totalSteps.value) {
      completedStepsRef.value.add(executionStepIndex.value)
      completedStepsRef.value = new Set(completedStepsRef.value)
      executionStepIndex.value++
      saveExecState()
    }
  }

  const goPrev = () => {
    if (executionStepIndex.value > 0) {
      completedStepsRef.value.delete(executionStepIndex.value - 1)
      completedStepsRef.value = new Set(completedStepsRef.value)
      executionStepIndex.value--
      saveExecState()
    }
  }

  const goToStep = (idx) => {
    if (idx >= 0 && idx < totalSteps.value) {
      for (let i = 0; i < idx; i++) {
        completedStepsRef.value.add(i)
      }
      completedStepsRef.value.delete(idx)
      completedStepsRef.value = new Set(completedStepsRef.value)
      executionStepIndex.value = idx
      saveExecState()
    }
  }

  return {
    executionActive,
    executionStepIndex,
    currentStep,
    totalSteps,
    remainingSteps,
    progressPercent,
    isLastStep,
    isAllDone,
    enterExecutionMode,
    exitExecutionMode,
    goNext,
    goPrev,
    goToStep,
    loadExecState,
    saveExecState,
    clearExecState
  }
}
