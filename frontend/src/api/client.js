import axios from 'axios'
import { getAuthToken } from '@/utils/auth'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
})

service.interceptors.request.use((config) => {
  const token = getAuthToken()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

service.interceptors.response.use(
  (response) => response.data,
  (error) => Promise.reject(error)
)

export default service
