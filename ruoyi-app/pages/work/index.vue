<template>
  <view class="work-container elder-large">
    <view class="hero">
      <text class="hero-title">上传老照片</text>
      <text class="hero-sub">交给社区学生志愿者帮您焕然一新</text>
    </view>

    <!-- 1. 上传照片 -->
    <view class="card">
      <view class="section-title">1. 请选择要修复的照片（最多5张）</view>
      <view class="upload-list" v-if="imageFiles.length > 0">
        <view class="upload-item" v-for="(img, idx) in imageFiles" :key="idx">
          <image :src="img" mode="aspectFill"></image>
          <view class="remove-btn" @tap="removeImage(idx)">×</view>
        </view>
        <view class="upload-item upload-add" v-if="imageFiles.length < 5" @tap="chooseImages">
          <text class="add-icon">+</text>
        </view>
      </view>
      <button class="btn btn-upload" v-if="imageFiles.length === 0" @click="chooseImages">点击这里打开相册/相机</button>
      <text class="upload-count" v-if="imageFiles.length > 0">已选 {{ imageFiles.length }}/5 张</text>
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
      <!-- 视频结果 -->
      <view v-if="resultVideoUrl" class="video-section">
        <view class="section-title" style="font-size:30rpx;margin-top:30rpx;">修复动态视频</view>
        <video :src="resultVideoUrl" class="result-video" controls autoplay loop></video>
        <view class="btn btn-download action-btn" style="margin-top:16rpx;" @tap="downloadResultVideo">下载视频到手机</view>
      </view>
      <view class="result-actions">
        <view class="btn btn-refresh action-btn" :class="isRefreshing ? 'is-disabled' : ''" @tap="handleRefreshClick">
          <text v-if="isRefreshing" class="refresh-spin">⟳</text>
          <text>{{ isRefreshing ? '刷新中...' : '刷新处理结果' }}</text>
        </view>
        <view class="btn btn-save action-btn" :class="!resultImageUrl ? 'is-disabled' : ''" @tap="saveResultImage">保存照片</view>
      </view>
    </view>
  </view>
</template>

<script>
import request from '@/utils/request'
import { uploadSourceFile } from '@/api/repair/task'
import config from '@/config'

export default {
  data() {
    return {
      imageFiles: [],
      aiPrompt: '',
      isSubmitting: false,
      isRefreshing: false,
      latestTaskId: null,
      resultImageUrl: '',
      resultVideoUrl: '',
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
      const remaining = 5 - this.imageFiles.length
      if (remaining <= 0) return
      uni.chooseImage({
        count: remaining,
        sizeType: ['original', 'compressed'],
        sourceType: ['album', 'camera'],
        success: (res) => {
          this.imageFiles = this.imageFiles.concat(res.tempFilePaths).slice(0, 5)
        }
      })
    },
    removeImage(idx) {
      this.imageFiles.splice(idx, 1)
    },
    async submitTask() {
      if (this.imageFiles.length === 0) {
        this.$modal.msgError("请先上传待修复的照片")
        return
      }
      this.isSubmitting = true
      this.$modal.loading("上传图片中...")

      try {
        // 串行上传所有图片
        const fileNames = []
        for (let i = 0; i < this.imageFiles.length; i++) {
          this.$modal.loading(`上传图片 ${i + 1}/${this.imageFiles.length}...`)
          const uploadRes = await uploadSourceFile(this.imageFiles[i])
          fileNames.push(uploadRes.fileName)
        }
        const sourceUrls = fileNames.join(',')

        this.$modal.loading("任务排队中...")
        // 提交修复任务
        const res = await request({
          url: '/app/repair/task/submit', 
          method: 'post',
          data: {
            repairMode: 'MANUAL',
            taskType: 'MANUAL',
            sourceType: 'IMAGE',
            sourceUrls: sourceUrls,
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
        this.resultVideoUrl = ''
        this.resultText = '暂无历史任务，先提交一张照片开始修复。'
        if (showToast) {
          this.$modal.msgError('暂无历史任务，先提交照片')
        }
        return
      }

      const latest = res.rows[0]
      this.latestTaskId = latest.taskId
      if (latest.resultVideoUrl) {
        const v = latest.resultVideoUrl
        this.resultVideoUrl = v.startsWith('http') ? v : config.baseUrl + v
      } else {
        this.resultVideoUrl = ''
      }
      if (latest.resultUrls) {
        const first = String(latest.resultUrls).split(',')[0]
        this.resultImageUrl = first.startsWith('http') ? first : config.baseUrl + first
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
    downloadResultVideo() {
      if (!this.resultVideoUrl) return
      this.$modal.loading('正在下载视频...')
      uni.downloadFile({
        url: this.resultVideoUrl,
        success: (res) => {
          if (res.statusCode === 200) {
            uni.saveVideoToPhotosAlbum({
              filePath: res.tempFilePath,
              success: () => {
                this.$modal.closeLoading()
                this.$modal.msgSuccess('视频已保存到相册')
              },
              fail: () => {
                this.$modal.closeLoading()
                this.$modal.msgError('保存失败，请授权相册权限')
              }
            })
          } else {
            this.$modal.closeLoading()
            this.$modal.msgError('视频下载失败')
          }
        },
        fail: () => {
          this.$modal.closeLoading()
          this.$modal.msgError('视频下载失败，请检查网络')
        }
      })
    },
    saveResultImage() {
      if (!this.resultImageUrl) {
        this.$modal.msgError('暂无可保存图片')
        return
      }
      this.$modal.loading('正在保存图片...')
      uni.downloadFile({
        url: this.resultImageUrl,
        success: (res) => {
          if (res.statusCode === 200) {
            uni.saveImageToPhotosAlbum({
              filePath: res.tempFilePath,
              success: () => {
                this.$modal.closeLoading()
                this.$modal.msgSuccess('已保存到相册')
              },
              fail: () => {
                this.$modal.closeLoading()
                this.$modal.msgError('保存失败，请授权相册权限')
              }
            })
          } else {
            this.$modal.closeLoading()
            this.$modal.msgError('图片下载失败')
          }
        },
        fail: () => {
          this.$modal.closeLoading()
          this.$modal.msgError('图片下载失败，请检查网络')
        }
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
.upload-count { font-size: 26rpx; color: #8a9a97; margin-top: 10rpx; display: block; }
.upload-list {
  display: flex; flex-wrap: wrap; margin-bottom: 20rpx;
  .upload-item {
    position: relative; width: 180rpx; height: 180rpx; margin-right: 20rpx; margin-bottom: 20rpx; border-radius: 16rpx; overflow: hidden; background: #eee;
    image { width: 100%; height: 100%; }
  }
  .remove-btn {
    position: absolute; top: 4rpx; right: 4rpx; width: 44rpx; height: 44rpx; background: rgba(0,0,0,0.5);
    color: #fff; font-size: 32rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; line-height: 1;
  }
  .upload-add {
    background: #f0f5f4; border: 4rpx dashed #3eb49f; display: flex; align-items: center; justify-content: center;
    .add-icon { font-size: 64rpx; color: #3eb49f; line-height: 1; }
  }
}
.video-section { margin-top: 10rpx; }
.result-video { width: 100%; height: 340rpx; border-radius: 16rpx; background: #000; }
.btn-download {
  background: #e8f5f2; color: #3eb49f; border: 2rpx solid #bfe6de;
  height: 84rpx; line-height: 84rpx; border-radius: 14rpx; font-size: 30rpx; text-align: center;
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