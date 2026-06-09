<template>
  <div class="publish">
    <div class="container">
      <div class="publish-card">
        <h1 class="page-title">发布烘焙配方</h1>

        <el-form :model="form" label-width="120px" ref="formRef">
          <el-form-item label="配方标题" required>
            <el-input v-model="form.title" placeholder="请输入配方标题" maxlength="100" show-word-limit />
          </el-form-item>

          <el-form-item label="封面图片" required>
            <el-upload
              :action="uploadUrl"
              :headers="uploadHeaders"
              list-type="picture-card"
              :limit="1"
              :on-success="handleCoverSuccess"
              :before-upload="beforeUpload"
              accept="image/*"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </el-form-item>

          <el-form-item label="配方简介">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="3"
              placeholder="简单介绍一下你的配方..."
              maxlength="500"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="所属分类" required>
            <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 300px">
              <el-option
                v-for="cat in categories"
                :key="cat.id"
                :label="cat.name"
                :value="cat.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="难度等级">
            <el-radio-group v-model="form.difficulty">
              <el-radio :value="1">简单</el-radio>
              <el-radio :value="2">中等</el-radio>
              <el-radio :value="3">困难</el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="烘焙温度">
            <el-input-number v-model="form.bakeTemp" :min="0" :max="300" :step="5" />
            <span style="margin-left: 8px">℃</span>
          </el-form-item>

          <el-form-item label="烘焙时长">
            <el-input-number v-model="form.bakeTime" :min="0" :max="600" :step="5" />
            <span style="margin-left: 8px">分钟</span>
          </el-form-item>

          <el-form-item label="份量">
            <el-input-number v-model="form.servings" :min="1" :max="20" />
            <span style="margin-left: 8px">人份</span>
          </el-form-item>

          <el-divider content-position="left">食材清单</el-divider>
          <el-form-item label="食材配比">
            <div class="ingredients-wrapper">
              <div
                v-for="(item, idx) in ingredients"
                :key="idx"
                class="ingredient-row"
              >
                <el-input
                  v-model="item.name"
                  placeholder="食材名称"
                  style="width: 200px; margin-right: 12px"
                />
                <el-input
                  v-model="item.amount"
                  placeholder="用量"
                  style="width: 200px; margin-right: 12px"
                />
                <el-button type="danger" text @click="removeIngredient(idx)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              <el-button type="primary" text @click="addIngredient">
                <el-icon><Plus /></el-icon>
                添加食材
              </el-button>
            </div>
          </el-form-item>

          <el-divider content-position="left">制作步骤</el-divider>
          <el-form-item label="制作步骤">
            <div class="steps-wrapper">
              <div
                v-for="(step, idx) in steps"
                :key="idx"
                class="step-row"
              >
                <div class="step-number">{{ idx + 1 }}</div>
                <div class="step-body">
                  <el-input
                    v-model="step.description"
                    type="textarea"
                    :rows="2"
                    placeholder="描述这一步的操作..."
                  />
                  <el-upload
                    :action="uploadUrl"
                    :headers="uploadHeaders"
                    :show-file-list="false"
                    :on-success="(res) => handleStepImageSuccess(idx, res)"
                    :before-upload="beforeUpload"
                    accept="image/*"
                  >
                    <el-button v-if="!step.image" size="small">
                      <el-icon><Picture /></el-icon>
                      添加步骤图
                    </el-button>
                    <div v-else class="step-image">
                      <img :src="step.image" />
                      <el-button type="danger" text @click="step.image = ''">
                        删除
                      </el-button>
                    </div>
                  </el-upload>
                </div>
                <el-button type="danger" text @click="removeStep(idx)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
              <el-button type="primary" text @click="addStep">
                <el-icon><Plus /></el-icon>
                添加步骤
              </el-button>
            </div>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" size="large" @click="submitForm" :loading="submitting">
              发布配方
            </el-button>
            <el-button size="large" @click="saveDraft">保存草稿</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Picture } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getCategories, createRecipe, uploadImage } from '@/api'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const submitting = ref(false)
const categories = ref([])
const uploadUrl = '/api/upload/image'
const uploadHeaders = {}

const form = reactive({
  title: '',
  coverImage: '',
  description: '',
  categoryId: null,
  difficulty: 1,
  bakeTemp: null,
  bakeTime: null,
  servings: null
})

