<template>
  <el-dialog
    v-model="dialogVisible"
    title="登录 / 注册"
    width="420px"
    :close-on-click-modal="false"
  >
    <el-tabs v-model="activeTab" class="login-tabs">
      <el-tab-pane label="登录" name="login">
        <el-form :model="loginForm" label-width="80px" @submit.prevent="handleLogin">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="loginForm.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" native-type="submit" style="width: 100%">登录</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="注册" name="register">
        <el-form :model="registerForm" label-width="80px" @submit.prevent="handleRegister">
          <el-form-item label="用户名" prop="username">
            <el-input v-model="registerForm.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="registerForm.nickname" placeholder="请输入昵称" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" show-password />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="registerForm.confirmPassword" type="password" placeholder="请再次输入密码" show-password />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" native-type="submit" style="width: 100%">注册</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { login, register } from '@/api'

const props = defineProps({
  visible: Boolean
})

const emit = defineEmits(['update:visible', 'login-success'])

const userStore = useUserStore()
const activeTab = ref('login')
const loginForm = ref({
  username: '',
  password: ''
})
const registerForm = ref({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  try {
    const user = await login(loginForm.value)
    userStore.setUserInfo(user)
    ElMessage.success('登录成功')
    emit('login-success')
    dialogVisible.value = false
  } catch (e) {
    console.error(e)
  }
}

const handleRegister = async () => {
  if (!registerForm.value.username || !registerForm.value.password || !registerForm.value.nickname) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    ElMessage.warning('两次密码输入不一致')
    return
  }
  try {
    const user = await register({
      username: registerForm.value.username,
      password: registerForm.value.password,
      nickname: registerForm.value.nickname
    })
    userStore.setUserInfo(user)
    ElMessage.success('注册成功')
    emit('login-success')
    dialogVisible.value = false
  } catch (e) {
    console.error(e)
  }
}
</script>

<style scoped>
.login-tabs {
  margin-top: 20px;
}
</style>
