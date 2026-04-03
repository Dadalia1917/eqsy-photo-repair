// 应用全局配置
// 微信小程序只允许 HTTPS 请求且域名须在开发平台白名单，localhost 在真机/预览环境完全不可用
// useCloudInDev=true：开发/生产均使用云端 HTTPS；若需本地联调请临时改为 false 并在微信开发者工具勾选「不校验合法域名」
const isProd = process.env.NODE_ENV === 'production'
const localBaseUrl = 'http://127.0.0.1:8080'
const cloudBaseUrl = 'https://ruoyi-backend.inmind-lab.com'
const useCloudInDev = false
const currentBaseUrl = isProd ? cloudBaseUrl : (useCloudInDev ? cloudBaseUrl : localBaseUrl)
export default {
  localBaseUrl,
  cloudBaseUrl,
  baseUrl: currentBaseUrl,
  staticUrl: currentBaseUrl,
  // 应用信息
  appInfo: {
    // 应用名称
    name: "e起守忆",
    // 应用版本
    version: "1.2.5",
    // 应用logo
    logo: "/static/logo.png",
    // 官方网站
    site_url: "https://eqsy.local",
    // 政策协议
    agreements: [{
        title: "隐私政策",
        url: "https://eqsy.local/protocol.html"
      },
      {
        title: "用户服务协议",
        url: "https://eqsy.local/protocol.html"
      }
    ]
  }
}
