<template>
  <view class="normal-login-container">
    <view class="logo-content align-center justify-center flex">
      <image class="logo" src="/static/logo.png" mode="widthFix"></image>
      <view class="title-box">
        <text class="title">e起守忆</text>
        <text class="sub-title">社区志愿服务平台</text>
      </view>
    </view>

    <view class="login-form-content">
      <view class="login-type-switch" style="display:flex;justify-content:center;margin-bottom:20px;">
        <text class="active" style="margin-right:10px;font-size:16px;">账号密码登录</text>
        <text class="divider">|</text>
        <text class="wx-entry" @click="handleWxLogin" style="margin-left:10px;font-size:16px;">微信一键登录</text>
      </view>

      <view class="input-item flex align-center">
        <uni-icons class="icon" type="person" size="20" color="#5f7d79"></uni-icons>
        <input v-model="loginForm.username" class="input" type="text" placeholder="请输入账号" maxlength="30" />
      </view>
      <view class="input-item flex align-center">
        <uni-icons class="icon" type="locked" size="20" color="#5f7d79"></uni-icons>
        <input v-model="loginForm.password" type="password" class="input" placeholder="请输入密码" maxlength="20" />
      </view>
      <view class="input-item code-item flex align-center" v-if="captchaEnabled">
        <uni-icons class="icon" type="help" size="20" color="#5f7d79"></uni-icons>
        <input v-model="loginForm.code" type="number" class="input" placeholder="请输入验证码" maxlength="4" />
        <image :src="codeUrl" @click="getCode" class="login-code-img"></image>
      </view>

      <view class="agreement-row">
        <view class="agreement-check" @click="agreementChecked = !agreementChecked">
          <uni-icons :type="agreementChecked ? 'checkbox-filled' : 'circle'" size="18" :color="agreementChecked ? '#3eb49f' : '#b7c3c1'"></uni-icons>
          <text class="agreement-text">我已阅读并同意</text>
        </view>
        <text class="agreement-link" @click.stop="openAgreement('privacy')">《隐私政策》</text>
        <text class="agreement-text">和</text>
        <text class="agreement-link" @click.stop="openAgreement('service')">《用户服务协议》</text>
      </view>

      <view class="action-btn">
        <button @click="handleLogin" class="login-btn round">账号密码登录</button>
      </view>
      <view class="reg text-center" v-if="register">
        <text class="text-grey1">还没有账号？</text>
        <text @click="handleUserRegister" class="text-blue">立即注册</text>
      </view>
    </view>

    <view class="wx-auth-mask" v-if="wxAuthVisible" @click="closeWxAuthDialog"></view>
    <view class="wx-auth-sheet" v-if="wxAuthVisible">
      <view class="wx-sheet-grip"></view>
      <view class="wx-sheet-header">
        <view class="wx-sheet-title">微信安全授权</view>
        <view class="wx-sheet-desc">仅在你主动点击微信登录后申请授权，不会在首页自动弹出</view>
      </view>

      <view class="wx-step-card" :class="{ done: wxProfileAuthorized }">
        <view class="wx-step-index">1</view>
        <view class="wx-step-main">
          <view class="wx-step-title">设置头像和昵称</view>
          <view class="wx-step-tip">{{ wxProfileAuthorized ? '已完成，可继续下一步' : '点击头像和昵称框完成授权' }}</view>
          <!-- #ifdef MP-WEIXIN -->
          <view class="wx-profile-row">
            <button class="wx-avatar-btn" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
              <image v-if="wxProfile.avatarUrl" :src="wxProfile.avatarUrl" class="wx-avatar-preview" mode="aspectFill" />
              <uni-icons v-else type="contact" size="28" color="#999"></uni-icons>
            </button>
            <input class="wx-nickname-input" type="nickname" v-model="wxProfile.nickName" placeholder="点击获取微信昵称" @blur="onNicknameBlur" />
          </view>
          <!-- #endif -->
        </view>
      </view>

      <!-- #ifdef MP-WEIXIN -->
      <view class="wx-step-card phone-step" :class="{ disabled: !wxProfileAuthorized }">
        <view class="wx-step-index">2</view>
        <view class="wx-step-main">
          <view class="wx-step-title">授权手机号并登录</view>
          <view class="wx-step-tip">用于完成手机号授权并立即登录</view>
        </view>
        <button class="wx-step-action primary-action" open-type="getPhoneNumber" @getphonenumber="handleGetPhoneNumber">
          去授权
        </button>
      </view>
      <!-- #endif -->

      <view class="wx-sheet-footer" @click="closeWxAuthDialog">暂不授权</view>
    </view>
  </view>
