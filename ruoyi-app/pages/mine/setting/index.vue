<template>
  <view class="setting-container" :style="{height: `${windowHeight}px`}">
    <view class="banner-card">
      <view class="banner-title">长辈模式设置</view>
      <view class="banner-sub">只保留常用功能，降低使用难度</view>
    </view>

    <view class="menu-list">
      <view class="list-cell">
        <view class="menu-item-box">
          <view>
            <view class="setting-title">字体放大</view>
            <view class="setting-desc">首页和我的页面使用更大的字号</view>
          </view>
          <switch :checked="elderLargeFont" color="#1f7f6f" @change="handleFontSwitch" />
        </view>
      </view>


    </view>

    <view class="cu-list menu">
      <view class="cu-item item-box">
        <view class="content text-center" @click="handleLogout">
          <text class="text-black">退出登录</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
  import { useUserStore } from '@/store'
  import { ref, computed , getCurrentInstance, onMounted } from "vue"

  const { proxy } = getCurrentInstance()
  const windowHeightValue = ref(600)
  const windowHeight = computed(() => windowHeightValue.value)
  const elderLargeFont = ref(false)

  const LARGE_FONT_KEY = 'eqsy_elder_large_font'

  onMounted(() => {
    if (typeof uni.getWindowInfo === 'function') {
      const info = uni.getWindowInfo()
      windowHeightValue.value = (info.windowHeight || 650) - 50
    } else {
      uni.getSystemInfo({
        success: (res) => {
          windowHeightValue.value = (res.windowHeight || 650) - 50
        }
      })
    }
    elderLargeFont.value = !!uni.getStorageSync(LARGE_FONT_KEY)
  })

  function handleFontSwitch(e) {
    elderLargeFont.value = e.detail.value
    uni.setStorageSync(LARGE_FONT_KEY, elderLargeFont.value)
    proxy.$modal.showToast(elderLargeFont.value ? '已开启字体放大' : '已关闭字体放大')
  }

  function handleLogout() {
    proxy.$modal.confirm('确定注销并退出系统吗？').then(() => {
      useUserStore().logOut().then(() => {}).finally(()=>{
        proxy.$tab.reLaunch('/pages/index')
      })
    })
  }
</script>

<style lang="scss" scoped>
  .setting-container {
    background-color: #f8f8f8;
    padding: 20rpx;
  }

  .banner-card {
    background: linear-gradient(135deg, #e8f5f0, #f6fbf8);
    border-radius: 16rpx;
    padding: 24rpx;
    margin-bottom: 20rpx;
  }

  .banner-title {
    font-size: 34rpx;
    font-weight: 700;
    color: #1f4f49;
  }

  .banner-sub {
    margin-top: 8rpx;
    font-size: 25rpx;
    color: #587571;
  }

  .setting-title {
    font-size: 30rpx;
    color: #243a37;
  }

  .setting-desc {
    font-size: 24rpx;
    color: #7a8f8b;
    margin-top: 4rpx;
  }

  .item-box {
    background-color: #FFFFFF;
    margin: 30rpx 0;
    display: flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;
    padding: 10rpx;
    border-radius: 8rpx;
    color: #303133;
    font-size: 32rpx;
  }
</style>
