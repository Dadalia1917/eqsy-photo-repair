<template>
  <view class="history-container elder-large">
    <view class="history-header">
      <text class="page-title">修复记录</text>
      <text class="page-sub">查看您所有上传和修复的照片</text>
    </view>

    <!-- 加载中（首次） -->
    <view class="loading-box" v-if="isLoading && taskList.length === 0">
      <text class="loading-text">加载中...</text>
    </view>

    <!-- 空状态 -->
    <view class="empty-box" v-if="!isLoading && taskList.length === 0">
      <text class="empty-icon">📷</text>
      <text class="empty-text">暂无上传记录</text>
      <text class="empty-sub">去工作台上传您的照片吧</text>
    </view>

    <!-- 任务列表 -->
    <view class="task-list" v-if="taskList.length > 0">
      <view class="task-card" v-for="task in taskList" :key="task.taskId">
        <!-- 卡片头部：状态 + 时间 -->
        <view class="task-card-header">
          <view class="task-status-badge" :class="getStatusClass(task.status)">
            {{ getStatusText(task.status) }}
          </view>
          <text class="task-time">{{ formatTime(task.createTime) }}</text>
        </view>

        <!-- 任务编号 -->
        <view class="task-no">任务编号：{{ task.taskNo }}</view>

        <!-- 照片对比展示 -->
        <view class="photo-pairs" v-if="parseUrlList(task.sourceUrls).length > 0">
          <view
            class="photo-pair-row"
            v-for="(srcUrl, i) in parseUrlList(task.sourceUrls)"
            :key="'pair-' + i"
          >
            <text class="pair-index">第 {{ i + 1 }} 张</text>
            <view class="pair-content">
              <!-- 原始照片 -->
              <view class="pair-block" @tap="previewImages(parseUrlList(task.sourceUrls), i)">
                <text class="pair-label">原始照片</text>
                <image :src="srcUrl" mode="aspectFill" class="pair-img" />
              </view>
              <!-- 修复照片 -->
              <view class="pair-block">
                <text class="pair-label pair-label--result">修复照片</text>
                <view
                  v-if="parseUrlList(task.resultUrls)[i]"
                  @tap="previewImages(parseUrlList(task.resultUrls), i)"
                >
                  <image :src="parseUrlList(task.resultUrls)[i]" mode="aspectFill" class="pair-img pair-img--result" />
                </view>
                <view v-else class="pair-placeholder">
                  <text class="pair-placeholder-text">修复中…</text>
                </view>
              </view>
            </view>
          </view>

          <!-- 进度（未完成时）-->
          <view class="task-progress" v-if="task.status !== 'COMPLETED'">
            <text class="progress-label">处理进度：{{ task.progress || 0 }}%</text>
            <view class="progress-bar-bg">
              <view class="progress-bar-fill" :style="{ width: (task.progress || 0) + '%' }"></view>
            </view>
          </view>

          <!-- 保存按钮 -->
          <view class="save-btn" v-if="parseUrlList(task.resultUrls).length > 0" @tap="saveAllResults(task)">
            保存全部修复结果到相册
          </view>
        </view>

        <!-- 备注留言 -->
        <view class="task-remark" v-if="task.remark">
          <text class="remark-label">留言：</text>
          <text class="remark-text">{{ task.remark }}</text>
        </view>
      </view>
    </view>

    <!-- 加载更多 / 已到底 -->
    <view class="load-more-box" v-if="taskList.length > 0">
      <view
        class="load-more-btn"
        v-if="hasMore"
        @tap="loadMore"
        :class="isLoading ? 'is-loading' : ''"
      >
        {{ isLoading ? '加载中...' : '加载更多' }}
      </view>
      <text class="no-more-text" v-else>已显示全部记录</text>
    </view>
  </view>
</template>

<script>
import request from '@/utils/request'
import config from '@/config'

const DOWNLOAD_TIMEOUT_MS = 45000

