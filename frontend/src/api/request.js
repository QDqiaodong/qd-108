import axios from 'axios'
import { ElMessage } from 'element-plus'

const RETRYABLE_STATUS_CODES = new Set([502, 503, 504])
const MAX_GET_RETRIES = 2
const RETRY_DELAY_MS = 800

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms))

request.interceptors.request.use(
  config => {
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  async error => {
    const config = error.config
    const method = config?.method?.toLowerCase()
    const status = error.response?.status
    const shouldRetry =
      config &&
      method === 'get' &&
      (RETRYABLE_STATUS_CODES.has(status) || !status)

    if (shouldRetry) {
      config.__retryCount = config.__retryCount || 0
      if (config.__retryCount < MAX_GET_RETRIES) {
        config.__retryCount += 1
        await sleep(RETRY_DELAY_MS * config.__retryCount)
        return request(config)
      }
    }

    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
