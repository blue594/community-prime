import axios from 'axios'
import { Message, Loading } from 'element-ui'
import store from '../store'
import router from '../router'

// 创建axios实例
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API || '/api',
  timeout: 10000
})

let loadingInstance = null

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 显示加载动画
    loadingInstance = Loading.service({
      lock: true,
      text: '加载中...',
      spinner: 'el-icon-loading',
      background: 'rgba(0, 0, 0, 0.7)'
    })

    // 添加token - 优先使用用户token，如果没有则尝试管理员token
    if (store.getters.isLoggedIn) {
      config.headers['authorization'] = store.state.token
    } else {
      // 检查是否是管理员请求
      const adminToken = localStorage.getItem('adminToken')
      if (adminToken && config.url && config.url.startsWith('/admin/')) {
        config.headers['authorization'] = adminToken
      }
    }

    return config
  },
  error => {
    if (loadingInstance) loadingInstance.close()
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    if (loadingInstance) loadingInstance.close()

    const res = response.data

    // 请求成功
    if (res.success) {
      return res.data
    }

    // 业务错误
    Message.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  error => {
    if (loadingInstance) loadingInstance.close()

    const { response } = error

    if (response) {
      switch (response.status) {
        case 401:
          Message.error('登录已过期，请重新登录')
          store.dispatch('logout')
          router.push('/login')
          break
        case 403:
          Message.error('没有权限访问')
          break
        case 404:
          Message.error('请求的资源不存在')
          break
        case 500:
          Message.error('服务器内部错误')
          break
        default:
          Message.error(response.data?.msg || '请求失败')
      }
    } else {
      Message.error('网络错误，请检查网络连接')
    }

    return Promise.reject(error)
  }
)

export default service
