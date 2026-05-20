<template>
  <div class="auth-page">
    <div class="auth-container">
      <!-- 品牌区 -->
      <div class="brand-area">
        <div class="brand-logo-wrap">
          <div class="brand-logo-fallback">忆</div>
        </div>
        <h1 class="brand-title">e起守忆</h1>
        <p class="brand-sub">社区志愿服务管理系统</p>
      </div>

      <!-- 注册卡片 -->
      <el-card class="auth-card" :body-style="{ padding: '36px 40px' }">
        <h2 class="card-title">注册账号</h2>

        <el-form ref="registerRef" :model="registerForm" :rules="registerRules">
          <el-form-item prop="username">
            <el-input
              v-model="registerForm.username"
              type="text"
              size="large"
              auto-complete="off"
              placeholder="用户名"
              class="form-input"
            >
              <template #prefix><svg-icon icon-class="user" class="el-input__icon input-icon" /></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="registerForm.password"
              type="password"
              size="large"
              auto-complete="off"
              placeholder="密码"
              @keyup.enter="handleRegister"
              class="form-input"
            >
              <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              size="large"
              auto-complete="off"
              placeholder="确认密码"
              @keyup.enter="handleRegister"
              class="form-input"
            >
              <template #prefix><svg-icon icon-class="password" class="el-input__icon input-icon" /></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="phonenumber">
            <el-input
              v-model="registerForm.phonenumber"
              type="text"
              size="large"
              auto-complete="off"
              placeholder="手机号"
              class="form-input"
            >
              <template #prefix><svg-icon icon-class="phone" class="el-input__icon input-icon" /></template>
            </el-input>
          </el-form-item>
          <el-form-item prop="code" v-if="captchaEnabled">
            <div class="captcha-row">
              <el-input
                size="large"
                v-model="registerForm.code"
                auto-complete="off"
                placeholder="图像验证码"
                @keyup.enter="handleRegister"
                class="captcha-input form-input"
              >
                <template #prefix><svg-icon icon-class="validCode" class="el-input__icon input-icon" /></template>
              </el-input>
              <div class="captcha-img-wrap">
                <img :src="codeUrl" @click="getCode" class="captcha-img" />
              </div>
            </div>
          </el-form-item>

          <el-button
            :loading="loading"
            size="large"
            type="primary"
            class="submit-btn"
            @click.prevent="handleRegister"
          >
            <span v-if="!loading">注 册</span>
            <span v-else>注册中...</span>
          </el-button>

          <div class="form-footer">
            已有账号？<router-link class="link-type" to="/login">立即登录</router-link>
          </div>
        </el-form>
      </el-card>
    </div>

    <div class="auth-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script setup>
import { ElMessageBox } from "element-plus"
import { getCodeImg, register } from "@/api/login"
import defaultSettings from '@/settings'

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const router = useRouter()
const { proxy } = getCurrentInstance()

const registerForm = ref({
  username: "",
  phonenumber: "",
  password: "",
  confirmPassword: "",
  code: "",
  uuid: ""
})