export default {
  data() {
    return {
      taskList: [],
      isLoading: false,
      pageNum: 1,
      pageSize: 10,
      total: 0,
      hasMore: false
    }
  },
  onShow() {
    // 每次进入页面重新加载第一页
    this.taskList = []
    this.pageNum = 1
    this.total = 0
    this.hasMore = false
    this.loadTasks()
  },
  methods: {
    normalizeUrl(rawUrl) {
      const value = String(rawUrl || '').trim()
      if (!value) return ''
      if (value.startsWith('http://tmp/') || value.startsWith('wxfile://')) return ''
      return value.startsWith('http') ? value : config.staticUrl + value
    },
    parseUrlList(urls) {
      return String(urls || '')
        .split(',')
        .map(u => this.normalizeUrl(u))
        .filter(u => !!u)
    },
    getStatusText(status) {
      const map = {
        WAIT_STUDENT: '等待志愿者认领',
        MANUAL_PROCESSING: '志愿者修复中',
        COMPLETED: '修复完成'
      }
      return map[status] || status
    },
    getStatusClass(status) {
      if (status === 'COMPLETED') return 'status-done'
      if (status === 'MANUAL_PROCESSING') return 'status-processing'
      return 'status-waiting'
    },
    formatTime(time) {
      if (!time) return ''
      const normalized = String(time).replace(/-/g, '/').replace('T', ' ')
      const d = new Date(normalized)
      const pad = n => String(n).padStart(2, '0')
      return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
    },
    async loadTasks() {
      if (this.isLoading) return
      this.isLoading = true
      try {
        const res = await request({
          url: '/app/repair/task/list',
          method: 'get',
          params: {
            pageNum: this.pageNum,
            pageSize: this.pageSize
          }
        })
        if (res && res.rows) {
          if (this.pageNum === 1) {
            this.taskList = res.rows
          } else {
            this.taskList = this.taskList.concat(res.rows)
          }
          this.total = res.total || 0
          this.hasMore = this.taskList.length < this.total
        }
      } catch (e) {
        this.$modal.msgError('加载记录失败，请稍后重试')
      } finally {
        this.isLoading = false
      }
    },
    loadMore() {
      if (this.isLoading || !this.hasMore) return
      this.pageNum++
      this.loadTasks()
    },
    previewImages(urls, currentIdx) {
      if (!urls || urls.length === 0) return
      uni.previewImage({
        current: urls[currentIdx] || urls[0],
        urls: urls
      })
    },
    downloadAndSave(url) {
      return new Promise((resolve, reject) => {
        uni.downloadFile({
          url: encodeURI(url),
          timeout: DOWNLOAD_TIMEOUT_MS,
          success: (res) => {
            if (res.statusCode !== 200) {
              reject(new Error('下载失败'))
              return
            }
            uni.saveImageToPhotosAlbum({
              filePath: res.tempFilePath,
              success: () => resolve(),
              fail: () => reject(new Error('保存失败，请授权相册权限'))
            })
          },
          fail: (err) => reject(err)
        })
      })
    },
    async saveAllResults(task) {
      const urls = this.parseUrlList(task.resultUrls)
      if (urls.length === 0) {
        this.$modal.msgError('暂无可保存的修复结果')
        return
      }
      this.$modal.loading('正在保存到相册...')
      let successCount = 0
      for (const url of urls) {
        try {
          await this.downloadAndSave(url)
          successCount++
        } catch (e) {
          console.error('save fail:', e)
        }
      }
      this.$modal.closeLoading()
      if (successCount === urls.length) {
        this.$modal.msgSuccess(`已保存 ${successCount} 张图片到相册`)
      } else if (successCount > 0) {
        this.$modal.msg(`已保存 ${successCount}/${urls.length} 张，部分图片保存失败`)
      } else {
        this.$modal.msgError('保存失败，请检查网络或相册权限')
      }
    }
  }
}
</script>

<style lang="scss">
page {
  background-color: #f5f6f7;
}

.history-container {
  padding: 40rpx 30rpx;
  padding-bottom: 80rpx;
}

.history-header {
  text-align: center;
  margin-bottom: 40rpx;

  .page-title {
    display: block;
    font-size: 52rpx;
    font-weight: bold;
    color: #2b3d3a;
    margin-bottom: 14rpx;
  }

  .page-sub {
    display: block;
    font-size: 32rpx;
    color: #5f7d79;
  }
}

