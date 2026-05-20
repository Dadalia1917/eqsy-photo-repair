<template>
  <div class="login">
    <!-- 左侧品牌区 -->
    <div class="login-brand">
      <div class="brand-content">
        <div class="brand-logo-wrap">
          <img src="@/assets/logo/logo.png" class="brand-logo" alt="logo" v-if="logoExists" @error="logoExists=false" />
          <div class="brand-logo-fallback" v-else>忆</div>
        </div>
        <div class="brand-title">e起守忆</div>
        <div class="brand-sub">社区志愿服务管理系统</div>
        <div class="brand-features">
          <div class="feature-item"><span class="feature-dot"></span>老照片修复任务管理</div>
          <div class="feature-item"><span class="feature-dot"></span>志愿者任务认领与完成</div>
          <div class="feature-item"><span class="feature-dot"></span>数据统计与服务追踪</div>
        </div>
      </div>
    </div>

    <!-- 右侧登录区 -->
    <div class="login-form-wrap">
      <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
        <div class="form-header">
          <h2 class="form-title">管理员登录</h2>
          <p class="form-sub">请输入您的管理账号</p>
        </div>

        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            type="text"
            auto-complete="off"
            placeholder="账号"
            class="form-input"
          >
            <template #prefix>
              <svg-icon icon-class="user" class="el-input__icon input-icon" />
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            auto-complete="off"
            placeholder="密码"
            @keyup.enter="handleLogin"
            class="form-input"
          >
            <template #prefix>
              <svg-icon icon-class="password" class="el-input__icon input-icon" />
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="code" v-if="captchaEnabled">
          <div class="captcha-row">
            <el-input
              v-model="loginForm.code"
              auto-complete="off"
              placeholder="验证码"
              @keyup.enter="handleLogin"
              class="captcha-input"
            >
              <template #prefix>
                <svg-icon icon-class="validCode" class="el-input__icon input-icon" />
              </template>
            </el-input>
            <div class="captcha-img-wrap">
              <img :src="codeUrl" @click="getCode" class="captcha-img" />
            </div>
          </div>
        </el-form-item>

        <div class="form-options">
          <el-checkbox v-model="loginForm.rememberMe">记住密码</el-checkbox>
          <router-link class="register-link" to="/register" v-if="register">立即注册</router-link>
        </div>

        <el-button
          :loading="loading"
          type="primary"
          class="login-btn"
          @click.prevent="handleLogin"
        >
          <span v-if="!loading">登 录</span>
          <span v-else>登录中...</span>
        </el-button>
      </el-form>
    </div>

    <div class="el-login-footer">
      <span>{{ footerContent }}</span>
    </div>
  </div>
</template>

<script setup>
import { getCodeImg } from "@/api/login"
import Cookies from "js-cookie"
import { encrypt, decrypt } from "@/utils/jsencrypt"
import useUserStore from '@/store/modules/user'
import defaultSettings from '@/settings'

const title = import.meta.env.VITE_APP_TITLE
const footerContent = defaultSettings.footerContent
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const { proxy } = getCurrentInstance()

const logoExists = ref(true)

const loginForm = ref({
  username: "",
  password: "",
  rememberMe: false,
  code: "",
  uuid: ""
})

const loginRules = {
  username: [{ required: true, trigger: "blur", message: "请输入您的账号" }],
  password: [{ required: true, trigger: "blur", message: "请输入您的密码" }],
  code: [{ required: true, trigger: "change", message: "请输入验证码" }]
}

const codeUrl = ref("")
const loading = ref(false)
// 验证码开关
const captchaEnabled = ref(true)
// 注册开关
const register = ref(true)
const redirect = ref(undefined)

watch(route, (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect
}, { immediate: true })

