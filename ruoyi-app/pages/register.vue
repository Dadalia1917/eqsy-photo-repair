<template>
  <view class="normal-login-container">
    <view class="logo-content align-center justify-center flex">
      <image class="logo" src="/static/logo.png" mode="widthFix"></image>
      <view class="title-box">
        <text class="title">注册 e起守忆</text>
        <text class="sub-title">让爱与回忆在社区流传</text>
      </view>
    </view>

    <view class="login-form-content">
      
      <!-- 用户名 -->
      <view class="input-item flex align-center">
        <uni-icons class="icon" type="person" size="20" color="#5f7d79"></uni-icons>
        <input v-model="registerForm.username" class="input" type="text" placeholder="请输入用户名" maxlength="20" />
      </view>
      
      <!-- 图形验证码兜底 (若后台开启了普通验证码也会显示) -->
      <view class="input-item code-item flex align-center" v-if="captchaEnabled">
        <uni-icons class="icon" type="help" size="20" color="#5f7d79"></uni-icons>
        <input v-model="registerForm.code" type="number" class="input" placeholder="请输入图形验证码" maxlength="4" />
        <image :src="codeUrl" @click="getCode" class="login-code-img"></image>
      </view>

      <!-- 密码设置 -->
      <view class="input-item flex align-center">
        <uni-icons class="icon" type="locked" size="20" color="#5f7d79"></uni-icons>
        <input v-model="registerForm.password" type="password" class="input" placeholder="请设置登录密码" maxlength="20" />
      </view>
      <view class="input-item flex align-center">
        <uni-icons class="icon" type="locked" size="20" color="#5f7d79"></uni-icons>
        <input v-model="registerForm.confirmPassword" type="password" class="input" placeholder="请再次输入密码" maxlength="20" />
      </view>

      <view class="action-btn" style="margin-top: 50rpx;">
        <button @click="handleRegister" class="login-btn round">同意协议并注册</button>
      </view>

      <view class="reg text-center" style="margin-top: 30rpx;">
        <text class="text-grey1">已有账号？</text>
        <text @click="goToLogin" class="text-blue">返回登录</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getCodeImg, register } from '@/api/login'

export default {
  data() {
    return {
      codeUrl: "",
      captchaEnabled: true,
      registerForm: {
        username: "",
        password: "",
        confirmPassword: "",
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
          this.registerForm.uuid = res.uuid
        }
      })
    },
    async handleRegister() {
      if (this.registerForm.username === "") {
        this.$modal.msgError("请输入用户名")
      } else if (this.registerForm.password === "") {
        this.$modal.msgError("请输入您的密码")
      } else if (this.registerForm.confirmPassword === "") {
        this.$modal.msgError("请再次输入您的密码")
      } else if (this.registerForm.password !== this.registerForm.confirmPassword) {
        this.$modal.msgError("两次输入的密码不一致")
      } else if (this.registerForm.code === "" && this.captchaEnabled) {
        this.$modal.msgError("请输入图形验证码")
      } else {
        this.$modal.loading("正在为您注册...")
        register(this.registerForm).then(res => {
          this.$modal.closeLoading()
          this.$modal.alert("长辈您好，您的账号注册成功！", "注册成功")
          setTimeout(() => {
            uni.reLaunch({ url: '/pages/login' })
          }, 400)
        }).catch(() => {
          this.$modal.closeLoading()
          if (this.captchaEnabled) {
            this.getCode()
          }
        })
      }
    },
    goToLogin() {
      this.$tab.reLaunch('/pages/login')
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
  padding-bottom: 60rpx;
  background: linear-gradient(135deg, #d4e4e1 0%, #f0f5f4 100%);
  position: relative;
  box-sizing: border-box;
  padding-top: 6vh;

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
        font-size: 48rpx;
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
        border-radius: 8rpx;
      }
    }

    .login-btn {
      background: linear-gradient(90deg, #4dc4b0, #3eb49f);
      color: #fff;
      font-size: 36rpx;
      height: 90rpx;
      line-height: 90rpx;
      border-radius: 45rpx;
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
}
</style>
