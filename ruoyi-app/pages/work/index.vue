<template>
  <view class="work-page">
    <!-- 顶部品牌栏 -->
    <view class="page-header">
      <view class="header-circle header-circle--1"></view>
      <view class="header-circle header-circle--2"></view>
      <view class="header-inner">
        <text class="page-title">上传老照片</text>
        <text class="page-sub">交给志愿者帮您焕然一新</text>
      </view>
    </view>

    <!-- 主内容区 -->
    <view class="page-body">

      <!-- 步骤一：上传照片 -->
      <view class="step-card">
        <view class="step-label">
          <view class="step-num">1</view>
          <text class="step-text">选择要修复的照片（最多5张）</text>
        </view>
        <view class="upload-grid" v-if="imageFiles.length > 0">
          <view class="upload-thumb" v-for="(img, idx) in imageFiles" :key="idx">
            <image :src="img" mode="aspectFill" class="thumb-img"></image>
            <view class="thumb-del" @tap="removeImage(idx)">✕</view>
          </view>
          <view class="upload-thumb upload-add" v-if="imageFiles.length < 5" @tap="chooseImages">
            <text class="add-plus">+</text>
          </view>
        </view>
        <button class="btn-upload" v-if="imageFiles.length === 0" @click="chooseImages">
          <text class="upload-icon">📷</text> 打开相册 / 相机
        </button>
        <text class="upload-hint" v-if="imageFiles.length > 0">已选 {{ imageFiles.length }} / 5 张</text>
      </view>

      <!-- 步骤二：留言 -->
      <view class="step-card">
        <view class="step-label">
          <view class="step-num">2</view>
          <view>
            <text class="step-text">留言给志愿者</text>
            <text class="step-sub">描述越清楚，效果越好</text>
          </view>
        </view>
        <textarea
          v-model="aiPrompt"
          class="prompt-textarea"
          placeholder="例如：照片有划痕，请帮修复；黑白照片想要变彩色..."
          maxlength="200"
        ></textarea>
      </view>

      <!-- 提交按钮 -->
      <button class="btn-submit" @click="submitTask" :loading="isSubmitting" :disabled="isSubmitting">
        {{ submitBtnText }}
      </button>

      <!-- 步骤三：接收结果 -->
      <view class="step-card result-card">
        <view class="result-header">
          <view class="step-label" style="margin-bottom: 0;">
            <view class="step-num">3</view>
            <text class="step-text">接收修复好的照片</text>
          </view>
          <view class="all-history-link" @tap="goToHistory">全部记录</view>
        </view>

        <!-- 有图结果 -->
        <view class="result-list" v-if="resultImageUrls.length > 0">
          <view class="result-item-block" v-for="(img, idx) in resultImageUrls" :key="idx" @tap="previewResultImage(idx)">
            <text class="result-item-label">修复照片 {{ idx + 1 }}</text>
            <image :src="img" mode="aspectFill" class="result-img-full"></image>
          </view>
        </view>
        <view class="result-empty" v-else>
          <text class="result-empty-icon">🖼️</text>
          <text class="result-empty-text">{{ resultText }}</text>
        </view>

        <!-- 视频结果 -->
        <view class="video-section" v-if="resultVideoUrl">
          <text class="video-label">修复动态视频</text>
          <video :src="resultVideoUrl" class="result-video" controls autoplay loop></video>
          <view class="btn-secondary" style="margin-top:16rpx;" @tap="downloadResultVideo">下载视频到手机</view>
        </view>

        <!-- 操作按钮组 -->
        <view class="result-actions">
          <view class="btn-action btn-refresh" :class="isRefreshing ? 'is-disabled' : ''" @tap="handleRefreshClick">
            <text v-if="isRefreshing" class="spin">⟳</text>
            {{ isRefreshing ? '刷新中...' : '刷新处理结果' }}
          </view>
          <view class="btn-action btn-save" :class="resultImageUrls.length === 0 ? 'is-disabled' : ''" @tap="saveResultImage">
            保存照片
          </view>
        </view>
      </view>

    </view>
  </view>
</template>

<script>
import request from '@/utils/request'
import { uploadSourceFile, submitRepairTask } from '@/api/repair/task'
import config from '@/config'

