<template>
  <view class="home-page elder-large">
    <!-- 顶部 Hero 区 -->
    <view class="hero-section">
      <view class="hero-bg-circle hero-bg-circle--1"></view>
      <view class="hero-bg-circle hero-bg-circle--2"></view>
      <view class="hero-content">
        <image src="/static/logo.png" class="hero-logo" mode="aspectFill"></image>
        <text class="hero-title">e起守忆</text>
        <text class="hero-sub">让科技有温度，让记忆有归处</text>
      </view>
    </view>

    <!-- 统计数据卡片 -->
    <view class="stats-card">
      <view class="stats-inner">
        <text class="stats-label">累计已为社区修复影像</text>
        <view class="stats-num-row">
          <text class="stats-num">{{ totalFixed }}</text>
          <text class="stats-unit">份</text>
        </view>
        <text class="stats-tips">由热心志愿者用心完成</text>
      </view>
    </view>

    <!-- 主要操作 -->
    <view class="action-section">
      <view class="action-btn-primary" @tap="goToWork">
        <text class="action-icon">📷</text>
        <view class="action-text">
          <text class="action-main">开始修复照片</text>
          <text class="action-desc">上传老照片，志愿者帮您焕然一新</text>
        </view>
        <uni-icons type="right" size="22" color="#fff"></uni-icons>
      </view>

      <view class="action-btn-secondary" @tap="goToHistory">
        <text class="action-icon action-icon--sm">📋</text>
        <view class="action-text">
          <text class="action-main action-main--dark">查看修复记录</text>
          <text class="action-desc action-desc--dark">查看已提交和完成的任务</text>
        </view>
        <uni-icons type="right" size="22" color="#3eb49f"></uni-icons>
      </view>
    </view>

    <!-- 使用指南 -->
    <view class="guide-card">
      <view class="guide-title">📖 使用步骤</view>
      <view class="guide-steps">
        <view class="guide-step">
          <view class="step-num">1</view>
          <text class="step-text">点击"开始修复"，从相册选取照片</text>
        </view>
        <view class="guide-step">
          <view class="step-num">2</view>
          <text class="step-text">留言填写修复要求，提交给志愿者</text>
        </view>
        <view class="guide-step">
          <view class="step-num">3</view>
          <text class="step-text">在"修复记录"查看进度并保存结果</text>
        </view>
      </view>
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
        console.error('加载统计数据失败', err)
      }
    },
    goToWork() {
      this.$tab.switchTab('/pages/work/index')
    },
    goToHistory() {
      this.$tab.navigateTo('/pages/work/history')
    }
  }
}
</script>

<style lang="scss">
page {
  background-color: #f0f7f5;
}

.home-page {
  padding-bottom: 80rpx;
}

/* ====== Hero ====== */
.hero-section {
  position: relative;
  background: linear-gradient(135deg, #1f7f6f 0%, #3eb49f 100%);
  padding: 80rpx 40rpx 100rpx;
  overflow: hidden;
  text-align: center;
}

.hero-bg-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}
.hero-bg-circle--1 {
  width: 400rpx;
  height: 400rpx;
  top: -100rpx;
  right: -80rpx;
}
.hero-bg-circle--2 {
  width: 300rpx;
  height: 300rpx;
  bottom: -80rpx;
  left: -60rpx;
}

.hero-content {
  position: relative;
  z-index: 1;
}

.hero-logo {
  width: 140rpx;
  height: 140rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.5);
  margin-bottom: 28rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.15);
}

.hero-title {
  display: block;
  font-size: 64rpx;
  font-weight: bold;
  color: #fff;
  margin-bottom: 16rpx;
  letter-spacing: 4rpx;
}

.hero-sub {
  display: block;
  font-size: 30rpx;
  color: rgba(255, 255, 255, 0.85);
}

/* ====== 统计卡片 ====== */
.stats-card {
  margin: -40rpx 30rpx 30rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx;
  box-shadow: 0 8rpx 32rpx rgba(62, 180, 159, 0.15);
  text-align: center;
}

.stats-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stats-label {
  font-size: 32rpx;
  color: #5f7d79;
  display: block;
}

.stats-num-row {
  display: flex;
  align-items: baseline;
  gap: 10rpx;
  margin: 16rpx 0 8rpx;
}

.stats-num {
  font-size: 88rpx;
  font-weight: bold;
  color: #3eb49f;
  line-height: 1;
}

.stats-unit {
  font-size: 36rpx;
  color: #666;
}

.stats-tips {
  font-size: 26rpx;
  color: #aaa;
  display: block;
}

/* ====== 操作区 ====== */
.action-section {
  padding: 0 30rpx;
  margin-bottom: 30rpx;
}

.action-btn-primary {
  background: linear-gradient(90deg, #4dc4b0, #3eb49f);
  border-radius: 20rpx;
  padding: 36rpx 40rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 10rpx 30rpx rgba(62, 180, 159, 0.35);
}

.action-btn-secondary {
  background: #fff;
  border: 2rpx solid #c8e8e3;
  border-radius: 20rpx;
  padding: 36rpx 40rpx;
  display: flex;
  align-items: center;
  gap: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.action-icon {
  font-size: 52rpx;
  flex-shrink: 0;
}
.action-icon--sm {
  font-size: 44rpx;
}

.action-text {
  flex: 1;
}

.action-main {
  display: block;
  font-size: 40rpx;
  font-weight: bold;
  color: #fff;
  margin-bottom: 8rpx;
}
.action-main--dark {
  color: #2b3d3a;
}

.action-desc {
  display: block;
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.85);
}
.action-desc--dark {
  color: #5f7d79;
}

/* ====== 使用指南 ====== */
.guide-card {
  margin: 0 30rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 40rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.guide-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #2b3d3a;
  margin-bottom: 30rpx;
}

.guide-steps {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.guide-step {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
}

.step-num {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: #3eb49f;
  color: #fff;
  font-size: 26rpx;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2rpx;
}

.step-text {
  font-size: 32rpx;
  color: #506a67;
  line-height: 1.7;
}
</style>