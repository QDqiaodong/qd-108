<template>
  <div class="recipe-card card" @click="$router.push(`/recipe/${recipe.id}`)">
    <div class="recipe-image">
      <img
        v-if="recipe.coverImage"
        :src="resolveImageUrl(recipe.coverImage)"
        :alt="recipe.title"
        loading="lazy"
      />
      <div v-else class="image-placeholder" style="height: 200px;"></div>
      <div class="recipe-category" v-if="recipe.categoryName">
        {{ recipe.categoryName }}
      </div>
    </div>
    <div class="recipe-content">
      <h3 class="recipe-title">{{ recipe.title }}</h3>
      <p class="recipe-desc" v-if="recipe.description">{{ recipe.description }}</p>
      <div class="recipe-meta">
        <div class="meta-item">
          <el-icon><View /></el-icon>
          <span>{{ recipe.viewCount || 0 }}</span>
        </div>
        <div class="meta-item">
          <el-icon><Star /></el-icon>
          <span>{{ recipe.favoriteCount || 0 }}</span>
        </div>
        <div class="meta-item">
          <el-icon><ChatDotRound /></el-icon>
          <span>{{ recipe.commentCount || 0 }}</span>
        </div>
      </div>
      <div class="recipe-author" v-if="recipe.authorName">
        <el-avatar :size="24" :src="recipe.authorAvatar">
          {{ recipe.authorName.charAt(0) }}
        </el-avatar>
        <span>{{ recipe.authorName }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { View, Star, ChatDotRound } from '@element-plus/icons-vue'

defineProps({
  recipe: {
    type: Object,
    required: true
  }
})

const resolveImageUrl = (url) => {
  if (!url) return url
  if (url.startsWith('/uploads/') && !url.startsWith('/api/uploads/')) {
    return '/api' + url
  }
  return url
}
</script>

<style scoped>
.recipe-card {
  cursor: pointer;
}

.recipe-image {
  position: relative;
  height: 200px;
  overflow: hidden;
}

.recipe-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.recipe-card:hover .recipe-image img {
  transform: scale(1.05);
}

.recipe-category {
  position: absolute;
  top: 12px;
  left: 12px;
  background: rgba(255, 107, 107, 0.9);
  color: #fff;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
}

.recipe-content {
  padding: 16px;
}

.recipe-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recipe-desc {
  font-size: 13px;
  color: #999;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}

.recipe-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #999;
}

.meta-item .el-icon {
  font-size: 14px;
}

.recipe-author {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
  padding-top: 12px;
  border-top: 1px solid #f5f5f5;
}
</style>
