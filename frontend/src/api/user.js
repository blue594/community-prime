import request from '../utils/request'

// 发送验证码
export function sendCode(phone) {
  return request({
    url: '/user/code',
    method: 'post',
    params: { phone }
  })
}

// 登录
export function login(data) {
  return request({
    url: '/user/login',
    method: 'post',
    data
  })
}

// 获取用户信息
export function getUserInfo() {
  return request({
    url: '/user/me',
    method: 'get'
  })
}

// 用户注册
export function register(phone, password) {
  return request({
    url: '/user/register',
    method: 'post',
    params: { phone, password }
  })
}

// 更新用户头像
export function updateIcon(icon) {
  return request({
    url: '/user/update-icon',
    method: 'post',
    data: { icon }
  })
}

// 更新用户昵称
export function updateNickName(nickName) {
  return request({
    url: '/user/update-nickname',
    method: 'post',
    data: { nickName }
  })
}
