import { useUserStore } from '@/store'
import config from '@/config'
import { getToken } from '@/utils/auth'
import errorCode from '@/utils/errorCode'
import { toast, showConfirm, tansParams } from '@/utils/common'

let timeout = 10000
const baseUrl = config.baseUrl

export default function upload(config) {
  // 是否需要设置 token
  const isToken = (config.headers || {}).isToken === false
  config.header = config.header || {}
  if (getToken() && !isToken) {
    config.header['Authorization'] = 'Bearer ' + getToken()
  }
  // get请求映射params参数
  if (config.params) {
    let url = config.url + '?' + tansParams(config.params)
    url = url.slice(0, -1)
    config.url = url
  }
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      timeout: config.timeout || timeout,
      url: baseUrl + config.url,
      filePath: config.filePath,
      name: config.name || 'file',
      header: config.header,
      formData: config.formData,
      success: (res) => {
        let result = {}
        try {
          result = JSON.parse(res.data)
        } catch (e) {
          const parseMsg = '上传响应解析失败，请稍后重试'
          toast(parseMsg)
          reject(parseMsg)
          return
        }
        const code = result.code || 200
        const msg = errorCode[code] || result.msg || errorCode['default']
        if (code === 200) {
          resolve(result)
        } else if (code == 401) {
          showConfirm("登录状态已过期，您可以继续留在该页面，或者重新登录?").then(res => {
            if (res.confirm) {
              useUserStore().logOut().then(res => {
                uni.reLaunch({ url: '/pages/login/login' })
              })
            }
          })
          reject('无效的会话，或者会话已过期，请重新登录。')
        } else if (code === 500) {
          toast(msg)
          reject(msg)
        } else if (code !== 200) {
          toast(msg)
          reject(msg)
        }
      },
      fail: (error) => {
        const rawMsg = (error && (error.message || error.errMsg)) || ''
        let message
        if (!rawMsg || rawMsg === 'Network Error') {
          message = '后端接口连接异常'
        } else if (rawMsg.includes('timeout')) {
          message = '系统接口请求超时'
        } else if (rawMsg.includes('Request failed with status code')) {
          message = '系统接口' + rawMsg.substr(rawMsg.length - 3) + '异常'
        } else {
          message = '后端接口连接异常'
        }
        toast(message)
        reject(error)
      }
    })
  })
}