.loading-box,
.empty-box {
  text-align: center;
  padding: 80rpx 0;

  .loading-text {
    font-size: 32rpx;
    color: #999;
  }

  .empty-icon {
    display: block;
    font-size: 80rpx;
    margin-bottom: 20rpx;
  }

  .empty-text {
    display: block;
    font-size: 36rpx;
    color: #666;
    margin-bottom: 10rpx;
  }

  .empty-sub {
    display: block;
    font-size: 28rpx;
    color: #aaa;
  }
}

.task-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 36rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.04);
}

.task-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.task-status-badge {
  font-size: 28rpx;
  padding: 10rpx 26rpx;
  border-radius: 30rpx;
  font-weight: 600;
}

.task-status-badge.status-waiting {
  background: #fff3e0;
  color: #e65100;
}

.task-status-badge.status-processing {
  background: #e3f2fd;
  color: #1565c0;
}

.task-status-badge.status-done {
  background: #e8f5e9;
  color: #2e7d32;
}

.task-time {
  font-size: 26rpx;
  color: #888;
}

.task-no {
  font-size: 26rpx;
  color: #999;
  margin-bottom: 24rpx;
}

/* 成对对比布局 */
.photo-pairs {
  margin-top: 16rpx;
}

.photo-pair-row {
  margin-bottom: 36rpx;
  padding-bottom: 36rpx;
  border-bottom: 1rpx solid #f0f5f4;
}

.photo-pair-row:last-of-type {
  border-bottom: none;
}

.pair-index {
  display: block;
  font-size: 30rpx;
  font-weight: 600;
  color: #3eb49f;
  margin-bottom: 16rpx;
}

.pair-content {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.pair-block {
  display: flex;
  flex-direction: column;
  gap: 12rpx;
}

.pair-img {
  width: 100%;
  height: 480rpx;
  border-radius: 20rpx;
  background: #eef5f3;
  display: block;
}

.pair-img--result {
  border: 4rpx solid #3eb49f;
  box-shadow: 0 6rpx 20rpx rgba(62, 180, 159, 0.22);
}

.pair-placeholder {
  width: 100%;
  height: 200rpx;
  border-radius: 20rpx;
  background: #f0f5f4;
  border: 2rpx dashed #b0d8d0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pair-placeholder-text {
  font-size: 28rpx;
  color: #8aaea9;
}

.pair-label {
  font-size: 28rpx;
  font-weight: 600;
  color: #5f7d79;
}

.pair-label--result {
  color: #2e8a6e;
}

.task-progress {
  margin-top: 24rpx;
}

.progress-label {
  display: block;
  font-size: 30rpx;
  color: #555;
  margin-bottom: 14rpx;
  font-weight: 500;
}

.progress-bar-bg {
  height: 18rpx;
  background: #e8f5f2;
  border-radius: 9rpx;
  overflow: hidden;
}

.progress-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #4dc4b0, #3eb49f);
  border-radius: 6rpx;
  transition: width 0.3s;
}

.save-btn {
  margin-top: 24rpx;
  background: linear-gradient(90deg, #4dc4b0, #3eb49f);
  color: #fff;
  text-align: center;
  padding: 30rpx 0;
  border-radius: 18rpx;
  font-size: 34rpx;
  font-weight: bold;
  box-shadow: 0 6rpx 20rpx rgba(62, 180, 159, 0.3);
  letter-spacing: 2rpx;
}

.task-remark {
  margin-top: 20rpx;
  background: #f8fbfb;
  border-radius: 12rpx;
  padding: 20rpx;
}

.remark-label {
  font-size: 26rpx;
  color: #5f7d79;
}

.remark-text {
  font-size: 26rpx;
  color: #555;
}

.load-more-box {
  text-align: center;
  padding: 30rpx 0;
}

.load-more-btn {
  display: inline-block;
  padding: 16rpx 60rpx;
  background: #e8f5f2;
  color: #3eb49f;
  border-radius: 30rpx;
  font-size: 28rpx;
}

.load-more-btn.is-loading {
  opacity: 0.6;
}

.no-more-text {
  font-size: 26rpx;
  color: #bbb;
}
</style>
