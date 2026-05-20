<template>
  <view class="mine-container" :class="{ 'elder-large': elderLargeFont }">
    <!-- 顶部渐变信息栏 -->
    <view class="header-section">
      <view class="header-inner">
        <view class="avatar-row">
          <view v-if="!avatar" class="avatar-placeholder" @click="handleToLogin">
            <uni-icons type="contact-filled" size="40" color="#a8d4cc"></uni-icons>
          </view>
          <image
            v-if="avatar"
            @click="handleToAvatar"
            :src="avatar"
            class="avatar-img"
            mode="aspectFill"
          ></image>
          <view class="user-text">
            <view v-if="!name" class="login-tip" @click="handleToLogin">点击登录</view>
            <view v-if="name" class="username" @click="handleToInfo">{{ name }}</view>
            <view class="user-sub" @click="handleToInfo">
              <text>查看个人信息</text>
              <uni-icons type="right" size="12" color="rgba(255,255,255,0.7)"></uni-icons>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 功能卡片区 -->
    <view class="content-section">
      <!-- 修复记录快捷入口 -->
      <view class="quick-card" @tap="handleToHistory">
        <view class="quick-card-icon">📷</view>
        <view class="quick-card-body">
          <view class="quick-card-title">我的修复记录</view>
          <view class="quick-card-sub">查看已提交和修复好的照片</view>
        </view>
        <uni-icons type="right" size="18" color="#3eb49f"></uni-icons>
      </view>

      <!-- 服务提示卡片 -->
      <view class="care-card">
        <view class="care-title">📢 志愿服务温馨提示</view>
        <view class="care-text">建议家属协助录入清晰照片，并保持联系方式畅通，便于服务完成后及时查看。</view>
      </view>

      <!-- 菜单列表 -->
      <view class="menu-group">
        <view class="list-cell list-cell-arrow" @click="handleToEditInfo">
          <view class="menu-item-box">
            <view class="menu-icon-wrap icon-person"><uni-icons type="person" size="20" color="#3eb49f"></uni-icons></view>
            <text class="menu-label">编辑资料</text>
          </view>
          <uni-icons type="right" size="16" color="#ccc"></uni-icons>
        </view>
        <view class="list-cell list-cell-arrow" @click="handleToPwd">
          <view class="menu-item-box">
            <view class="menu-icon-wrap icon-lock"><uni-icons type="locked" size="20" color="#3eb49f"></uni-icons></view>
            <text class="menu-label">修改登录密码</text>
          </view>
          <uni-icons type="right" size="16" color="#ccc"></uni-icons>
        </view>
        <view class="list-cell list-cell-arrow" @click="handleHelp">
          <view class="menu-item-box">
            <view class="menu-icon-wrap icon-help"><uni-icons type="help" size="20" color="#3eb49f"></uni-icons></view>
            <text class="menu-label">常见问题</text>
          </view>
          <uni-icons type="right" size="16" color="#ccc"></uni-icons>
        </view>
        <view class="list-cell list-cell-arrow" @click="handleAbout">
          <view class="menu-item-box">
            <view class="menu-icon-wrap icon-heart"><uni-icons type="heart" size="20" color="#3eb49f"></uni-icons></view>
            <text class="menu-label">关于我们</text>
          </view>
          <uni-icons type="right" size="16" color="#ccc"></uni-icons>
        </view>
        <view class="list-cell list-cell-arrow" @click="handleToSetting">
          <view class="menu-item-box">
            <view class="menu-icon-wrap icon-gear"><uni-icons type="gear" size="20" color="#3eb49f"></uni-icons></view>
            <text class="menu-label">应用设置</text>
          </view>
          <uni-icons type="right" size="16" color="#ccc"></uni-icons>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
  import { useUserStore } from '@/store'
  import { computed , getCurrentInstance, ref } from "vue"
  import { onShow, onLoad } from '@dcloudio/uni-app'

  const { proxy } = getCurrentInstance()
  const name = useUserStore().name
  const avatar = computed(() => useUserStore().avatar)
  const windowHeightValue = ref(600)
  const windowHeight = computed(() => windowHeightValue.value)
  const elderLargeFont = ref(false)

  onLoad(() => {
    if (typeof uni.getWindowInfo === 'function') {
      const info = uni.getWindowInfo()
      windowHeightValue.value = (info.windowHeight || 650) - 50
      return
    }
    uni.getSystemInfo({
      success: (res) => {
        windowHeightValue.value = (res.windowHeight || 650) - 50
      }
    })
  })

  onShow(() => {
    elderLargeFont.value = !!uni.getStorageSync('eqsy_elder_large_font')
  })

  function handleToInfo() {
    proxy.$tab.navigateTo('/pages/mine/info/index')
  }

  function handleToEditInfo() {
    proxy.$tab.navigateTo('/pages/mine/info/edit')
  }

  function handleToSetting() {
    proxy.$tab.navigateTo('/pages/mine/setting/index')
  }

  function handleToPwd() {
    proxy.$tab.navigateTo('/pages/mine/pwd/index')
  }

  function handleToLogin() {
    proxy.$tab.reLaunch('/pages/login')
  }

  function handleToAvatar() {
    proxy.$tab.navigateTo('/pages/mine/avatar/index')
  }
      
  function handleHelp() {
    proxy.$tab.navigateTo('/pages/mine/help/index')
  }
      
  function handleAbout() {
    proxy.$tab.navigateTo('/pages/mine/about/index')
  }

  function handleToHistory() {
    proxy.$tab.navigateTo('/pages/work/history')
  }
