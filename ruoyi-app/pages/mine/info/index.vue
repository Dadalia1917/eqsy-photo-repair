<template>
  <view class="container">
    <uni-list>
      <uni-list-item showExtraIcon="true" :extraIcon="{type: 'locked'}" title="登录账号" :rightText="user.userName" />
      <uni-list-item showExtraIcon="true" :extraIcon="{type: 'person-filled'}" title="昵称" :rightText="user.nickName" />
      <uni-list-item showExtraIcon="true" :extraIcon="{type: 'phone-filled'}" title="手机号码" :rightText="user.phonenumber" />
      <uni-list-item showExtraIcon="true" :extraIcon="{type: 'email-filled'}" title="邮箱" :rightText="user.email" />
      <uni-list-item showExtraIcon="true" :extraIcon="{type: 'staff-filled'}" title="用户身份" :rightText="identityLabel" />
      <uni-list-item showExtraIcon="true" :extraIcon="{type: 'calendar-filled'}" title="创建日期" :rightText="user.createTime" />
    </uni-list>
  </view>
</template>

<script setup>
  import { getUserProfile } from "@/api/system/user"
  import { ref } from "vue"

  const user = ref({})
  const identityLabel = ref("")

  function resolveIdentityLabel(response) {
    const roleText = ((response && response.roleGroup) || '').trim()
    const userName = (((response || {}).data || {}).userName || '').trim()
    const createBy = (((response || {}).data || {}).createBy || '').trim()

    if (roleText.includes('admin') || roleText.includes('管理员') || userName === 'admin') {
      return '管理员'
    }

    if (createBy === 'wxLogin') {
      if (!roleText || roleText.includes('common') || roleText.includes('普通角色') || /学生|志愿者|修复/.test(roleText)) {
        return '社区用户'
      }
    }

    if (!roleText) {
      return '社区用户'
    }
    return roleText
  }

  function getUser() {
    getUserProfile().then(response => {
      user.value = response.data
      identityLabel.value = resolveIdentityLabel(response)
    })
  }

  getUser()
</script>

<style lang="scss">
  page {
    background-color: #ffffff;
  }
</style>
