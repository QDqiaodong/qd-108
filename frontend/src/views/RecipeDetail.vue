<template>
  <div class="recipe-detail" v-loading="loading">
    <div class="container" v-if="recipe">
      <div class="detail-header">
        <div class="recipe-gallery">
          <div class="main-image">
            <img v-if="recipe.coverImage" :src="recipe.coverImage" :alt="recipe.title" />
            <div v-else class="image-placeholder" style="height: 400px;"></div>
          </div>
          <div class="thumb-list" v-if="recipe.images && recipe.images.length">
            <div
              v-for="(img, idx) in recipe.images"
              :key="img.id"
              class="thumb-item"
              :class="{ active: currentImage === idx }"
              @click="currentImage = idx"
            >
              <img :src="img.imageUrl" />
            </div>
          </div>
        </div>

        <div class="recipe-info">
          <h1 class="recipe-title">{{ recipe.title }}</h1>
          <div class="recipe-meta-top">
            <div class="author">
              <el-avatar :size="40" :src="recipe.authorAvatar">
                {{ recipe.authorName?.charAt(0) }}
              </el-avatar>
              <div class="author-info">
                <span class="author-name">{{ recipe.authorName }}</span>
                <span class="publish-time">{{ recipe.createdAt }}</span>
              </div>
            </div>
            <div class="actions">
              <el-button
                :type="isFavorited ? 'danger' : 'default'"
                @click="toggleFavorite"
                :icon="isFavorited ? StarFilled : Star"
              >
                {{ isFavorited ? '已收藏' : '收藏' }}
              </el-button>
            </div>
          </div>

          <p class="recipe-description" v-if="recipe.description">{{ recipe.description }}</p>

          <div class="recipe-stats">
            <div class="stat-item">
              <span class="stat-value">{{ recipe.viewCount || 0 }}</span>
              <span class="stat-label">浏览</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ recipe.favoriteCount || 0 }}</span>
              <span class="stat-label">收藏</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ recipe.commentCount || 0 }}</span>
              <span class="stat-label">评论</span>
            </div>
          </div>

          <div class="recipe-tags">
            <el-tag v-if="recipe.categoryName" type="warning" effect="light">{{ recipe.categoryName }}</el-tag>
            <el-tag v-if="recipe.difficulty" type="info" effect="light">
              {{ getDifficultyText(recipe.difficulty) }}
            </el-tag>
            <el-tag v-if="recipe.bakeTime" type="success" effect="light">
              {{ recipe.bakeTime }}分钟
            </el-tag>
            <el-tag v-if="recipe.bakeTemp" type="danger" effect="light">
              {{ recipe.bakeTemp }}℃
            </el-tag>
            <el-tag v-if="recipe.servings" type="primary" effect="light">
              {{ recipe.servings }}人份
            </el-tag>
          </div>
        </div>
      </div>

      <div class="detail-content">
        <div class="content-section" v-if="ingredients.length">
          <h2 class="section-title">食材清单</h2>
          <div class="ingredients-list">
            <div v-for="(item, idx) in ingredients" :key="idx" class="ingredient-item">
              <span class="ingredient-name">{{ item.name }}</span>
              <span class="ingredient-amount">{{ item.amount }}</span>
            </div>
          </div>
        </div>

        <RecipeTimer :recipe-id="recipe.id" />

        <div class="content-section" v-if="steps.length">
          <h2 class="section-title">制作步骤</h2>
          <div class="steps-list">
            <div v-for="(step, idx) in steps" :key="idx" class="step-item">
              <div class="step-number">{{ idx + 1 }}</div>
              <div class="step-content">
                <p>{{ step.description }}</p>
                <img v-if="step.image" :src="step.image" />
              </div>
            </div>
          </div>
        </div>

        <div class="content-section">
          <h2 class="section-title">制作心得 ({{ recipe.commentCount || 0 }})</h2>
          <div class="comment-input">
            <el-input
              v-model="commentContent"
              type="textarea"
              :rows="3"
              placeholder="分享你的制作心得..."
            />
            <div class="comment-actions">
              <el-button type="primary" @click="submitComment" :disabled="!commentContent.trim()">
                发布评论
              </el-button>
            </div>
          </div>

          <div class="comments-list" v-loading="commentsLoading">
            <div v-for="comment in comments" :key="comment.id" class="comment-item">
              <div class="comment-header">
                <el-avatar :size="36" :src="comment.userAvatar">
                  {{ comment.userName?.charAt(0) }}
                </el-avatar>
                <div class="comment-info">
                  <span class="comment-user">{{ comment.userName }}</span>
                  <span class="comment-time">{{ comment.createdAt }}</span>
                </div>
                <el-button
                  text
                  size="small"
                  @click="likeComment(comment.id)"
                >
                  {{ comment.likeCount || 0 }}
                </el-button>
              </div>
              <p class="comment-content">{{ comment.content }}</p>
              <div class="comment-replies" v-if="comment.replies && comment.replies.length">
                <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                  <el-avatar :size="28" :src="reply.userAvatar">
                    {{ reply.userName?.charAt(0) }}
                  </el-avatar>
                  <div class="reply-content">
                    <span class="reply-user">{{ reply.userName }}</span>
                    <span>{{ reply.content }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import RecipeTimer from '@/components/RecipeTimer.vue'
import {
  getRecipeDetail,
  checkFavorite,
  addFavorite,
  removeFavorite,
  getComments,
  addComment,
  likeComment as likeCommentApi
} from '@/api'

const route = useRoute()
const userStore = useUserStore()
const recipe = ref(null)
const loading = ref(false)
const currentImage = ref(0)
const isFavorited = ref(false)
const commentContent = ref('')
const comments = ref([])
const commentsLoading = ref(false)
const showLogin = ref(false)

const ingredients = computed(() => {
  if (!recipe.value?.ingredients) return []
  try {
    return JSON.parse(recipe.value.ingredients)
  } catch {
    return []
  }
})

const steps = computed(() => {
  if (!recipe.value?.steps) return []
  try {
    return JSON.parse(recipe.value.steps)
  } catch {
    return []
  }
})

const getDifficultyText = (level) => {
  const map = { 1: '简单', 2: '中等', 3: '困难' }
  return map[level] || '简单'
}

onMounted(() => {
  loadRecipe()
  loadComments()
})

const loadRecipe = async () => {
  loading.value = true
  try {
    recipe.value = await getRecipeDetail(route.params.id)
    if (userStore.isLogin) {
      isFavorited.value = await checkFavorite(userStore.userInfo.id, route.params.id)
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const toggleFavorite = async () => {
  if (!userStore.isLogin) {
    showLogin.value = true
    return
  }
  try {
    if (isFavorited.value) {
      await removeFavorite(userStore.userInfo.id, route.params.id)
      isFavorited.value = false
      recipe.value.favoriteCount--
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite({ userId: userStore.userInfo.id, recipeId: route.params.id })
      isFavorited.value = true
      recipe.value.favoriteCount++
      ElMessage.success('收藏成功')
    }
  } catch (e) {
    console.error(e)
  }
}

const loadComments = async () => {
  commentsLoading.value = true
  try {
    const res = await getComments({ recipeId: route.params.id, pageNum: 1, pageSize: 20 })
    comments.value = res.records || []
  } catch (e) {
    console.error(e)
  } finally {
    commentsLoading.value = false
  }
}

const submitComment = async () => {
  if (!userStore.isLogin) {
    showLogin.value = true
    return
  }
  if (!commentContent.value.trim()) return
  try {
    await addComment({
      recipeId: route.params.id,
      userId: userStore.userInfo.id,
      content: commentContent.value.trim()
    })
    commentContent.value = ''
    recipe.value.commentCount++
    ElMessage.success('评论成功')
    loadComments()
  } catch (e) {
    console.error(e)
  }
}

const likeComment = async (id) => {
  try {
    await likeCommentApi(id)
    const comment = comments.value.find(c => c.id === id)
    if (comment) comment.likeCount++
  } catch (e) {
    console.error(e)
  }
}
</script>

<style scoped>
.recipe-detail {
  min-height: 60vh;
}

.detail-header {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  margin-bottom: 40px;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}

.recipe-gallery {
  position: sticky;
  top: 100px;
}

.main-image {
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 12px;
}

.main-image img {
  width: 100%;
  height: 400px;
  object-fit: cover;
}

.thumb-list {
  display: flex;
  gap: 8px;
  overflow-x: auto;
}

.thumb-item {
  width: 80px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid transparent;
  flex-shrink: 0;
}

.thumb-item.active {
  border-color: #ff6b6b;
}

.thumb-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.recipe-title {
  font-size: 28px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
}

.recipe-meta-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.author {
  display: flex;
  align-items: center;
  gap: 12px;
}

.author-info {
  display: flex;
  flex-direction: column;
}

.author-name {
  font-size: 15px;
  font-weight: 500;
  color: #333;
}

.publish-time {
  font-size: 13px;
  color: #999;
}

.recipe-description {
  font-size: 15px;
  color: #666;
  line-height: 1.8;
  margin-bottom: 24px;
}

.recipe-stats {
  display: flex;
  gap: 40px;
  margin-bottom: 24px;
  padding: 20px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}

.stat-item {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.stat-label {
  font-size: 13px;
  color: #999;
}

.recipe-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-content {
  background: #fff;
  border-radius: 12px;
  padding: 30px;
}

.content-section {
  margin-bottom: 40px;
}

.content-section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
  padding-left: 12px;
  border-left: 4px solid #ff6b6b;
}

.ingredients-list {
  background: #faf7f2;
  border-radius: 8px;
  padding: 20px;
}

.ingredient-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px dashed #eee;
}

.ingredient-item:last-child {
  border-bottom: none;
}

.ingredient-name {
  color: #333;
}

.ingredient-amount {
  color: #ff6b6b;
  font-weight: 500;
}

.steps-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.step-item {
  display: flex;
  gap: 16px;
}

.step-number {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff9a56 0%, #ff6b6b 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  flex-shrink: 0;
}

.step-content p {
  font-size: 15px;
  color: #333;
  line-height: 1.8;
  margin-bottom: 12px;
}

.step-content img {
  max-width: 400px;
  border-radius: 8px;
}

.comment-input {
  margin-bottom: 30px;
}

.comment-actions {
  margin-top: 12px;
  text-align: right;
}

.comments-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comment-item {
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.comment-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.comment-user {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-content {
  font-size: 14px;
  color: #666;
  line-height: 1.8;
  margin-left: 48px;
}

.comment-replies {
  margin-top: 16px;
  margin-left: 48px;
  padding: 16px;
  background: #faf7f2;
  border-radius: 8px;
}

.reply-item {
  display: flex;
  gap: 10px;
  padding: 8px 0;
}

.reply-content {
  flex: 1;
  font-size: 13px;
  color: #666;
}

.reply-user {
  font-weight: 500;
  color: #333;
  margin-right: 8px;
}
</style>
