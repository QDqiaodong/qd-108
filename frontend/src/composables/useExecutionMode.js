import { ref, computed, watch } from 'vue'

export function useExecutionMode(stepsGetter, completedStepsRef) {
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
  }

  const exitExecutionMode = () => {
    executionActive.value = false
  }

  const goNext = () => {
    if (executionStepIndex.value < totalSteps.value) {
      completedStepsRef.value.add(executionStepIndex.value)
      completedStepsRef.value = new Set(completedStepsRef.value)
      executionStepIndex.value++
    }
  }

  const goPrev = () => {
    if (executionStepIndex.value > 0) {
      completedStepsRef.value.delete(executionStepIndex.value - 1)
      completedStepsRef.value = new Set(completedStepsRef.value)
      executionStepIndex.value--
    }
  }

  const goToStep = (idx) => {
    if (idx >= 0 && idx < totalSteps.value) {
      executionStepIndex.value = idx
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
    goToStep
  }
}
