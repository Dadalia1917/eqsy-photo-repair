<template>
  <view class="login-page">
    <!-- 顶部品牌区 -->
    <view class="brand-section">
      <view class="brand-circle brand-circle--1"></view>
      <view class="brand-circle brand-circle--2"></view>
      <view class="brand-inner">
        <image src="/static/logo.png" class="brand-logo" mode="aspectFill"></image>
        <text class="brand-name">e起守忆</text>
        <text class="brand-slogan">社区志愿服务平台</text>
      </view>
    </view>

    <!-- 登录卡片 -->
    <view class="login-card">
      <text class="card-heading">用户登录</text>

      <!-- 账号密码表单（主要入口） -->
      <view class="form-section">
        <view class="field-label">账号</view>
        <view class="input-item">
          <uni-icons type="person" size="20" color="#3eb49f" class="input-icon"></uni-icons>
          <input
            v-model="loginForm.username"
            class="input"
            type="text"
            placeholder="请输入账号"
            maxlength="30"
          />
        </view>
        <view class="field-label">密码</view>
        <view class="input-item">
          <uni-icons type="locked" size="20" color="#3eb49f" class="input-icon"></uni-icons>
          <input
            v-model="loginForm.password"
            type="password"
            class="input"
            placeholder="请输入密码"
            maxlength="20"
          />
        </view>
        <view class="field-label" v-if="captchaEnabled">验证码</view>
        <view class="input-item" v-if="captchaEnabled">
          <uni-icons type="help" size="20" color="#3eb49f" class="input-icon"></uni-icons>
          <input
            v-model="loginForm.code"
            type="number"
            class="input"
            placeholder="请输入验证码"
            maxlength="4"
          />
          <image :src="codeUrl" @click="getCode" class="captcha-img"></image>
        </view>
      </view>

      <!-- 主登录按钮 -->
      <button class="btn-login" @click="handleLogin">登 录</button>

      <!-- 协议 -->
      <view class="agreement-row">
        <view class="agreement-check" @click="agreementChecked = !agreementChecked">
          <uni-icons
            :type="agreementChecked ? 'checkbox-filled' : 'circle'"
            size="18"
            :color="agreementChecked ? '#3eb49f' : '#b7c3c1'"
          ></uni-icons>
          <text class="agreement-text">登录即同意</text>
        </view>
        <text class="agreement-link" @click.stop="openAgreement('privacy')">《用户协议》</text>
        <text class="agreement-text">和</text>
        <text class="agreement-link" @click.stop="openAgreement('service')">《隐私协议》</text>
      </view>

      <!-- 分隔线 -->
      <view class="divider-row">
        <view class="divider-line"></view>
        <text class="divider-text">其他登录方式</text>
        <view class="divider-line"></view>
      </view>

      <!-- 微信登录（次要入口）-->
      <!-- #ifdef MP-WEIXIN -->
      <button
        class="btn-wx"
        open-type="getPhoneNumber"
        @getphonenumber="handleGetPhoneNumber"
      >
        <view class="btn-wx-inner">
          <uni-icons type="weixin" size="28" color="#07c160"></uni-icons>
          <text class="btn-wx-label">微信一键登录</text>
        </view>
      </button>
      <!-- #endif -->

      <!-- 注册链接 -->
      <view class="reg-row" v-if="register">
        <text class="reg-text">还没有账号？</text>
        <text class="reg-link" @click="handleUserRegister">立即注册</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getCodeImg, wxLoginApi } from '@/api/login'
import { useUserStore } from '@/store/modules/user'
import { setToken } from '@/utils/auth'

export default {
  data() {
    return {
      codeUrl: "",
      captchaEnabled: true,
      register: true,
      agreementChecked: false,
      loginForm: {
        username: "",
        password: "",
        code: "",
        uuid: ""
      }
    }
  },
  onReady() {
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
      this.$modal.msgError('请在微信小程序中使用一键授权登录')
    },
    async ensureWxPrivacyAuthorized() {
      // #ifdef MP-WEIXIN
      if (typeof wx !== 'undefined' && wx.requirePrivacyAuthorize) {
        await new Promise((resolve, reject) => {
          wx.requirePrivacyAuthorize({
            success: resolve,
            fail: reject
          })
        })
      }
      // #endif
    },
    async handleGetPhoneNumber(e) {
      if (!this.ensureAgreementAccepted()) {
        return
      }
      const detail = (e && e.detail) || {}

      // 兼容不同机型返回差异：只要拿到 code 就直接走登录。
      if (detail.code) {
        await this.doWxLogin(detail.code)
        return
      }

      const errMsg = detail.errMsg || ''
      if (errMsg.indexOf('cancel') !== -1 || errMsg.indexOf('deny') !== -1) {
        this.$modal.msgError('你已取消手机号授权')
        return
      }

      // 未拿到 code 时再尝试隐私授权兜底，避免部分安卓机型授权链路被中断。
      try {
        await this.ensureWxPrivacyAuthorized()
        this.$modal.msgError('请再次点击微信一键登录完成授权')
      } catch (err) {
        this.$modal.msgError('请先同意隐私协议后再登录')
      }
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
          phoneCode
        })
        if (!res || res.code !== 200 || !res.token) {
          this.$modal.msgError((res && res.msg) || '微信登录失败')
          return
        }
        if (res.defaultUserName) {
          uni.showModal({
            title: '已自动开通账号',
            content: `账号：${res.defaultUserName}\n初始密码：123456\n后续可在"我的-编辑资料/修改登录密码"中修改`,
            showCancel: false
          })
        }
        setToken(res.token)
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
  background-color: #f0f7f5;
}

