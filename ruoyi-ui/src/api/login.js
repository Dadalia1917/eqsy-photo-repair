import request from '@/utils/request'

// 登录方法
export function login(username, password, code, uuid) {
  const data = {
    username,
    password,
    code,
    uuid
  }
  return request({
    url: '/login',
    headers: {
      isToken: false,
      repeatSubmit: false
    },
    method: 'post',
    data: data
  })
}

// 注册方法
export function register(data) {
  return request({
    url: '/register',
    headers: {
      isToken: false
    },
    method: 'post',
    data: data
  })
}

// 发送短信验证码
export function sendSmsCode(phone) {
  return request({
    url: '/sendSmsCode',
    headers: {
      isToken: false
    },
    method: 'post',
    data: { phone }
  })
}

// 短信验证码登录
export function smsLogin(phone, smsCode) {
  return request({
    url: '/smsLogin',
    headers: {
      isToken: false
    },
    method: 'post',
    data: { phone, smsCode }
  })
}

// 获取用户详细信息
export function getInfo() {
  return request({
    url: '/getInfo',
    method: 'get'
  })
}

// 退出方法
export function logout() {
  return request({
    url: '/logout',
    method: 'post'
  })
}

// 获取验证码
export function getCodeImg() {
  return request({
    url: '/captchaImage',
    headers: {
      isToken: false
    },
    method: 'get',
    timeout: 20000
  })
}