</template>

<script>
import { getCodeImg, wxLoginApi } from '@/api/login'
import { uploadAvatar } from '@/api/system/user'
import { useUserStore } from '@/store/modules/user'
import { setToken } from '@/utils/auth'

export default {
  data() {
    return {
      codeUrl: "",
      captchaEnabled: true,
      register: true,
      agreementChecked: false,
      wxAuthVisible: false,
      wxProfileAuthorized: false,
      wxProfile: {
        nickName: "",
        avatarUrl: ""
      },
      loginForm: {
        username: "",
        password: "",
        code: "",
        uuid: ""
      }
    }
  },
  created() {
    this.getCode()
  },
  methods: {
    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled
        if (this.captchaEnabled) {
          this.codeUrl = 'data:image/gif;base64,' + res.img
          this.loginForm.uuid = res.uuid
        }
      })
    },
    ensureAgreementAccepted() {
      if (this.agreementChecked) {
        return true
      }
      this.$modal.msgError("请先阅读并同意《隐私政策》《用户服务协议》")
      return false
    },
    openAgreement(type) {
      this.$tab.navigateTo(`/pages/common/textview/index?type=${type}`)
    },
    async handleLogin() {
      if (!this.ensureAgreementAccepted()) {
        return
      }
      if (this.loginForm.username === "") {
        this.$modal.msgError("请输入您的账号")
      } else if (this.loginForm.password === "") {
        this.$modal.msgError("请输入您的密码")
      } else if (this.loginForm.code === "" && this.captchaEnabled) {
        this.$modal.msgError("请输入验证码")
      } else {
        this.$modal.loading("登录中，请耐心等待...")
        this.pwdLogin()
      }
    },
    async pwdLogin() {
      useUserStore().login(this.loginForm).then(() => {
        this.$modal.closeLoading()
        this.loginSuccess()
      }).catch(() => {
        this.$modal.closeLoading()
        if (this.captchaEnabled) {
          this.getCode()
        }
      })
    },
    loginSuccess() {
      this.$modal.loading("正在加载...")
      useUserStore().getInfo().then(() => {
        this.$modal.closeLoading()
        this.$tab.reLaunch('/pages/index')
      }).catch(() => {
        this.$modal.closeLoading()
        this.$modal.msgError("获取用户信息失败，请重新登录")
      })
    },
    handleUserRegister() {
      this.$tab.navigateTo('/pages/register')
    },
    handleWxLogin() {
      if (!this.ensureAgreementAccepted()) {
        return
      }
      this.wxAuthVisible = true
      this.wxProfileAuthorized = false
      this.wxProfile = {
        nickName: "",
        avatarUrl: ""
      }
    },
    closeWxAuthDialog() {
      this.wxAuthVisible = false
    },
    onChooseAvatar(e) {
      if (e.detail && e.detail.avatarUrl) {
        this.wxProfile.avatarUrl = e.detail.avatarUrl
        this.checkProfileComplete()
      }
    },
    onNicknameBlur(e) {
      const val = (e.detail && e.detail.value) || ''
      this.wxProfile.nickName = val.trim()
      this.checkProfileComplete()
    },
    checkProfileComplete() {
      if (this.wxProfile.avatarUrl && this.wxProfile.nickName) {
        this.wxProfileAuthorized = true
      }
    },
    async handleGetPhoneNumber(e) {
      if (!this.ensureAgreementAccepted()) {
        return
      }
      if (!this.wxProfileAuthorized) {
        this.$modal.msgError('请先设置头像和昵称')
        return
      }
      const detail = (e && e.detail) || {}
      if (detail.errMsg !== 'getPhoneNumber:ok' || !detail.code) {
        this.$modal.msgError('请先同意手机号授权')
        return
      }
      await this.doWxLogin(detail.code)
    },
    async doWxLogin(phoneCode) {
      try {
        const wxRes = await new Promise((resolve, reject) => {
          uni.login({
            provider: 'weixin',
            success: resolve,
            fail: reject
          })
        })
        if (!wxRes.code) {
          this.$modal.msgError('获取微信授权失败，请重试')
          return
        }
        const res = await wxLoginApi(wxRes.code, {
          phoneCode,
          nickName: this.wxProfile.nickName,
          avatar: ''
        })
        if (!res || res.code !== 200 || !res.token) {
          this.$modal.msgError((res && res.msg) || '微信登录失败')
          return
        }
        this.closeWxAuthDialog()
        if (res.defaultUserName) {
          uni.showModal({
            title: '已自动开通账号',
            content: `账号：${res.defaultUserName}\n初始密码：123456\n后续可在"我的-编辑资料/修改登录密码"中修改`,
            showCancel: false
          })
        }
        setToken(res.token)
        // 登录成功后上传微信头像（chooseAvatar 返回临时路径，需上传到服务器）
        if (this.wxProfile.avatarUrl) {
          try {
            await uploadAvatar({ name: 'avatarfile', filePath: this.wxProfile.avatarUrl })
          } catch (e) {
            // 头像上传失败不影响登录流程
          }
        }
        this.loginSuccess()
      } catch (e) {
        // request.js 已通过 toast 显示网络错误，此处静默处理
      }
    }
  }
}
</script>