.login-page {
  min-height: 100vh;
  background: #f0f7f5;
}

/* ====== 品牌顶部区 ====== */
.brand-section {
  position: relative;
  background: linear-gradient(135deg, #1f7f6f 0%, #3eb49f 100%);
  padding: 80rpx 40rpx 80rpx;
  text-align: center;
  overflow: hidden;
}

.brand-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}
.brand-circle--1 {
  width: 320rpx;
  height: 320rpx;
  top: -80rpx;
  right: -60rpx;
}
.brand-circle--2 {
  width: 240rpx;
  height: 240rpx;
  bottom: -60rpx;
  left: -40rpx;
}

.brand-inner {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.brand-logo {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  border: 4rpx solid rgba(255, 255, 255, 0.5);
  margin-bottom: 20rpx;
  box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.15);
}

.brand-name {
  font-size: 56rpx;
  font-weight: bold;
  color: #fff;
  margin-bottom: 10rpx;
  letter-spacing: 4rpx;
}

.brand-slogan {
  font-size: 26rpx;
  color: rgba(255, 255, 255, 0.8);
}

/* ====== 登录卡片 ====== */
.login-card {
  background: #fff;
  border-radius: 40rpx 40rpx 0 0;
  margin-top: -30rpx;
  padding: 60rpx 50rpx 80rpx;
  min-height: 60vh;
  position: relative;
  z-index: 1;
}

.card-heading {
  display: block;
  font-size: 48rpx;
  font-weight: bold;
  color: #2b3d3a;
  margin-bottom: 40rpx;
}

/* 字段标签 */
.field-label {
  font-size: 30rpx;
  font-weight: 600;
  color: #4a6260;
  margin-bottom: 12rpx;
  margin-top: 24rpx;

  &:first-child {
    margin-top: 0;
  }
}

/* 微信登录（次要按钮） */
.btn-wx {
  width: 100%;
  background: #fff;
  color: #07c160;
  border: 2rpx solid #07c160;
  height: 96rpx;
  border-radius: 48rpx;
  margin-bottom: 24rpx;

  &::after {
    border: none;
  }

  &:active {
    background: #f0fff4;
  }
}

.btn-wx-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14rpx;
  width: 100%;
  height: 100%;
}


.btn-wx-label {
  font-size: 34rpx;
  font-weight: bold;
  color: #07c160;
  letter-spacing: 1rpx;
}

/* 分隔线 */
.divider-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin: 40rpx 0;
}

.divider-line {
  flex: 1;
  height: 1rpx;
  background: #e8e8e8;
}

.divider-text {
  font-size: 24rpx;
  color: #bbb;
  white-space: nowrap;
}

/* 输入框 */
.form-section {
  margin-bottom: 10rpx;
}

.input-item {
  background: #f8fbfb;
  border-radius: 20rpx;
  padding: 0 30rpx;
  height: 100rpx;
  margin-bottom: 24rpx;
  border: 2rpx solid transparent;
  display: flex;
  align-items: center;
  gap: 20rpx;

  &:focus-within {
    border-color: #3eb49f;
    background: #fff;
  }
}

.input-icon {
  flex-shrink: 0;
}

.input {
  flex: 1;
  font-size: 32rpx;
  color: #333;
  height: 100%;
}

.captcha-img {
  width: 160rpx;
  height: 64rpx;
  border-radius: 8rpx;
  flex-shrink: 0;
}

/* 协议 */
.agreement-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  margin: 12rpx 0 20rpx;
  font-size: 24rpx;
  color: #6b7e7b;
}

.agreement-check {
  display: flex;
  align-items: center;
  gap: 8rpx;
  margin-right: 8rpx;
}

.agreement-text {
  margin: 0 4rpx;
}

.agreement-link {
  color: #3eb49f;
}

/* 登录按钮 */
.btn-login {
  width: 100%;
  background: linear-gradient(90deg, #4dc4b0, #3eb49f);
  color: #fff;
  font-size: 38rpx;
  font-weight: bold;
  height: 108rpx;
  line-height: 108rpx;
  border-radius: 54rpx;
  margin-top: 36rpx;
  letter-spacing: 6rpx;
  box-shadow: 0 10rpx 28rpx rgba(62, 180, 159, 0.4);

  &::after {
    border: none;
  }

  &:active {
    transform: translateY(2rpx);
    box-shadow: 0 4rpx 12rpx rgba(62, 180, 159, 0.2);
  }
}

/* 注册 */
.reg-row {
  text-align: center;
  margin-top: 40rpx;
  font-size: 28rpx;
}

.reg-text {
  color: #999;
}

.reg-link {
  color: #3eb49f;
  font-weight: bold;
  margin-left: 8rpx;
}
</style>