const SUBMIT_DELAY_MS = 600
const UPLOAD_RETRY_DELAY_MS = 1200
const DOWNLOAD_TIMEOUT_MS = 45000

export default {
  data() {
    return {
      imageFiles: [],
      aiPrompt: '',
      isSubmitting: false,
      isRefreshing: false,
      latestTaskId: null,
      resultImageUrls: [],
      resultVideoUrl: '',
      resultText: '提交任务后，点击“刷新处理结果”可查看修复完成的图片。'
    }
  },
  computed: {
    submitBtnText() {
      return '提交给志愿者'
    }
  },
  onShow() {
    this.refreshLatestResult()
  },
  methods: {
    resolveRequestError(error, fallback = '网络异常，请稍后重试') {
      if (typeof error === 'string') {
        if (error === '500') return '服务器繁忙，请稍后再试'
        if (error.includes('无效的会话')) return '登录状态已过期，请重新登录'
        if (error.includes('### Error')) return '后端数据库异常，请联系管理员检查云端数据库表结构'
        return error
      }
      const rawMsg = (error && (error.message || error.errMsg)) || ''
      if (!rawMsg) {
        return fallback
      }
      if (rawMsg.includes('timeout') || rawMsg.includes('timed out') || rawMsg.includes('socket')) {
        return '系统接口请求超时，请稍后再试'
      }
      if (rawMsg.includes('ECONNREFUSED') || rawMsg.includes('Network Error') || rawMsg.includes('request:fail') || rawMsg.includes('Failed to fetch') || rawMsg.includes('interrupted') || rawMsg.includes('abort')) {
        return '后端接口连接异常，请确认后端服务已启动'
      }
      return fallback
    },
    isWeakNetworkError(error) {
      const rawMsg = typeof error === 'string' ? error : (error && (error.message || error.errMsg)) || ''
      return rawMsg.includes('timeout') || rawMsg.includes('timed out') || rawMsg.includes('socket') || rawMsg.includes('request:fail') || rawMsg.includes('Network Error') || rawMsg.includes('interrupted') || rawMsg.includes('abort')
    },
    sleep(ms) {
      return new Promise(resolve => setTimeout(resolve, ms))
    },
    normalizeResourceUrl(rawUrl) {
      const value = String(rawUrl || '').trim()
      if (!value) {
        return ''
      }
      if (value.startsWith('http://tmp/') || value.startsWith('wxfile://')) {
        return ''
      }
      return value.startsWith('http') ? value : config.staticUrl + value
    },
    parseResourceUrlList(urls) {
      return String(urls || '')
        .split(',')
        .map(item => this.normalizeResourceUrl(item))
        .filter(item => !!item)
    },
    parseUploadFileNames(uploadRes) {
      if (!uploadRes || typeof uploadRes !== 'object') {
        return []
      }
      const values = []
      if (uploadRes.fileName) {
        values.push(uploadRes.fileName)
      }
      if (uploadRes.fileNames) {
        values.push(...String(uploadRes.fileNames).split(','))
      }
      if (uploadRes.url && !uploadRes.fileName && !uploadRes.fileNames) {
        const url = String(uploadRes.url)
        const pathMatch = url.match(/^https?:\/\/[^/]+(\/.*)$/)
        values.push(pathMatch ? pathMatch[1] : url)
      }
      return values.map(item => String(item || '').trim()).filter(item => !!item)
    },
    async uploadWithRetry(filePath, maxRetry = 1) {
      let lastErr = null
      for (let attempt = 0; attempt <= maxRetry; attempt++) {
        try {
          return await uploadSourceFile(filePath)
        } catch (error) {
          lastErr = error
          if (attempt >= maxRetry || !this.isWeakNetworkError(error)) {
            throw error
          }
          await this.sleep(UPLOAD_RETRY_DELAY_MS * (attempt + 1))
        }
      }
      throw lastErr || '上传失败'
    },
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
      const doChoose = () => {
        uni.chooseImage({
          count: remaining,
          sizeType: ['compressed'],
          sourceType: ['album', 'camera'],
          success: (res) => {
            this.imageFiles = this.imageFiles.concat(res.tempFilePaths).slice(0, 5)
          },
          fail: (err) => {
            console.error('chooseImage fail:', JSON.stringify(err))
            const msg = (err && err.errMsg) || ''
            if (msg.indexOf('cancel') === -1) {
              this.$modal.msgError('选择图片失败，请检查相册权限')
            }
          }
        })
      }
      // #ifdef MP-WEIXIN
      if (wx.requirePrivacyAuthorize) {
        wx.requirePrivacyAuthorize({
          success: () => doChoose(),
          fail: () => {
            console.error('requirePrivacyAuthorize fail')
            this.$modal.msgError('请先同意隐私协议后再选择图片')
          }
        })
      } else {
        doChoose()
      }
      // #endif
      // #ifndef MP-WEIXIN
      doChoose()
      // #endif
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
      this.$modal.loading("正在提交任务...")

      try {
        // 串行上传所有图片
        const fileNames = []
        for (let i = 0; i < this.imageFiles.length; i++) {
          const uploadRes = await this.uploadWithRetry(this.imageFiles[i], 1)
          const uploadedFiles = this.parseUploadFileNames(uploadRes)
          if (uploadedFiles.length === 0) {
            throw new Error('上传成功但未返回文件地址')
          }
          fileNames.push(...uploadedFiles)
        }
        const sourceUrls = fileNames.slice(0, 5).join(',')
        await this.sleep(SUBMIT_DELAY_MS)

        // 提交修复任务
        const res = await submitRepairTask({
          repairMode: 'MANUAL',
          taskType: 'MANUAL',
          sourceType: 'IMAGE',
          sourceUrls: sourceUrls,
          remark: this.aiPrompt
        })
        if (res.code === 200) {
          this.$modal.msgSuccess("排队成功！可前往您的个人中心查看进度~")
          this.latestTaskId = res.taskId || null
          this.imageFiles = []
          this.aiPrompt = ''
          this.resultImageUrls = []
          this.resultText = '任务已提交，正在处理中，请稍后点击“刷新处理结果”。'
        } else {
          this.$modal.msgError(res.msg || "提交失败")
        }
      } catch (e) {
        this.$modal.msgError(this.resolveRequestError(e, '提交失败，请检查后端服务或网络配置'))
      } finally {
        this.isSubmitting = false
        this.$modal.closeLoading()
      }
    },
    async refreshLatestResult(showToast = false) {
      let res = null
      try {
        res = await request({
          url: '/app/repair/task/list',
          method: 'get',
          params: {
            pageNum: 1,
            pageSize: 1
          }
        })
      } catch (e) {
        const errMsg = this.resolveRequestError(e, '刷新失败，请稍后重试')
        this.resultImageUrls = []
        this.resultVideoUrl = ''
        this.resultText = errMsg
        if (showToast) {
          this.$modal.msgError(errMsg)
        }
        return
      }

      if (!res || !res.rows || res.rows.length === 0) {
        this.resultImageUrls = []
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
        this.resultVideoUrl = v.startsWith('http') ? v : config.staticUrl + v
      } else {
        this.resultVideoUrl = ''
      }
      if (latest.resultUrls) {
        this.resultImageUrls = this.parseResourceUrlList(latest.resultUrls)
        this.resultText = this.resultImageUrls.length > 0 ? `修复已完成，共 ${this.resultImageUrls.length} 张，可预览并保存。` : '修复已完成，结果文件暂不可预览。'
        if (showToast) {
          this.$modal.msgSuccess('已刷新，修复结果可查看')
        }
      } else {
        this.resultImageUrls = []
        const progress = latest.progress || 0
        this.resultText = `正在处理中，当前进度 ${progress}%`
        if (showToast) {
          this.$modal.msgSuccess(`已刷新，当前进度 ${progress}%`)
        }
      }
    },
    previewResultImage(index) {
      if (!this.resultImageUrls || this.resultImageUrls.length === 0) {
        return
      }
      uni.previewImage({
        current: this.resultImageUrls[index] || this.resultImageUrls[0],
        urls: this.resultImageUrls
      })
    },
    downloadAndSaveImage(url) {
      return new Promise((resolve, reject) => {
        uni.downloadFile({
          url: encodeURI(url),
          timeout: DOWNLOAD_TIMEOUT_MS,
          success: (res) => {
            if (res.statusCode !== 200) {
              reject(new Error('图片下载失败'))
              return
            }
            uni.saveImageToPhotosAlbum({
              filePath: res.tempFilePath,
              success: () => resolve(),
              fail: () => reject(new Error('保存失败，请授权相册权限'))
            })
          },
          fail: (error) => reject(error)
        })
      })
    },
    downloadResultVideo() {
      if (!this.resultVideoUrl) return
      this.$modal.loading('正在下载视频...')
      uni.downloadFile({
        url: encodeURI(this.resultVideoUrl),
        timeout: DOWNLOAD_TIMEOUT_MS,
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
        fail: (error) => {
          this.$modal.closeLoading()
          this.$modal.msgError(this.resolveRequestError(error, '视频下载失败，请检查网络'))
        }
      })
    },
    goToHistory() {
      this.$tab.navigateTo('/pages/work/history')
    },
    async saveResultImage() {
      if (!this.resultImageUrls || this.resultImageUrls.length === 0) {
        this.$modal.msgError('暂无可保存图片')
        return
      }
      this.$modal.loading('正在批量保存图片...')
      let successCount = 0
      for (let i = 0; i < this.resultImageUrls.length; i++) {
        try {
          await this.downloadAndSaveImage(this.resultImageUrls[i])
          successCount++
        } catch (error) {
          console.error('saveResultImage failed:', error)
        }
      }
      this.$modal.closeLoading()
      if (successCount === this.resultImageUrls.length) {
        this.$modal.msgSuccess(`已保存 ${successCount} 张图片到相册`)
      } else if (successCount > 0) {
        this.$modal.msg(`已保存 ${successCount}/${this.resultImageUrls.length} 张，部分图片保存失败`)
      } else {
        this.$modal.msgError('保存失败，请检查网络或相册权限')
      }
    }
  }
}
</script>

