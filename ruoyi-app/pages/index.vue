<template>
  <view class="home-page elder-large">
    <!-- 顶部极简介绍 -->
    <view class="header-card card-box">
      <view class="logo-box text-center" style="margin-bottom: 20rpx;">
        <image src="/static/logo.png" style="width: 120rpx; height: 120rpx; border-radius: 50%; box-shadow: 0 4rpx 10rpx rgba(0,0,0,0.1);"></image>
      </view>
      <view class="welcome-title text-center text-bold text-black" style="font-size: 52rpx;">
        欢迎来到 e起守忆
      </view>
      <view class="welcome-sub text-center" style="font-size: 34rpx; color: #5f7d79; margin-top: 20rpx;">
        帮您把老照片变清晰、让回忆动起来
      </view>
    </view>

    <!-- 动态数据统计栏 -->
    <view class="stats-bar text-center card-box" style="margin-top: 30rpx;">
      <text class="stats-text" style="font-size: 36rpx; color: #333; display: block;">累计已为社区修复影像</text>
      <view style="margin-top: 20rpx;">
        <text class="stats-num" style="font-size: 72rpx; font-weight: bold; color: #3eb49f;">{{ totalFixed }}</text> 
        <text style="font-size: 36rpx; color: #666; margin-left: 10rpx;">份</text>
      </view>
    </view>

    <!-- 引导操作区 -->
    <view class="action-container" style="padding: 40rpx 20rpx;">
      <button class="bg-blue round block shadow-blur" style="height: 110rpx; line-height: 110rpx; font-size: 42rpx; background: linear-gradient(90deg, #4dc4b0, #3eb49f); color: #fff; margin-bottom: 30rpx;" @click="goToWork">
        👉 点击这里开始修复照片
      </button>
      
      <button class="bg-grey round block" style="height: 110rpx; line-height: 110rpx; font-size: 42rpx; background: #ffffff; color: #5f7d79; border: 2rpx solid #d4e4e1;" @click="showGuide">
        📖 如何使用本小程序？
      </button>
    </view>
  </view>
</template>

<script>
import request from '@/utils/request'

export default {
  data() {
    return {
      totalFixed: 0
    }
  },
  onShow() {
    this.fetchTotalStats()
  },
  methods: {
    // 动态拉取后台真实数据
    async fetchTotalStats() {
      try {
        const res = await request({
          url: '/repair/task/stats/total',
          method: 'get'
        })
        if (res.code === 200 && res.data != null) {
          this.totalFixed = res.data
        }
      } catch (err) {
        console.error("加载统计数据失败", err)
      }
    },
    goToWork() {
      this.$tab.switchTab('/pages/work/index')
    },
    showGuide() {
      this.$modal.alert('亲爱的长辈您好：\\n\\n1. 点击上方的“开始修复照片”。\\n2. 在相册中选择需要修复的照片。\\n3. 选择功能并提交。\\n\\n系统会自动利用人工智能帮您修复如初！')
    }
  }
}
</script>

<style lang="scss">
page {
  background-color: #f5f6f7;
}
.home-page {
  padding: 30rpx 20rpx;
}
.card-box {
  background: #ffffff;
  border-radius: 24rpx;
  padding: 50rpx 40rpx;
  box-shadow: 0 8rpx 24rpx rgba(0,0,0,0.04);
}
</style>