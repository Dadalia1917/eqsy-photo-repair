<template>
  <view class="work-container elder-large">
    <view class="hero">
      <text class="hero-title">上传老照片</text>
      <text class="hero-sub">交给社区学生志愿者帮您焕然一新</text>
    </view>

    <!-- 1. 上传照片 -->
    <view class="card">
      <view class="section-title">1. 请选择要修复的照片</view>
      <view class="upload-list" v-if="imageFiles.length > 0">
        <view class="upload-item" v-for="(img, idx) in imageFiles" :key="idx">
          <image :src="img" mode="aspectFill"></image>
        </view>
      </view>
      <button class="btn btn-upload" @click="chooseImages">点击这里打开相册/相机</button>
    </view>

    <!-- 2. 留言框 -->
    <view class="card">
      <view class="prompt-header">
        <view class="section-title" style="margin-bottom: 8rpx;">2. 想要什么，留言</view>
        <text class="prompt-tip">描述越清楚，修复效果越贴近你的期待</text>
      </view>
      <textarea v-model="aiPrompt" class="prompt-input" placeholder="例如：我想把这张黑白照片变成彩色的... 或者：照片角被撕破了，请尽量帮我拼全..." maxlength="200"></textarea>
    </view>

    <!-- 4. 提交任务 -->
    <view class="action-card" style="margin-top: 40rpx;">
      <button class="btn btn-primary" @click="submitTask" :loading="isSubmitting" :disabled="isSubmitting">{{ submitBtnText }}</button>
    </view>

    <!-- 3. 接收修复结果 -->
    <view class="card" style="margin-top: 30rpx;">
      <view class="section-title">3. 请接收修复好的照片</view>
      <view class="result-box" v-if="resultImageUrl">
        <image :src="resultImageUrl" mode="aspectFit" class="result-image"></image>
      </view>
      <view class="result-placeholder" v-else>
        <text>{{ resultText }}</text>
      </view>
      <view class="result-actions">
        <view class="btn btn-refresh action-btn" :class="isRefreshing ? 'is-disabled' : ''" @tap="handleRefreshClick">
          <text v-if="isRefreshing" class="refresh-spin">⟳</text>
          <text>{{ isRefreshing ? '刷新中...' : '刷新处理结果' }}</text>
        </view>
        <view class="btn btn-save action-btn" :class="!resultImageUrl ? 'is-disabled' : ''" @tap="saveResultImage">保存到相册</view>
      </view>
    </view>
  </view>
</template>

<script>
import request from '@/utils/request'
import { uploadSourceFile } from '@/api/repair/task'

export default {
  data() {
    return {
      imageFiles: [],
      aiPrompt: '',
      isSubmitting: false,
      isRefreshing: false,
      latestTaskId: null,
      resultImageUrl: '',
      resultText: '提交任务后，点击“刷新处理结果”可查看修复完成的图片。'
    }
  },
  computed: {
    submitBtnText() {
      return '提交给大学生志愿者'
    }
  },
  onShow() {
    this.refreshLatestResult()
  },
  methods: {
    async handleRefreshClick() {
      if (this.isRefreshing) {
        return
      }
      this.isRefreshing = true
      try {
        await this.refreshLatestResult(true)
      } finally {
        this.isRefreshing = false
      }
    },
    chooseImages() {
      uni.chooseImage({
        count: 1, 
        sizeType: ['original', 'compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          this.imageFiles = res.tempFilePaths
        }
      })
    },
    async submitTask() {
      if (this.imageFiles.length === 0) {
        this.$modal.msgError("请先上传待修复的照片")
        return
      }
      this.isSubmitting = true
      this.$modal.loading("上传图片中...")

      try {
        // 先将图片上传到服务器，获取可访问的URL
        const uploadRes = await uploadSourceFile(this.imageFiles[0])
        const imageUrl = uploadRes.fileName

        this.$modal.loading("任务排队中...")
        // 提交修复任务
        const res = await request({
          url: '/app/repair/task/submit', 
          method: 'post',
          data: {
            repairMode: 'MANUAL',
            taskType: 'MANUAL',
            sourceType: 'IMAGE',
            sourceUrls: imageUrl,
            remark: this.aiPrompt
          }
        })
        if (res.code === 200) {
          this.$modal.msgSuccess("排队成功！可前往您的个人中心查看进度~")
          this.latestTaskId = res.taskId || null
          this.imageFiles = []
          this.aiPrompt = ''
          this.resultText = '任务已提交，正在处理中，请稍后点击“刷新处理结果”。'
        } else {
          this.$modal.msgError(res.msg || "提交失败")
        }
      } catch (e) {
        this.$modal.msgError("网络拥堵")
      } finally {
        this.isSubmitting = false
        this.$modal.closeLoading()
      }
    },
    async refreshLatestResult(showToast = false) {
      const res = await request({
        url: '/app/repair/task/list',
        method: 'get',
        params: {
          pageNum: 1,
          pageSize: 1
        }
      }).catch(() => null)

      if (!res || !res.rows || res.rows.length === 0) {
        this.resultImageUrl = ''
        this.resultText = '暂无历史任务，先提交一张照片开始修复。'
        if (showToast) {
          this.$modal.msgError('暂无历史任务，先提交照片')
        }
        return
      }

      const latest = res.rows[0]
      this.latestTaskId = latest.taskId
      if (latest.resultUrls) {
        const first = String(latest.resultUrls).split(',')[0]
        this.resultImageUrl = first
        this.resultText = '修复已完成，您可以预览并保存图片。'
        if (showToast) {
          this.$modal.msgSuccess('已刷新，修复结果可查看')
        }
      } else {
        this.resultImageUrl = ''
        const progress = latest.progress || 0
        this.resultText = `正在处理中，当前进度 ${progress}%`
        if (showToast) {
          this.$modal.msgSuccess(`已刷新，当前进度 ${progress}%`)
        }
      }
    },
    saveResultImage() {
      if (!this.resultImageUrl) {
        this.$modal.msgError('暂无可保存图片')
        return
      }
      uni.saveImageToPhotosAlbum({
        filePath: this.resultImageUrl,
        success: () => this.$modal.msgSuccess('已保存到相册'),
        fail: () => this.$modal.msgError('保存失败，请检查权限')
      })
    }
  }
}
</script>

