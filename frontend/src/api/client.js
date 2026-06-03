import axios from 'axios'
import { getAuthToken } from '@/utils/auth'
import { sanitizeRequestParams } from '@/utils/id-utils'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
})

service.interceptors.request.use((config) => {
  const token = getAuthToken()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  if (config.params) {
    config.params = sanitizeRequestParams(config.params)
  }

  return config
})

service.interceptors.response.use(
  (response) => {
    if (response.config?.responseType === 'blob') {
      return response
    }

    const payload = response.data

    if (payload && typeof payload === 'object' && payload.success === false) {
      const businessError = new Error(payload.message || 'Request failed')
      businessError.response = {
        ...response,
        data: payload
      }
      return Promise.reject(businessError)
    }

    return payload
  },
  (error) => Promise.reject(error)
)

export default service
