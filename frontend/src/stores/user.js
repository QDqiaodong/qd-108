import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  const isLogin = computed(() => !!userInfo.value)

  const setUserInfo = (user) => {
    userInfo.value = user
    localStorage.setItem('userInfo', JSON.stringify(user))
  }

  const logout = () => {
    userInfo.value = null
    localStorage.removeItem('userInfo')
  }

  return {
    userInfo,
    isLogin,
    setUserInfo,
    logout
  }
})