<style lang="scss">
page { background-color: #f5f6f7; }
.work-container { padding: 40rpx 30rpx; padding-bottom: 80rpx; }
.hero {
  text-align: center; margin-bottom: 50rpx;
  .hero-title { display: block; font-size: 52rpx; font-weight: bold; color: #2b3d3a; margin-bottom: 16rpx;}
  .hero-sub { display: block; font-size: 32rpx; color: #5f7d79; }
}
.card {
  background: #fff; border-radius: 20rpx; padding: 40rpx; margin-bottom: 40rpx;
  box-shadow: 0 4rpx 20rpx rgba(0,0,0,0.03);
  .section-title { font-size: 36rpx; font-weight: bold; color: #333; margin-bottom: 30rpx; }
}
.btn-upload {
  background: #f0f5f4; color: #3eb49f; border: 4rpx dashed #3eb49f; height: 100rpx; line-height: 90rpx; font-size: 36rpx; border-radius: 16rpx;
}
.upload-list {
  display: flex; flex-wrap: wrap; margin-bottom: 30rpx;
  .upload-item { width: 180rpx; height: 180rpx; margin-right: 20rpx; margin-bottom: 20rpx; border-radius: 16rpx; overflow: hidden; background: #eee; }
  image { width: 100%; height: 100%; }
}

.prompt-header {
  margin-bottom: 10rpx;
}

.prompt-tip {
  font-size: 24rpx;
  color: #8a9a97;
}

.prompt-input {
  width: 100%; height: 220rpx; background: #f8fbfb; border-radius: 16rpx; padding: 30rpx; font-size: 32rpx; box-sizing: border-box; border: 2rpx solid #eee;
}
.btn-primary { background: linear-gradient(90deg, #4dc4b0, #3eb49f); color: #fff; height: 110rpx; line-height: 110rpx; font-size: 40rpx; border-radius: 55rpx; box-shadow: 0 10rpx 24rpx rgba(62,180,159,0.3); }
.result-box {
  width: 100%;
  height: 320rpx;
  background: #f8fbfb;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.result-image {
  width: 100%;
  height: 100%;
}
.result-placeholder {
  width: 100%;
  min-height: 120rpx;
  background: #f8fbfb;
  border: 2rpx dashed #d9e7e4;
  border-radius: 16rpx;
  padding: 24rpx;
  color: #6f7f7c;
  font-size: 28rpx;
  line-height: 1.6;
  box-sizing: border-box;
}
.result-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 20rpx;
}
.btn-refresh,
.btn-save {
  flex: 1;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 14rpx;
  font-size: 30rpx;
}
.action-btn {
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
}
.btn-refresh {
  background: #eef7f5;
  color: #3eb49f;
  border: 2rpx solid #bfe6de;
}
.btn-save {
  background: #f1f3f4;
  color: #5f7d79;
  border: 2rpx solid #dde5e2;
}
.btn-save.is-disabled {
  opacity: 0.6;
}

.btn-refresh.is-disabled {
  opacity: 0.7;
}

.refresh-spin {
  display: inline-block;
  animation: refresh-spin 0.9s linear infinite;
}

@keyframes refresh-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>