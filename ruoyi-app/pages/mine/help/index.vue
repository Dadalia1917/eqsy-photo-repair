<template>
  <view class="help-container" :class="{ 'elder-large': elderLargeFont }">
    <view class="banner">
      <view class="banner-title">常见问题与使用帮助</view>
      <view class="banner-sub">面向社区老年人，步骤尽量简单清晰</view>
    </view>

    <view v-for="(item, findex) in list" :key="findex" :title="item.title" class="list-title">
      <view class="text-title">
        <view class="title-dot"></view>{{ item.title }}
      </view>
      <view class="childList">
        <view v-for="(child, zindex) in item.childList" :key="zindex" class="question" hover-class="hover"
          @click="handleText(child)">
          <view class="text-item">{{ child.title }}</view>
          <view class="line" v-if="zindex !== item.childList.length - 1"></view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
  import { ref, getCurrentInstance } from "vue"
  import { onShow } from '@dcloudio/uni-app'
  
  const { proxy } = getCurrentInstance()
  const elderLargeFont = ref(false)
  const voiceAssist = ref(false)

  const list = ref([{
    title: '修复流程问题',
    childList: [{
      title: '一次最多上传几张照片？',
      content: '每次最多上传5张，建议先选最清晰的照片。'
    }, {
      title: 'AI修复和人工修复有什么区别？',
      content: 'AI修复速度快，通常几分钟返回。人工修复由学生志愿者处理，质量更精细，耗时会更长。'
    }, {
      title: '照片一直在修复中怎么办？',
      content: '请在工作台点击“刷新进度”。人工修复场景下，学生会在完成后上传结果，请耐心等待。'
    }, {
      title: '上传照片有什么建议？',
      content: '请尽量避免反光、模糊和水印，建议光线均匀、人物主体完整。'
    }]
  },
  {
    title: '账号与操作问题',
    childList: [{
      title: '如何退出登录？',
      content: '请点击[我的] - [应用设置] - [退出登录]即可退出登录',
    }, {
      title: '如何修改个人信息？',
      content: '请点击[我的] - [编辑资料]，修改后保存即可。',
    }, {
      title: '忘记密码怎么办？',
      content: '请联系社区工作人员或管理员协助重置账号密码。',
    }]
  }])

  function handleText(item) {
    proxy.$tab.navigateTo(`/pages/common/textview/index?title=${item.title}&content=${item.content}`)
  }

  onShow(() => {
    elderLargeFont.value = !!uni.getStorageSync('eqsy_elder_large_font')
    voiceAssist.value = !!uni.getStorageSync('eqsy_voice_assist')
    // #ifdef H5
    if (voiceAssist.value && window.speechSynthesis) {
      const utter = new SpeechSynthesisUtterance('这里是常见问题页面，点击问题即可查看答案。')
      utter.lang = 'zh-CN'
      window.speechSynthesis.cancel()
      window.speechSynthesis.speak(utter)
    }
    // #endif
  })
</script>

<style lang="scss" scoped>
  page {
    background-color: #f8f8f8;
  }

  .help-container {
    margin-bottom: 50rpx;
    padding: 30rpx;
  }

  .banner {
    background: linear-gradient(135deg, #e9f5f1, #f7fbf9);
    border-radius: 16rpx;
    padding: 22rpx;
    margin-bottom: 24rpx;
  }

  .banner-title {
    font-size: 34rpx;
    font-weight: 700;
    color: #1f4f49;
  }

  .banner-sub {
    margin-top: 8rpx;
    font-size: 26rpx;
    color: #5a7470;
  }

  .list-title {
    margin-bottom: 30rpx;
  }

  .childList {
    background: #ffffff;
    box-shadow: 0px 0px 10rpx rgba(193, 193, 193, 0.2);
    border-radius: 16rpx;
    margin-top: 10rpx;
  }

  .line {
    width: 100%;
    height: 1rpx;
    background-color: #F5F5F5;
  }

  .text-title {
    color: #303133;
    font-size: 32rpx;
    font-weight: bold;
    margin-left: 10rpx;

    display: flex;
    align-items: center;

    .title-dot {
      width: 14rpx;
      height: 14rpx;
      border-radius: 50%;
      background: #1f7f6f;
      margin-right: 12rpx;
    }
  }

  .text-item {
    font-size: 30rpx;
    padding: 24rpx;
  }

  .question {
    color: #606266;
    font-size: 28rpx;
  }

  .help-container.elder-large {
    .banner-title {
      font-size: 40rpx;
    }

    .banner-sub,
    .text-title,
    .text-item,
    .question {
      font-size: 34rpx;
    }
  }
</style>
