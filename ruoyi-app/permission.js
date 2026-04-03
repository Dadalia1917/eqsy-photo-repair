import { getToken } from '@/utils/auth'

// 登录页面
const loginPage = "/pages/login"
  
// 页面白名单
const whiteList = [
  '/pages/login', '/pages/register', '/pages/common/webview/index'
]

// 检查地址白名单
function checkWhite(url) {
  const path = url.split('?')[0]
  return whiteList.indexOf(path) !== -1
}

// 页面跳转验证拦截器
// 不拦截 reLaunch，避免在启动阶段递归调用导致 LifeCycle.load fail
let isNavigating = false
let list = ["navigateTo", "redirectTo", "switchTab"]
list.forEach(item => {
  uni.addInterceptor(item, {
    invoke(to) {
      if (isNavigating) return true
      if (getToken()) {
        if (to.url === loginPage) {
          isNavigating = true
          uni.reLaunch({ url: "/" })
          setTimeout(() => { isNavigating = false }, 500)
          return false
        }
        return true
      } else {
        if (checkWhite(to.url)) {
          return true
        }
        isNavigating = true
        uni.reLaunch({ url: loginPage })
        setTimeout(() => { isNavigating = false }, 500)
        return false
      }
    },
    fail(err) {
      console.log(err)
    }
  })
})
