import Vue from 'vue'
import Vuex from 'vuex'
import Cookies from 'js-cookie'
import { getUserInfo } from '@/api/user'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    token: Cookies.get('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || '{}')
  },
  getters: {
    isLoggedIn: state => !!state.token,
    userInfo: state => state.userInfo
  },
  mutations: {
    SET_TOKEN(state, token) {
      state.token = token
      Cookies.set('token', token, { expires: 1 })
    },
    SET_USER_INFO(state, userInfo) {
      state.userInfo = userInfo
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
    },
    CLEAR_USER(state) {
      state.token = ''
      state.userInfo = {}
      Cookies.remove('token')
      localStorage.removeItem('userInfo')
    }
  },
  actions: {
    login({ commit }, data) {
      commit('SET_TOKEN', data.token)
      commit('SET_USER_INFO', data)
    },
    logout({ commit }) {
      commit('CLEAR_USER')
    },
    async getUserInfo({ commit }) {
      try {
        const data = await getUserInfo()
        // request.js会解包响应，直接返回data
        if (data) {
          commit('SET_USER_INFO', data)
        }
      } catch (error) {
        console.error('获取用户信息失败:', error)
      }
    }
  }
})
