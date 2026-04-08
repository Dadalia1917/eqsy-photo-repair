<template>
  <view class="container">
    <view class="example">
      <uni-forms ref="form" :model="user" labelWidth="80px">
        <uni-forms-item label="登录账号" name="userName">
          <uni-easyinput v-model="user.userName" placeholder="请输入登录账号" maxlength="30" />
        </uni-forms-item>
        <uni-forms-item label="用户昵称" name="nickName">
          <uni-easyinput v-model="user.nickName" placeholder="请输入昵称" />
        </uni-forms-item>
        <uni-forms-item label="手机号码" name="phonenumber">
          <uni-easyinput v-model="user.phonenumber" placeholder="请输入手机号码" />
        </uni-forms-item>
        <uni-forms-item label="邮箱" name="email">
          <uni-easyinput v-model="user.email" placeholder="请输入邮箱" />
        </uni-forms-item>
        <uni-forms-item label="性别" name="sex" required>
          <uni-data-checkbox v-model="user.sex" :localdata="sexs" />
        </uni-forms-item>
      </uni-forms>
      <button type="primary" @click="submit">提交</button>
    </view>
  </view>
</template>

<script setup>
  import { getUserProfile } from "@/api/system/user"
  import { updateUserProfile } from "@/api/system/user"
  import { ref , getCurrentInstance } from "vue"
  import { onReady } from  "@dcloudio/uni-app"

  const { proxy } = getCurrentInstance()
  const user = ref({
    userName: "",
    nickName: "",
    phonenumber: "",
    email: "",
    sex: ""
  })
  const sexs = [{
    text: '男',
    value: "0"
  }, {
    text: '女',
    value: "1"
  }]
  const rules = ref({
    userName: {
      rules: [{
        required: true,
        errorMessage: '登录账号不能为空'
      }, {
        minLength: 2,
        maxLength: 30,
        errorMessage: '登录账号长度需在2到30个字符之间'
      }]
    },
    nickName: {
      rules: [{
        required: true,
        errorMessage: '用户昵称不能为空'
      }]
    },
    phonenumber: {
      rules: [{
        validateFunction: (rule, value) => {
          if (!value) return true
          return /^1[3|4|5|6|7|8|9][0-9]\d{8}$/.test(value)
        },
        errorMessage: '请输入正确的手机号码'
      }]
    },
    email: {
      rules: [{
        validateFunction: (rule, value) => {
          if (!value) return true
          return /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(value)
        },
        errorMessage: '请输入正确的邮箱地址'
      }]
    }
  })

  function getUser() {
    getUserProfile().then(response => {
      user.value = response.data
    })
  }

  function submit(ref) {
    proxy.$refs.form.validate().then(res => {
      updateUserProfile(user.value).then(response => {
        proxy.$modal.msgSuccess("修改成功")
      })
    })
  }

  onReady(() => {
    proxy.$refs.form.setRules(rules.value)
  })

  getUser()
</script>

<style lang="scss" scoped>
  page {
    background-color: #ffffff;
  }

  .example {
    padding: 15px;
    background-color: #fff;
  }

  .segmented-control {
    margin-bottom: 15px;
  }

  .button-group {
    margin-top: 15px;
    display: flex;
    justify-content: space-around;
  }

  .form-item {
    display: flex;
    align-items: center;
    flex: 1;
  }

  .button {
    display: flex;
    align-items: center;
    height: 35px;
    line-height: 35px;
    margin-left: 10px;
  }
</style>