const equalToPassword = (rule, value, callback) => {
  if (registerForm.value.password !== value) {
    callback(new Error("两次输入的密码不一致"))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, trigger: "blur", message: "请输入用户名" },
    { min: 2, max: 20, message: "用户名长度必须介于 2 和 20 之间", trigger: "blur" }
  ],
  phonenumber: [
    { required: true, trigger: "blur", message: "请输入手机号" },
    { pattern: /^1\d{10}$/, message: "请输入正确的11位手机号", trigger: "blur" }
  ],
  password: [
    { required: true, trigger: "blur", message: "请输入您的密码" },
    { min: 5, max: 20, message: "用户密码长度必须介于 5 和 20 之间", trigger: "blur" },
    { pattern: /^[^<>"'|\\]+$/, message: "不能包含非法字符：< > \" ' \\\ |", trigger: "blur" }
  ],
  confirmPassword: [
    { required: true, trigger: "blur", message: "请再次输入您的密码" },
    { required: true, validator: equalToPassword, trigger: "blur" }
  ],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const codeUrl = ref("")
const loading = ref(false)
const captchaEnabled = ref(true)

function handleRegister() {
  proxy.$refs.registerRef.validate(valid => {
    if (valid) {
      loading.value = true
      register(registerForm.value).then(res => {
        const username = registerForm.value.username
        ElMessageBox.alert("<font color='red'>恭喜你，您的账号 " + username + " 注册成功！</font>", "系统提示", {
          dangerouslyUseHTMLString: true,
          type: "success",
        }).then(() => {
          router.push("/login")
        }).catch(() => {})
      }).catch(() => {
        loading.value = false
        if (captchaEnabled.value) {
          getCode()
        }
      })
    }
  })
}

function getCode() {
  getCodeImg().then(res => {
    captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled
    if (captchaEnabled.value) {
      codeUrl.value = "data:image/gif;base64," + res.img
      registerForm.value.uuid = res.uuid
    }
  })
}

getCode()
</script>

<style lang='scss' scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a6b5e 0%, #3eb49f 60%, #5dd4c0 100%);
  padding: 40px 16px;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    width: 500px; height: 500px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.06);
    top: -150px; right: -100px;
    pointer-events: none;
  }
  &::after {
    content: '';
    position: absolute;
    width: 400px; height: 400px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.06);
    bottom: -120px; left: -80px;
    pointer-events: none;
  }
}

.auth-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 28px;
  position: relative;
  z-index: 1;
  width: 100%;
}

.brand-area {
  text-align: center;
  color: #fff;
}

.brand-logo-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.brand-logo-fallback {
  width: 80px; height: 80px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  border: 3px solid rgba(255, 255, 255, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
  font-weight: bold;
  color: #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}

.brand-title {
  font-size: 36px;
  font-weight: bold;
  letter-spacing: 4px;
  margin: 0 0 10px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.brand-sub {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.85);
  margin: 0;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  border-radius: 18px !important;
  border: none !important;
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.2) !important;
}

.card-title {
  text-align: center;
  font-size: 22px;
  font-weight: 600;
  color: #2b3d3a;
  margin: 0 0 28px;
}

.form-input {
  :deep(.el-input__wrapper) {
    border-radius: 10px;
    padding: 4px 12px;
    box-shadow: 0 0 0 1px #dde8e6 inset;
    &:hover { box-shadow: 0 0 0 1px #3eb49f inset; }
    &.is-focus { box-shadow: 0 0 0 2px rgba(62, 180, 159, 0.25) inset, 0 0 0 1px #3eb49f inset; }
  }
}

.captcha-row {
  display: flex;
  gap: 12px;
  align-items: center;
  width: 100%;
}
.captcha-input { flex: 1; }
.captcha-img-wrap { flex-shrink: 0; }
.captcha-img {
  height: 40px;
  border-radius: 6px;
  cursor: pointer;
  display: block;
  border: 1px solid #dde8e6;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  border-radius: 10px;
  background: linear-gradient(90deg, #4dc4b0, #3eb49f);
  border: none;
  box-shadow: 0 6px 16px rgba(62, 180, 159, 0.4);
  letter-spacing: 4px;
  margin-top: 4px;

  &:hover {
    background: linear-gradient(90deg, #5dd4c0, #4ec4af);
    box-shadow: 0 8px 20px rgba(62, 180, 159, 0.5);
  }
}

.form-footer {
  text-align: center;
  color: #8a9a97;
  font-size: 13px;
  margin-top: 16px;
}

.link-type {
  color: #3eb49f;
  text-decoration: none;
  font-weight: 500;
  &:hover { color: #2a9a87; }
}

.auth-footer {
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  letter-spacing: 1px;
  padding: 10px 0;
  z-index: 1;
}
</style>