<style lang="scss">
page {
  background: #f0f7f5;
}

/* ====== 页面顶部 ====== */
.work-page {
  min-height: 100vh;
  background: #f0f7f5;
}

.page-header {
  position: relative;
  background: linear-gradient(135deg, #1f7f6f 0%, #3eb49f 100%);
  padding: 70rpx 40rpx 60rpx;
  overflow: hidden;
}

.header-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}
.header-circle--1 {
  width: 280rpx;
  height: 280rpx;
  top: -70rpx;
  right: -50rpx;
}
.header-circle--2 {
  width: 200rpx;
  height: 200rpx;
  bottom: -50rpx;
  left: -30rpx;
}

.header-inner {
  position: relative;
  z-index: 1;
}

.page-title {
  display: block;
  font-size: 50rpx;
  font-weight: bold;
  color: #fff;
  margin-bottom: 12rpx;
}

.page-sub {
  display: block;
  font-size: 28rpx;
  color: rgba(255, 255, 255, 0.8);
}

/* ====== 主内容区 ====== */
.page-body {
  padding: 30rpx 30rpx 80rpx;
  margin-top: -20rpx;
}

/* ====== 步骤卡片 ====== */
.step-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 40rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.04);
}

.step-label {
  display: flex;
  align-items: flex-start;
  gap: 20rpx;
  margin-bottom: 30rpx;
}