const ingredients = ref([{ name: '', amount: '' }])
const steps = ref([{ description: '', image: '' }])

const DRAFT_KEY = 'publish_draft'

onMounted(() => {
  loadCategories()
  loadDraft()
})

watch([ingredients, steps, form], () => {
  saveToLocal()
}, { deep: true })

const loadCategories = async () => {
  try {
    categories.value = await getCategories()
  } catch (e) {
    console.error(e)
  }
}

const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB!')
    return false
  }
  return true
}

const handleCoverSuccess = (response) => {
  form.coverImage = response.data.url
  ElMessage.success('封面上传成功')
}

const handleStepImageSuccess = (idx, response) => {
  steps.value[idx].image = response.data.url
  ElMessage.success('图片上传成功')
}

const addIngredient = () => {
  ingredients.value.push({ name: '', amount: '' })
}

const removeIngredient = (idx) => {
  if (ingredients.value.length > 1) {
    ingredients.value.splice(idx, 1)
  } else {
    ElMessage.warning('至少保留一种食材')
  }
}

const addStep = () => {
  steps.value.push({ description: '', image: '' })
}

const removeStep = (idx) => {
  if (steps.value.length > 1) {
    steps.value.splice(idx, 1)
  } else {
    ElMessage.warning('至少保留一个步骤')
  }
}

const saveToLocal = () => {
  const draft = {
    form: { ...form },
    ingredients: [...ingredients.value],
    steps: [...steps.value],
    savedAt: Date.now()
  }
  localStorage.setItem(DRAFT_KEY, JSON.stringify(draft))
}

const loadDraft = () => {
  const draftStr = localStorage.getItem(DRAFT_KEY)
  if (draftStr) {
    const draft = JSON.parse(draftStr)
    Object.assign(form, draft.form)
    ingredients.value = draft.ingredients
    steps.value = draft.steps
  }
}

const saveDraft = () => {
  saveToLocal()
  ElMessage.success('草稿已保存')
}

const validateForm = () => {
  if (!form.title.trim()) {
    ElMessage.warning('请输入配方标题')
    return false
  }
  if (!form.coverImage) {
    ElMessage.warning('请上传封面图片')
    return false
  }
  if (!form.categoryId) {
    ElMessage.warning('请选择分类')
    return false
  }
  const validIngredients = ingredients.value.filter(i => i.name.trim())
  if (validIngredients.length === 0) {
    ElMessage.warning('请至少添加一种食材')
    return false
  }
  const validSteps = steps.value.filter(s => s.description.trim())
  if (validSteps.length === 0) {
    ElMessage.warning('请至少添加一个制作步骤')
    return false
  }
  return true
}

const submitForm = async () => {
  if (!userStore.isLogin) {
    ElMessage.warning('请先登录')
    return
  }
  if (!validateForm()) return

  try {
    await ElMessageBox.confirm(
      '确认发布此配方吗？发布后可以在个人中心查看。',
      '发布确认',
      { type: 'info' }
    )
  } catch {
    return
  }

  submitting.value = true
  try {
    const validIngredients = ingredients.value.filter(i => i.name.trim())
    const validSteps = steps.value.filter(s => s.description.trim())

    await createRecipe({
      ...form,
      userId: userStore.userInfo.id,
      ingredients: JSON.stringify(validIngredients),
      steps: JSON.stringify(validSteps),
      images: [form.coverImage, ...validSteps.filter(s => s.image).map(s => s.image)]
    })

    localStorage.removeItem(DRAFT_KEY)
    ElMessage.success('发布成功！')
    router.push('/')
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.publish {
  min-height: 60vh;
}

.publish-card {
  background: #fff;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #333;
  margin-bottom: 30px;
  text-align: center;
}

.ingredients-wrapper,
.steps-wrapper {
  width: 100%;
}

.ingredient-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}

.step-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.step-row:last-child {
  border-bottom: none;
}

.step-number {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff9a56 0%, #ff6b6b 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  margin-right: 12px;
  flex-shrink: 0;
}

.step-body {
  flex: 1;
  margin-right: 12px;
}

.step-body :deep(.el-textarea) {
  margin-bottom: 12px;
}

.step-image {
  position: relative;
  display: inline-block;
  margin-top: 8px;
}

.step-image img {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 6px;
}

.step-image .el-button {
  position: absolute;
  top: 4px;
  right: 4px;
}
</style>
