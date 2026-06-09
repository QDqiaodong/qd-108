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

export const getHotRecipes = (limit = 10) => {
  return request({
    url: '/recipes/hot',
    method: 'get',
    params: { limit }
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

export const uploadImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
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
