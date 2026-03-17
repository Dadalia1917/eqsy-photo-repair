<template>
  <view class="about-container" :class="{ 'elder-large': elderLargeFont }">
    <view class="header-section text-center">
      <image style="width: 150rpx;height: 150rpx;" src="/static/logo200.png" mode="widthFix">
      </image>
      <uni-title type="h2" title="e起守忆"></uni-title>
      <view class="slogan">学校与社区联合的老人影像关怀服务</view>
    </view>

    <view class="content-section">
      <view class="intro-card">
        <view class="intro-title">平台简介</view>
        <view class="intro-text">“e起守忆”聚焦老照片修复与记忆传承。社区居民可上传照片，选择 AI 快速修复或学生志愿者人工精修，修复完成后直接回传到小程序。</view>
      </view>

      <view class="menu-list">
        <view class="list-cell list-cell-arrow">
          <view class="menu-item-box">
            <view>版本信息</view>
            <view class="text-right">v{{version}}</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow">
          <view class="menu-item-box">
            <view>社区服务邮箱</view>
            <view class="text-right">service@eqsy.local</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow">
          <view class="menu-item-box">
            <view>社区服务热线</view>
            <view class="text-right">400-123-4567</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow">
          <view class="menu-item-box">
            <view>公司网站</view>
            <view class="text-right">
              <uni-link :href="url" :text="url" showUnderLine="false"></uni-link>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="copyright">
      <view>Copyright &copy; 2026 e起守忆 All Rights Reserved.</view>
    </view>
  </view>
</template>

<script setup>
  import { ref } from 'vue'
  import { onShow } from '@dcloudio/uni-app'
  import { useConfigStore } from '@/store'

  const url = useConfigStore().config.appInfo.site_url
  const version = useConfigStore().config.appInfo.version
  const elderLargeFont = ref(false)
  const voiceAssist = ref(false)

  onShow(() => {
    elderLargeFont.value = !!uni.getStorageSync('eqsy_elder_large_font')
    voiceAssist.value = !!uni.getStorageSync('eqsy_voice_assist')
    // #ifdef H5
    if (voiceAssist.value && window.speechSynthesis) {
      const utter = new SpeechSynthesisUtterance('这里是关于我们页面，您可以查看平台介绍和社区联系方式。')
      utter.lang = 'zh-CN'
      window.speechSynthesis.cancel()
      window.speechSynthesis.speak(utter)
    }
    // #endif
  })
</script>

<style lang="scss" scoped>
  page {
    background-color: #f8f8f8;
  }

  .copyright {
    margin-top: 30rpx;
    text-align: center;
    line-height: 60rpx;
    color: #999;
  }

  .header-section {
    display: flex;
    padding: 30rpx 0 0;
    flex-direction: column;
    align-items: center;
  }

  .slogan {
    margin-top: 8rpx;
    color: #4d6662;
    font-size: 26rpx;
  }

  .intro-card {
    margin: 20rpx 0;
    background: #ffffff;
    border-radius: 16rpx;
    padding: 22rpx;
    box-shadow: 0 0 10rpx rgba(193, 193, 193, 0.2);
  }

  .intro-title {
    font-size: 30rpx;
    font-weight: 700;
    color: #1f4f49;
    margin-bottom: 8rpx;
  }

  .intro-text {
    color: #516b67;
    line-height: 1.8;
    font-size: 27rpx;
  }

  .about-container.elder-large {
    .slogan,
    .intro-text,
    .menu-item-box view,
    .copyright {
      font-size: 32rpx;
    }

    .intro-title {
      font-size: 36rpx;
    }
  }
</style>