.step-num {
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #4dc4b0, #3eb49f);
  color: #fff;
  font-size: 28rpx;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.step-text {
  font-size: 38rpx;
  font-weight: bold;
  color: #2b3d3a;
  line-height: 52rpx;
}

.step-sub {
  display: block;
  font-size: 28rpx;
  color: #8a9a97;
  margin-top: 4rpx;
}

/* ====== 上传区 ====== */
.upload-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
  margin-bottom: 16rpx;
}

.upload-thumb {
  width: calc(50% - 10rpx);
  aspect-ratio: 1 / 1;
  border-radius: 20rpx;
  overflow: hidden;
  position: relative;
  background: #eef5f3;
}

.thumb-img {
  width: 100%;
  height: 100%;
}

.thumb-del {
  position: absolute;
  top: 6rpx;
  right: 6rpx;
  width: 44rpx;
  height: 44rpx;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  font-size: 26rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-add {
  border: 4rpx dashed #3eb49f;
  display: flex;
  align-items: center;
  justify-content: center;
}

.add-plus {
  font-size: 68rpx;
  color: #3eb49f;
  line-height: 1;
}

.btn-upload {
  width: 100%;
  background: #f0f5f4;
  color: #3eb49f;
  border: 4rpx dashed #3eb49f;
  height: 110rpx;
  line-height: 110rpx;
  font-size: 36rpx;
  border-radius: 20rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;

  &::after {
    border: none;
  }
}

.upload-icon {
  font-size: 40rpx;
}

.upload-hint {
  font-size: 30rpx;
  color: #8a9a97;
  display: block;
  text-align: center;
  margin-top: 10rpx;
}

/* ====== 留言框 ====== */
.prompt-textarea {
  width: 100%;
  height: 220rpx;
  background: #f8fbfb;
  border-radius: 16rpx;
  padding: 26rpx;
  font-size: 32rpx;
  box-sizing: border-box;
  border: 2rpx solid #eee;
  color: #333;
  line-height: 1.6;
}

/* ====== 提交按钮 ====== */
.btn-submit {
  width: 100%;
  background: linear-gradient(90deg, #4dc4b0, #3eb49f);
  color: #fff;
  height: 110rpx;
  line-height: 110rpx;
  font-size: 40rpx;
  font-weight: bold;
  border-radius: 55rpx;
  box-shadow: 0 10rpx 24rpx rgba(62, 180, 159, 0.35);
  margin-bottom: 30rpx;

  &::after {
    border: none;
  }

  &:active {
    transform: translateY(2rpx);
  }
}

/* ====== 结果卡片 ====== */
.result-card {
  .step-label {
    margin-bottom: 0;
  }
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30rpx;
}

.all-history-link {
  font-size: 28rpx;
  color: #3eb49f;
  font-weight: 500;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 32rpx;
  margin-bottom: 20rpx;
}

.result-item-block {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.result-item-label {
  font-size: 28rpx;
  font-weight: 600;
  color: #2e8a6e;
}

.result-img-full {
  width: 100%;
  height: 480rpx;
  border-radius: 20rpx;
  border: 4rpx solid #3eb49f;
  box-shadow: 0 6rpx 20rpx rgba(62, 180, 159, 0.22);
  display: block;
}

.result-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
}

.preview-icon {
  font-size: 40rpx;
}

.result-empty {
  width: 100%;
  min-height: 120rpx;
  background: #f8fbfb;
  border: 2rpx dashed #d9e7e4;
  border-radius: 16rpx;
  padding: 24rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 20rpx;
}

.result-empty-icon {
  font-size: 48rpx;
}

.result-empty-text {
  font-size: 32rpx;
  color: #6f7f7c;
  text-align: center;
  line-height: 1.7;
}

/* 视频 */
.video-section {
  margin-top: 20rpx;
  margin-bottom: 20rpx;
}

.video-label {
  font-size: 30rpx;
  font-weight: bold;
  color: #2b3d3a;
  margin-bottom: 16rpx;
  display: block;
}

.result-video {
  width: 100%;
  height: 340rpx;
  border-radius: 16rpx;
  background: #000;
}

.btn-secondary {
  background: #e8f5f2;
  color: #3eb49f;
  border: 2rpx solid #bfe6de;
  height: 84rpx;
  line-height: 84rpx;
  border-radius: 14rpx;
  font-size: 30rpx;
  text-align: center;
}

/* 操作按钮组 */
.result-actions {
  display: flex;
  gap: 16rpx;
  margin-top: 20rpx;
}

.btn-action {
  flex: 1;
  height: 96rpx;
  line-height: 96rpx;
  border-radius: 16rpx;
  font-size: 32rpx;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  font-weight: 600;
}

.btn-refresh {
  background: #eef7f5;
  color: #3eb49f;
  border: 2rpx solid #bfe6de;

  &.is-disabled {
    opacity: 0.7;
  }
}

.btn-save {
  background: #f1f3f4;
  color: #5f7d79;
  border: 2rpx solid #dde5e2;

  &.is-disabled {
    opacity: 0.6;
  }
}

.spin {
  display: inline-block;
  animation: spin-anim 0.9s linear infinite;
}

@keyframes spin-anim {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}
</style>