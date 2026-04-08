import request from '@/utils/request'

// 鐧诲綍鏂规硶
export function login(username, password, code, uuid) {
  const data = {
    username,
    password,
    code,
    uuid
  }
  return request({
    'url': '/login',
    headers: {
      isToken: false
    },
    'method': 'post',
    'data': data
  })
}

// 娉ㄥ唽鏂规硶
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

// 鑾峰彇鐢ㄦ埛璇︾粏淇℃伅
export function getInfo() {
  return request({
    'url': '/getInfo',
    'method': 'get'
  })
}

// 閫€鍑烘柟娉?
export function logout() {
  return request({
    'url': '/logout',
    'method': 'post'
  })
}

// 鑾峰彇楠岃瘉鐮?
export function getCodeImg() {
  return request({
    'url': '/captchaImage',
    headers: {
      isToken: false
    },
    method: 'get',
    timeout: 20000
  })
}

// 发送短信验证码 (模拟)
export function sendSmsCodeApi(phone) {
  return request({
    url: '/sendSmsCode',
    headers: { isToken: false },
    method: 'post',
    data: { phone }
  })
}

// 短信登录
export function smsLoginApi(phone, smsCode) {
  return request({
    url: '/smsLogin',
    headers: { isToken: false },
    method: 'post',
    data: { phone, smsCode }
  })
}

// 微信一键登录
export function wxLoginApi(code, extra = {}) {
  return request({
    url: '/wxLogin',
    headers: { isToken: false },
    method: 'post',
    data: {
      code,
      ...extra
    }
  })
}