</script>

<style lang="scss" scoped>
  page {
    background-color: #f0f7f5;
  }

  .mine-container {
    width: 100%;
    min-height: 100vh;
    background: #f0f7f5;
  }

  /* ====== 顶部 ====== */
  .header-section {
    background: linear-gradient(135deg, #1f7f6f 0%, #3eb49f 100%);
    padding: 60rpx 40rpx 80rpx;
    color: #fff;
  }

  .header-inner {
    display: flex;
    flex-direction: column;
    gap: 0;
  }

  .avatar-row {
    display: flex;
    align-items: center;
    gap: 30rpx;
  }

  .avatar-placeholder {
    width: 120rpx;
    height: 120rpx;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.2);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .avatar-img {
    width: 120rpx;
    height: 120rpx;
    border-radius: 50%;
    border: 4rpx solid rgba(255, 255, 255, 0.5);
    flex-shrink: 0;
  }

  .user-text {
    flex: 1;
  }

  .login-tip {
    font-size: 36rpx;
    color: rgba(255, 255, 255, 0.9);
  }

  .username {
    font-size: 40rpx;
    font-weight: bold;
    color: #fff;
    margin-bottom: 8rpx;
  }

  .user-sub {
    font-size: 26rpx;
    color: rgba(255, 255, 255, 0.7);
    display: flex;
    align-items: center;
    gap: 4rpx;
  }

  /* ====== 内容区 ====== */
  .content-section {
    position: relative;
    top: -40rpx;
    padding: 0 30rpx;
  }

  /* 修复记录快捷卡片 */
  .quick-card {
    background: #fff;
    border-radius: 20rpx;
    padding: 36rpx 40rpx;
    margin-bottom: 24rpx;
    box-shadow: 0 4rpx 20rpx rgba(62, 180, 159, 0.12);
    display: flex;
    align-items: center;
    gap: 28rpx;
    border-left: 8rpx solid #3eb49f;
  }

  .quick-card-icon {
    font-size: 56rpx;
    flex-shrink: 0;
  }

  .quick-card-body {
    flex: 1;
  }

  .quick-card-title {
    font-size: 34rpx;
    font-weight: bold;
    color: #2b3d3a;
    margin-bottom: 6rpx;
  }

  .quick-card-sub {
    font-size: 26rpx;
    color: #5f7d79;
  }

  /* 提示卡片 */
  .care-card {
    background: #e8f5f2;
    border-radius: 16rpx;
    padding: 30rpx 36rpx;
    margin-bottom: 24rpx;

    .care-title {
      font-size: 30rpx;
      font-weight: 600;
      color: #1f7f6f;
      margin-bottom: 12rpx;
    }

    .care-text {
      font-size: 28rpx;
      line-height: 1.7;
      color: #506a67;
    }
  }

  /* 菜单组 */
  .menu-group {
    background: #fff;
    border-radius: 20rpx;
    overflow: hidden;
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
  }

  .list-cell {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 36rpx 40rpx;
    border-bottom: 1rpx solid #f0f0f0;

    &:last-child {
      border-bottom: none;
    }
  }

  .menu-item-box {
    display: flex;
    align-items: center;
    gap: 24rpx;
  }

  .menu-icon-wrap {
    width: 64rpx;
    height: 64rpx;
    border-radius: 16rpx;
    background: #e8f5f2;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .menu-label {
    font-size: 32rpx;
    color: #2b3d3a;
  }

  /* ====== 大字模式 ====== */
  .mine-container.elder-large {
    .username,
    .login-tip {
      font-size: 44rpx;
    }

    .quick-card-title,
    .menu-label {
      font-size: 38rpx;
    }

    .quick-card-sub,
    .care-text {
      font-size: 32rpx;
    }
  }
</style>
