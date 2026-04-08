<template>
  <view class="mine-container" :class="{ 'elder-large': elderLargeFont }" :style="{height: `${windowHeight}px`}">
    <!--顶部个人信息栏-->
    <view class="header-section">
      <view class="flex padding justify-between">
        <view class="flex align-center">
          <view v-if="!avatar" class="cu-avatar xl round bg-white">
            <uni-icons type="contact-filled" size="34" color="#7f8f8d"></uni-icons>
          </view>
          <image v-if="avatar" @click="handleToAvatar" :src="avatar" class="cu-avatar xl round" mode="widthFix">
          </image>
          <view v-if="!name" @click="handleToLogin" class="login-tip">
            点击登录
          </view>
          <view v-if="name" @click="handleToInfo" class="user-info">
            <view class="u_title">
              {{ name }}
            </view>
          </view>
        </view>
        <view @click="handleToInfo" class="flex align-center">
          <text>个人信息</text>
          <uni-icons type="right" size="16" color="#e6fffb"></uni-icons>
        </view>
      </view>
    </view>

    <view class="content-section">
      <view class="care-card">
        <view class="care-title">社区志愿服务提醒</view>
        <view class="care-text">建议家属协助录入清晰照片，并保持联系方式畅通，便于服务完成后及时查看。</view>
      </view>

      <view class="menu-list">
        <view class="list-cell list-cell-arrow" @click="handleToEditInfo">
          <view class="menu-item-box">
            <uni-icons type="person" size="20" color="#1f7f6f"></uni-icons>
            <view>编辑资料</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow" @click="handleToPwd">
          <view class="menu-item-box">
            <uni-icons type="locked" size="20" color="#1f7f6f"></uni-icons>
            <view>修改登录密码</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow" @click="handleHelp">
          <view class="menu-item-box">
            <uni-icons type="help" size="20" color="#1f7f6f"></uni-icons>
            <view>常见问题</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow" @click="handleAbout">
          <view class="menu-item-box">
            <uni-icons type="heart" size="20" color="#1f7f6f"></uni-icons>
            <view>关于我们</view>
          </view>
        </view>
        <view class="list-cell list-cell-arrow" @click="handleToSetting">
          <view class="menu-item-box">
            <uni-icons type="gear" size="20" color="#1f7f6f"></uni-icons>
            <view>应用设置</view>
          </view>
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
</script>

<style lang="scss" scoped>
  page {
    background-color: #f5f6f7;
  }

  .mine-container {
    width: 100%;
    height: 100%;


    .header-section {
      padding: 15px 15px 45px 15px;
      background-color: #1f7f6f;
      color: white;

      .login-tip {
        font-size: 18px;
        margin-left: 10px;
      }

      .cu-avatar {
        border: 2px solid #eaeaea;

        .icon {
          font-size: 40px;
        }
      }

      .user-info {
        margin-left: 15px;

        .u_title {
          font-size: 18px;
          line-height: 30px;
        }
      }
    }

    .content-section {
      position: relative;
      top: -50px;

      .care-card {
        margin: 15px 15px;
        padding: 16px;
        border-radius: 8px;
        background-color: white;
        box-shadow: 0 6rpx 16rpx rgba(38, 77, 69, 0.06);

        .care-title {
          font-size: 16px;
          font-weight: 600;
          color: #21433f;
          margin-bottom: 6px;
        }

        .care-text {
          font-size: 14px;
          line-height: 1.6;
          color: #506a67;
        }
      }

      .voice-tip {
        margin: 12px 15px 0;
        color: #3f6f68;
        font-size: 13px;
      }
    }

    &.elder-large {
      .header-section {
        .login-tip,
        .user-info .u_title,
        text {
          font-size: 20px;
        }
      }

      .content-section {
        .care-title {
          font-size: 18px;
        }

        .care-text,
        .menu-item-box view {
          font-size: 16px;
        }
      }
    }
  }
</style>
