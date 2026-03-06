import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import './styles/common.scss'
import axios from 'axios'

Vue.use(ElementUI)

Vue.config.productionTip = false

// 配置axios
Vue.prototype.$http = axios
axios.defaults.baseURL = '/api'
axios.defaults.timeout = 10000

// 请求拦截器
axios.interceptors.request.use(
  config => {
    // 管理员请求添加admin-token
    if (config.url && config.url.startsWith('/admin')) {
      const adminToken = localStorage.getItem('adminToken')
      if (adminToken) {
        config.headers['admin-token'] = adminToken
      }
    } else {
      // 普通用户请求添加token
      const token = store.state.token
      if (token) {
        config.headers['authorization'] = token
      }
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
axios.interceptors.response.use(
  response => {
    return response
  },
  error => {
    if (error.response && error.response.status === 401) {
      // 管理员未登录
      if (error.config.url && error.config.url.startsWith('/admin')) {
        localStorage.removeItem('adminToken')
        localStorage.removeItem('adminInfo')
        // 避免重复跳转
        if (router.currentRoute.path !== '/admin/login') {
          router.push('/admin/login')
        }
      } else {
        // 普通用户未登录
        store.dispatch('logout')
        if (router.currentRoute.path !== '/login') {
          router.push('/login')
        }
      }
    }
    return Promise.reject(error)
  }
)

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
