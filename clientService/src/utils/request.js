
import axios from 'axios'
import store from '@/store'
import router from '@/router'
import { getToken, getTokenType } from '@/utils/auth'

const service = axios.create({
  withCredentials: true,
  timeout: 1000*60*10
})

// 请求拦截
service.interceptors.request.use(
  config => {
    config.headers['Authorization'] = getTokenType() + ' ' + getToken()
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截
service.interceptors.response.use(
  response => {

    console.log('response==',response)
    if (response.data.code !== 200) {
      if (response.data.code === 401) {
        // 回退到登录页
        router.push('/login')
      } else if (
        response.data.code === '3005' ||
        response.data.code === '3007'
      ) {
        // 登录过期 回退到登录页
      } else {
        // 错误提示
        // Notify({ type: 'danger', message: response.data.message })
        // return response
      }
      return response
      // return Promise.reject(new Error(response.data.message || 'Error'))
    } else {
      return response
    }
  },
  error => {
    // 错误提示
    return Promise.reject(error)
  }
)

export default service
