<script setup>
  import config from './config'
  import { getToken } from '@/utils/auth'
  import { useConfigStore } from '@/store'
  import { onLaunch } from '@dcloudio/uni-app'

  onLaunch(() => {
    initApp()
  })

  // 初始化应用
  function initApp() {
    // 初始化应用配置
    initConfig()
    // 检查用户登录状态
    //#ifdef H5
    checkLogin()
    //#endif
  }

  function initConfig() {
    useConfigStore().setConfig(config)
  }

  //#ifdef H5
  function checkLogin() {
    if (!getToken()) {
      uni.reLaunch({ url: '/pages/login' })
    }
  }
  //#endif
</script>

<style lang="scss">
  @import '@/static/scss/index.scss'
</style>
