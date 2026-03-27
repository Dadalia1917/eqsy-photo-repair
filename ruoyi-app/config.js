// 应用全局配置
// 本地开发：http://127.0.0.1:8080
// 生产环境：改为服务器公网 IP 或域名，例如 https://api.eqsy.com
const isProd = process.env.NODE_ENV === 'production'
export default {
  baseUrl: isProd ? 'https://ruoyi.inmind-lab.com/prod-api' : 'http://127.0.0.1:8080',
  // 应用信息
  appInfo: {
    // 应用名称
    name: "e起守忆",
    // 应用版本
    version: "1.2.0",
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