function handleLogin() {
  proxy.$refs.loginRef.validate(valid => {
    if (valid) {
      loading.value = true
      // 勾选了需要记住密码设置在 cookie 中设置记住用户名和密码
      if (loginForm.value.rememberMe) {
        Cookies.set("username", loginForm.value.username, { expires: 30 })
        Cookies.set("password", encrypt(loginForm.value.password), { expires: 30 })
        Cookies.set("rememberMe", loginForm.value.rememberMe, { expires: 30 })
      } else {
        // 否则移除
        Cookies.remove("username")
        Cookies.remove("password")
        Cookies.remove("rememberMe")
      }
      // 调用action的登录方法
      userStore.login(loginForm.value).then(() => {
        const query = route.query
        const otherQueryParams = Object.keys(query).reduce((acc, cur) => {
          if (cur !== "redirect") {
            acc[cur] = query[cur]
          }
          return acc
        }, {})
        router.push({ path: redirect.value || "/", query: otherQueryParams })
      }).catch(() => {
        loading.value = false
        // 重新获取验证码
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
      loginForm.value.uuid = res.uuid
    }
  })
}

function getCookie() {
  const rememberMe = Cookies.get("rememberMe")
  if (!rememberMe || !Boolean(rememberMe)) {
    return
  }
  const username = Cookies.get("username")
  const password = Cookies.get("password")
  loginForm.value = {
    username: username === undefined ? loginForm.value.username : username,
    password: password === undefined ? loginForm.value.password : decrypt(password),
    rememberMe: true
  }
}

getCode()
getCookie()
</script>

<style lang='scss' scoped>
.login {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: #f0f7f5;
}

/* ====== 左侧品牌区 ====== */
.login-brand {
  width: 45%;
  background: linear-gradient(135deg, #1a6b5e 0%, #3eb49f 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    width: 400px; height: 400px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.06);
    top: -100px; right: -100px;
  }
  &::after {
    content: '';
    position: absolute;
    width: 300px; height: 300px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.06);
    bottom: -80px; left: -60px;
  }
}

.brand-content {
  text-align: center;
  color: #fff;
  position: relative;
  z-index: 1;
}

.brand-logo-wrap { margin-bottom: 20px; }

.brand-logo {
  width: 80px; height: 80px;
  border-radius: 50%;
  border: 3px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
}

.brand-logo-fallback {
  width: 80px; height: 80px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  border: 3px solid rgba(255, 255, 255, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  font-weight: bold;
  color: #fff;
  margin: 0 auto;
}

.brand-title {
  font-size: 36px;
  font-weight: bold;
  letter-spacing: 3px;
  margin-bottom: 10px;
}

.brand-sub {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 40px;
}

.brand-features { text-align: left; display: inline-block; }

.feature-item {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.feature-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.7);
  flex-shrink: 0;
}

/* ====== 右侧登录区 ====== */
.login-form-wrap {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  background: #fff;
}

.login-form {
  width: 100%;
  max-width: 380px;
}

.form-header {
  margin-bottom: 32px;
}

.form-title {
  font-size: 28px;
  font-weight: bold;
  color: #2b3d3a;
  margin: 0 0 8px;
}

.form-sub {
  font-size: 14px;
  color: #8a9a97;
  margin: 0;
}

:deep(.el-form-item) {
  margin-bottom: 18px;
}

.form-input,
.captcha-input {
  :deep(.el-input__wrapper) {
    border-radius: 10px;
    padding: 0 12px;
    height: 42px;
    box-shadow: 0 0 0 1px #dde8e6 inset;
    &:hover { box-shadow: 0 0 0 1px #3eb49f inset; }
    &.is-focus { box-shadow: 0 0 0 2px rgba(62, 180, 159, 0.2) inset, 0 0 0 1px #3eb49f inset; }
  }
  :deep(.el-input__inner) {
    height: 42px;
    line-height: 42px;
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

.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 4px 0 20px;
}

.register-link {
  font-size: 14px;
  color: #3eb49f;
  text-decoration: none;
  &:hover { color: #2a9a87; }
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 10px;
  background: linear-gradient(90deg, #4dc4b0, #3eb49f);
  border: none;
  box-shadow: 0 6px 16px rgba(62, 180, 159, 0.35);
  letter-spacing: 4px;

  &:hover {
    background: linear-gradient(90deg, #5dd4c0, #4ec4af);
    box-shadow: 0 8px 20px rgba(62, 180, 159, 0.45);
  }
}

.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #999;
  font-size: 12px;
  letter-spacing: 1px;
}

@media (max-width: 768px) {
  .login-brand { display: none; }
  .login-form-wrap { padding: 40px 24px; }
}
</style>
