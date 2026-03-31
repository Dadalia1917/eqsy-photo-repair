<template>
  <view class="normal-login-container">
    <view class="logo-content align-center justify-center flex">
      <image class="logo" src="/static/logo.png" mode="widthFix"></image>
      <view class="title-box">
        <text class="title">e起守忆</text>
        <text class="sub-title">社区关怀老人影像修复平台</text>
      </view>
    </view>

    <view class="login-form-content">
      <view class="login-type-switch" style="display:flex;justify-content:center;margin-bottom:20px;">
        <text :class="loginType === 'pwd' ? 'active' : ''" @click="loginType = 'pwd'" style="margin-right:10px;font-size:16px;">密码登录</text>
        <text class="divider">|</text>
        <text :class="loginType === 'sms' ? 'active' : ''" @click="loginType = 'sms'" style="margin-left:10px;font-size:16px;">短信登录</text>
      </view>

      <!-- 密码登录 -->
      <view class="input-item flex align-center" v-if="loginType === 'pwd'">
        <uni-icons class="icon" type="person" size="20" color="#5f7d79"></uni-icons>
        <input v-model="loginForm.username" class="input" type="text" placeholder="请输入账号" maxlength="30" />
      </view>
      <view class="input-item flex align-center" v-if="loginType === 'pwd'">
        <uni-icons class="icon" type="locked" size="20" color="#5f7d79"></uni-icons>
        <input v-model="loginForm.password" type="password" class="input" placeholder="请输入密码" maxlength="20" />
      </view>
      <view class="input-item code-item flex align-center" v-if="loginType === 'pwd' && captchaEnabled">
        <uni-icons class="icon" type="help" size="20" color="#5f7d79"></uni-icons>
        <input v-model="loginForm.code" type="number" class="input" placeholder="请输入验证码" maxlength="4" />
        <image :src="codeUrl" @click="getCode" class="login-code-img"></image>
      </view>

      <!-- 短信登录 -->
      <view class="input-item flex align-center" v-if="loginType === 'sms'">
        <uni-icons class="icon" type="phone" size="20" color="#5f7d79"></uni-icons>
        <input v-model="smsForm.phone" class="input" type="number" placeholder="请输入手机号" maxlength="11" />
      </view>
      <view class="input-item code-item flex align-center" v-if="loginType === 'sms'">
        <uni-icons class="icon" type="chat" size="20" color="#5f7d79"></uni-icons>
        <input v-model="smsForm.code" type="number" class="input" placeholder="请输入短信验证码" maxlength="6" />
        <view class="login-code" style="padding-right:0;">
          <button class="cu-btn sm text-green" style="background:transparent;" @click="sendSmsCode" :disabled="smsTimer > 0">{{ smsTimer > 0 ? smsTimer + "s后获取" : "获取验证码" }}</button>
        </view>
      </view>

      <view class="action-btn">
        <button @click="handleLogin" class="login-btn round">登录</button>
      </view>
      <view class="reg text-center" v-if="register">
        <text class="text-grey1">还没有账号？</text>
        <text @click="handleUserRegister" class="text-blue">立即注册</text>
      </view>

      <view class="divider-row">
        <view class="divider-line"></view>
        <text class="divider-text">或</text>
        <view class="divider-line"></view>
      </view>

      <button class="wx-login-btn" @click="handleWxLogin">
        <uni-icons type="weixin" size="22" color="#fff" style="margin-right:12rpx;"></uni-icons>
        <text>微信一键登录</text>
      </button>
    </view>
  </view>
</template>

<script>
import { getCodeImg, sendSmsCodeApi, smsLoginApi, wxLoginApi } from '@/api/login'
import { useUserStore } from '@/store/modules/user'
import { setToken } from '@/utils/auth'

export default {
  data() {
    return {
      loginType: 'pwd',
      codeUrl: "",
      captchaEnabled: true,
      register: true,
      loginForm: {
        username: "",
        password: "",
        code: "",
        uuid: ""
      },
            smsTimer: 0,
      smsForm: {
        phone: "",
        code: ""
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
        async sendSmsCode() {
      if (!this.smsForm.phone || this.smsForm.phone.length !== 11) {
        this.$modal.msgError("请输入有效的11位手机号")
        return
      }
      if (this.smsTimer > 0) return

          const res = await sendSmsCodeApi(this.smsForm.phone).catch(() => null)
          if (!res || res.code !== 200) {
            this.$modal.msgError((res && res.msg) || "验证码发送失败")
            return
          }
      this.smsTimer = 60
      let timer = setInterval(() => {
        this.smsTimer--
        if (this.smsTimer <= 0) {
          clearInterval(timer)
        }
      }, 1000)
          this.$modal.msgSuccess("验证码已发送，请注意查收")
          if (res.smsCode) {
            this.$modal.msg("开发调试验证码: " + res.smsCode)
          }
    },
    async handleLogin() {
      if (this.loginType === 'pwd') {
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
      } else {
        if (this.smsForm.phone === "") {
          this.$modal.msgError("请输入手机号")
        } else if (this.smsForm.code === "") {
          this.$modal.msgError("请输入短信验证码")
        } else {
          this.$modal.loading("登录中，请耐心等待...")
          const res = await smsLoginApi(this.smsForm.phone, this.smsForm.code).catch(() => null)
          this.$modal.closeLoading()
          if (!res || res.code !== 200 || !res.token) {
            this.$modal.msgError((res && res.msg) || "短信登录失败")
            return
          }
          setToken(res.token)
          this.loginSuccess()
        }
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
    async handleWxLogin() {
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
        const res = await wxLoginApi(wxRes.code)
        if (!res || res.code !== 200 || !res.token) {
          this.$modal.msgError((res && res.msg) || '微信登录失败')
          return
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

    .divider-row {
      display: flex;
      align-items: center;
      margin: 40rpx 0 30rpx;

      .divider-line {
        flex: 1;
        height: 1rpx;
        background: #e8e8e8;
      }

      .divider-text {
        font-size: 26rpx;
        color: #bbb;
        margin: 0 20rpx;
      }
    }

    .wx-login-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      background: #07c160;
      color: #fff;
      font-size: 32rpx;
      height: 90rpx;
      line-height: 90rpx;
      border-radius: 45rpx;
      border: none;
      box-shadow: 0 8rpx 20rpx rgba(7,193,96,0.25);

      &::after {
        border: none;
      }
      &:active {
        transform: translateY(2rpx);
        box-shadow: 0 4rpx 10rpx rgba(7,193,96,0.15);
      }

    }
  }
}
</style>