<style lang="scss">
page {
  background-color: #f5f6f7;
}

.normal-login-container {
  width: 100%;
  height: 100vh;
  background: linear-gradient(135deg, #d4e4e1 0%, #f0f5f4 100%);
  position: relative;
  box-sizing: border-box;
  padding-top: 10vh;

  .logo-content {
    flex-direction: column;
    margin-bottom: 40rpx;
    
    .logo {
      width: 160rpx;
      height: 160rpx;
      border-radius: 50%;
      box-shadow: 0 4rpx 12rpx rgba(0,0,0,0.1);
      margin-bottom: 30rpx;
    }

    .title-box {
      text-align: center;
      .title {
        display: block;
        font-size: 56rpx;
        font-weight: bold;
        color: #2b3d3a;
        margin-bottom: 10rpx;
      }
      .sub-title {
        display: block;
        font-size: 28rpx;
        color: #5f7d79;
      }
    }
  }

  .login-form-content {
    background: #ffffff;
    border-radius: 40rpx;
    padding: 60rpx 40rpx;
    margin: 0 40rpx;
    box-shadow: 0 10rpx 30rpx rgba(43,61,58,0.08);

    .login-type-switch {
      align-items: center;

      .wx-entry {
        color: #3eb49f;
        font-weight: bold;
      }
    }

    .active {
      font-weight: bold;
      color: #2b3d3a;
      border-bottom: 4rpx solid #3eb49f;
      padding-bottom: 8rpx;
    }
    .divider {
      color: #eee;
      margin: 0 20rpx;
    }

    .input-item {
      background: #f8fbfb;
      border-radius: 20rpx;
      padding: 0 30rpx;
      height: 100rpx;
      margin-bottom: 30rpx;
      border: 2rpx solid transparent;
      transition: all 0.3s;

      &:focus-within {
        border-color: #3eb49f;
        background: #ffffff;
      }

      .icon {
        margin-right: 20rpx;
      }

      .input {
        flex: 1;
        font-size: 32rpx;
        color: #333;
      }
    }

    .code-item {
      .login-code-img {
        width: 160rpx;
        height: 60rpx;
        margin-left: 20rpx;
      }
    }

    .agreement-row {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      margin: 6rpx 0 12rpx;
      font-size: 24rpx;
      color: #6b7e7b;

      .agreement-check {
        display: flex;
        align-items: center;
        margin-right: 8rpx;
      }

      .agreement-text {
        margin: 0 6rpx;
      }

      .agreement-link {
        color: #3eb49f;
      }
    }

    .login-btn {
      background: linear-gradient(90deg, #4dc4b0, #3eb49f);
      color: #fff;
      font-size: 36rpx;
      height: 90rpx;
      line-height: 90rpx;
      border-radius: 45rpx;
      margin-top: 60rpx;
      box-shadow: 0 8rpx 20rpx rgba(62,180,159,0.3);

      &::after {
        border: none;
      }
      &:active {
        transform: translateY(2rpx);
        box-shadow: 0 4rpx 10rpx rgba(62,180,159,0.2);
      }
    }

    .reg {
      margin-top: 40rpx;
      font-size: 28rpx;
      .text-grey1 {
        color: #999;
      }
      .text-blue {
        color: #3eb49f;
        margin-left: 10rpx;
        font-weight: bold;
      }
    }
  }

  .wx-auth-mask {
    position: fixed;
    left: 0;
    right: 0;
    top: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.4);
    z-index: 99;
  }

  .wx-auth-sheet {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    background: #fff;
    border-radius: 34rpx 34rpx 0 0;
    padding: 18rpx 30rpx 26rpx;
    z-index: 100;
    box-shadow: 0 -8rpx 30rpx rgba(0, 0, 0, 0.12);
    animation: wxSheetUp .22s ease-out;

    .wx-sheet-grip {
      width: 88rpx;
      height: 8rpx;
      border-radius: 999rpx;
      background: #d9e4e1;
      margin: 6rpx auto 18rpx;
    }

    .wx-sheet-header {
      text-align: center;

      .wx-sheet-title {
        font-size: 34rpx;
        font-weight: 700;
        color: #213532;
        letter-spacing: 1rpx;
      }

      .wx-sheet-desc {
        margin-top: 8rpx;
        font-size: 24rpx;
        color: #6f8480;
        line-height: 1.5;
      }
    }

    .wx-step-card {
      margin-top: 22rpx;
      padding: 20rpx;
      border-radius: 18rpx;
      background: linear-gradient(135deg, #f6fbfa, #eef7f4);
      border: 2rpx solid #dfebe8;
      display: flex;
      align-items: center;

      &.done {
        border-color: #94d8c8;
        background: linear-gradient(135deg, #f3fbf9, #e7f7f2);
      }

      .wx-step-index {
        width: 44rpx;
        height: 44rpx;
        line-height: 44rpx;
        text-align: center;
        border-radius: 12rpx;
        background: #2f9f8b;
        color: #fff;
        font-size: 24rpx;
        font-weight: 600;
        margin-right: 16rpx;
      }

      .wx-step-main {
        flex: 1;
        min-width: 0;

        .wx-step-title {
          font-size: 30rpx;
          color: #223532;
          font-weight: 600;
        }

        .wx-step-tip {
          margin-top: 4rpx;
          font-size: 22rpx;
          color: #718884;
        }
      }

      .wx-profile-row {
        display: flex;
        align-items: center;
        margin-top: 14rpx;
        gap: 16rpx;
      }

      .wx-avatar-btn {
        width: 80rpx;
        height: 80rpx;
        border-radius: 50%;
        background: #f0f4f3;
        border: 2rpx dashed #b7c8c4;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 0;
        margin: 0;
        overflow: hidden;
        flex-shrink: 0;

        &::after {
          border: none;
        }

        .wx-avatar-preview {
          width: 80rpx;
          height: 80rpx;
          border-radius: 50%;
        }
      }

      .wx-nickname-input {
        flex: 1;
        height: 64rpx;
        line-height: 64rpx;
        font-size: 26rpx;
        padding: 0 16rpx;
        background: #f8fbfb;
        border-radius: 12rpx;
        border: 2rpx solid #dfebe8;
        color: #333;
      }

      .wx-step-action {
        margin: 0;
        width: 146rpx;
        height: 62rpx;
        line-height: 62rpx;
        border-radius: 14rpx;
        font-size: 24rpx;
        color: #2b8f7e;
        background: #ddf3ed;

        &::after {
          border: none;
        }

        &.primary-action {
          color: #fff;
          background: linear-gradient(92deg, #4ec4b0, #26967f);
        }
      }
    }

    .phone-step {
      margin-top: 14rpx;

      &.disabled {
        .wx-step-action.primary-action {
          color: #95aaa4;
          background: #dbe7e4;
        }
      }
    }

    .wx-sheet-footer {
      margin-top: 16rpx;
      text-align: center;
      font-size: 26rpx;
      color: #7d908d;
      padding: 10rpx 0;
    }
  }

  @keyframes wxSheetUp {
    from {
      transform: translateY(100%);
      opacity: 0;
    }
    to {
      transform: translateY(0);
      opacity: 1;
    }
  }
}
</style>





