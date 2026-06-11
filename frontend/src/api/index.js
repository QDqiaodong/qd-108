import request from './request'

export const getCategories = () => {
  return request({
    url: '/categories',
    method: 'get'
  })
}

export const getRecipes = (params) => {
  return request({
    url: '/recipes',
    method: 'get',
    params
  })
}

export const getHotRecipes = (limit = 10, categoryId = null) => {
  return request({
    url: '/recipes/hot',
    method: 'get',
    params: { limit, categoryId }
  })
}

export const getRecipeDetail = (id) => {
  return request({
    url: `/recipes/${id}`,
    method: 'get'
  })
}

export const createRecipe = (data) => {
  return request({
    url: '/recipes',
    method: 'post',
    data
  })
}

export const getUserRecipes = (userId, params) => {
  return request({
    url: `/recipes/user/${userId}`,
    method: 'get',
    params
  })
}

export const getFavoriteRecipes = (userId, params) => {
  return request({
    url: `/recipes/favorite/${userId}`,
    method: 'get',
    params
  })
}

export const checkFavorite = (userId, recipeId) => {
  return request({
    url: '/favorites/check',
    method: 'get',
    params: { userId, recipeId }
  })
}

export const addFavorite = (data) => {
  return request({
    url: '/favorites',
    method: 'post',
    data
  })
}

export const removeFavorite = (userId, recipeId) => {
  return request({
    url: '/favorites',
    method: 'delete',
    params: { userId, recipeId }
  })
}

export const getFavoriteFolders = (userId) => {
  return request({
    url: `/favorites/folders/${userId}`,
    method: 'get'
  })
}

export const createFavoriteFolder = (data) => {
  return request({
    url: '/favorites/folders',
    method: 'post',
    data
  })
}

export const getComments = (params) => {
  return request({
    url: '/comments',
    method: 'get',
    params
  })
}

export const addComment = (data) => {
  return request({
    url: '/comments',
    method: 'post',
    data
  })
}

export const likeComment = (id) => {
  return request({
    url: `/comments/${id}/like`,
    method: 'post'
  })
}

export const login = (data) => {
  return request({
    url: '/users/login',
    method: 'post',
    data
  })
}

export const register = (data) => {
  return request({
    url: '/users/register',
    method: 'post',
    data
  })
}

export const getUserById = (id) => {
  return request({
    url: `/users/${id}`,
    method: 'get'
  })
}

export const updateUser = (data) => {
  return request({
    url: '/users',
    method: 'put',
    data
  })
}

export const uploadImage = (file, imageType = 'general') => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('imageType', imageType)
  return request({
    url: '/upload/image',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export const getAchievements = () => {
  return request({
    url: '/achievements',
    method: 'get'
  })
}

export const getUserAchievements = (userId) => {
  return request({
    url: `/achievements/user/${userId}`,
    method: 'get'
  })
}

export const checkIn = (userId) => {
  return request({
    url: `/achievements/check-in/${userId}`,
    method: 'post'
  })
}

export const getCheckInStatus = (userId) => {
  return request({
    url: `/achievements/check-in/${userId}/status`,
    method: 'get'
  })
}

export const getCalendarMonthData = (userId, year, month) => {
  return request({
    url: `/calendar/month/${userId}`,
    method: 'get',
    params: { year, month }
  })
}

export const getCalendarDayDetail = (userId, date) => {
  return request({
    url: `/calendar/day/${userId}`,
    method: 'get',
    params: { date }
  })
}

export const createBakePlan = (data) => {
  return request({
    url: '/calendar/plan',
    method: 'post',
    data
  })
}

export const updateBakePlan = (data) => {
  return request({
    url: '/calendar/plan',
    method: 'put',
    data
  })
}

export const deleteBakePlan = (id, userId) => {
  return request({
    url: `/calendar/plan/${id}`,
    method: 'delete',
    params: { userId }
  })
}

export const getRecipeProgress = (userId, recipeId) => {
  return request({
    url: '/recipe-progress',
    method: 'get',
    params: { userId, recipeId }
  })
}

export const saveRecipeProgress = (data) => {
  return request({
    url: '/recipe-progress',
    method: 'post',
    data
  })
}

export const resetRecipeProgress = (userId, recipeId) => {
  return request({
    url: '/recipe-progress',
    method: 'delete',
    params: { userId, recipeId }
  })
}

export const getTrialReceipts = (params) => {
  return request({
    url: '/trial-receipts',
    method: 'get',
    params
  })
}

export const addTrialReceipt = (data) => {
  return request({
    url: '/trial-receipts',
    method: 'post',
    data
  })
}

export const getRecipesByIds = (ids) => {
  return request({
    url: '/recipes/batch',
    method: 'post',
    data: { ids }
  })
}

export const getIngredientAliasMap = () => {
  return request({
    url: '/ingredient-aliases/map',
    method: 'get'
  })
}

export const getRecipeBakeStats = (recipeId) => {
  return request({
    url: `/recipes/${recipeId}/bake-stats`,
    method: 'get'
  })
}

export const getCategoryBakeStats = () => {
  return request({
    url: '/recipes/category/bake-stats',
    method: 'get'
  })
}
