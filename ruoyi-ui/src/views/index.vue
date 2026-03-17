<template>
  <div class="dashboard-page">
    <div class="hero">
      <div>
        <h2>e起守忆数据看板</h2>
        <p>面向大型社区的老人影像修复运营总览</p>
      </div>
      <el-button type="primary" @click="refreshData" :loading="loading">刷新数据</el-button>
    </div>

    <el-row :gutter="16" class="kpi-row">
      <el-col :xs="24" :sm="12" :md="6">
        <div class="kpi-card">
          <div class="kpi-label">总任务数</div>
          <div class="kpi-value">{{ stats.total }}</div>
          <div class="kpi-sub">累计提交</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="kpi-card">
          <div class="kpi-label">待认领任务</div>
          <div class="kpi-value">{{ stats.waiting }}</div>
          <div class="kpi-sub">等待学生志愿者认领</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="kpi-card">
          <div class="kpi-label">处理中任务</div>
          <div class="kpi-value">{{ stats.processing }}</div>
          <div class="kpi-sub">学生志愿者处理中</div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="kpi-card success">
          <div class="kpi-label">完成率</div>
          <div class="kpi-value">{{ completionRate }}%</div>
          <div class="kpi-sub">已完成 / 总任务</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="panel-row">
      <el-col :xs="24" :lg="10">
        <div class="panel">
          <div class="panel-title">任务状态占比</div>
          <div class="status-list">
            <div class="status-item" v-for="item in statusBars" :key="item.key">
              <div class="status-head">
                <span>{{ item.label }}</span>
                <span>{{ item.value }}</span>
              </div>
              <div class="status-track">
                <div class="status-fill" :style="{ width: item.percent + '%', background: item.color }"></div>
              </div>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="14">
        <div class="panel">
          <div class="panel-title">修复任务趋势（近7天）</div>
          <svg viewBox="0 0 760 220" class="line-chart" preserveAspectRatio="none">
            <polyline :points="trendPolylineTotal" fill="none" stroke="#1f7f6f" stroke-width="4" />
            <polyline :points="trendPolylineCompleted" fill="none" stroke="#4d9ed8" stroke-width="4" />
            <line x1="20" y1="205" x2="740" y2="205" stroke="#dfe7e6" />
          </svg>
          <div class="legend">
            <span><i class="dot green"></i>总任务趋势</span>
            <span><i class="dot blue"></i>完成任务趋势</span>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="panel table-panel">
      <div class="panel-title">最近任务</div>
      <el-table :data="recentTasks" v-loading="loading" size="small">
        <el-table-column prop="taskNo" label="任务编号" min-width="220" />
        <el-table-column prop="userName" label="提交用户" width="120" />
        <el-table-column label="模式" width="120">
          <template #default="scope">
            <el-tag type="warning">志愿者修复</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="160">
          <template #default="scope">{{ mapStatus(scope.row.status) }}</template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="100" />
        <el-table-column label="创建时间" width="180">
          <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup name="Dashboard">
import { computed, reactive, ref } from 'vue'
import { listRepairTask, getRepairTrend } from '@/api/repair/task'

const loading = ref(false)
const taskRows = ref([])
const trendRows = ref([])

const stats = reactive({
  total: 0,
  completed: 0,
  failed: 0,
  waiting: 0,
  processing: 0
})

const statusLabelMap = {
  WAIT_STUDENT: '等待认领',
  AI_QUEUE: '系统排队中',
  AI_PROCESSING: '处理中',
  MANUAL_PROCESSING: '人工处理中',
  COMPLETED: '已完成',
  FAILED: '失败'
}

const completionRate = computed(() => {
  if (!stats.total) return 0
  return Math.round((stats.completed / stats.total) * 100)
})

const statusBars = computed(() => {
  const total = stats.total || 1
  return [
    { key: 'waiting', label: '待处理', value: stats.waiting, percent: Math.round((stats.waiting / total) * 100), color: '#f0ad4e' },
    { key: 'processing', label: '处理中', value: stats.processing, percent: Math.round((stats.processing / total) * 100), color: '#4d9ed8' },
    { key: 'completed', label: '已完成', value: stats.completed, percent: Math.round((stats.completed / total) * 100), color: '#1f7f6f' },
    { key: 'failed', label: '失败', value: stats.failed, percent: Math.round((stats.failed / total) * 100), color: '#d9534f' }
  ]
})

const recentTasks = computed(() => taskRows.value.slice(0, 8))

const trendPolylineTotal = computed(() => buildPolyline(trendRows.value.map(i => i.totalCount || 0)))
const trendPolylineCompleted = computed(() => buildPolyline(trendRows.value.map(i => i.completedCount || 0)))

function mapStatus(status) {
  return statusLabelMap[status] || status
}

function formatTime(v) {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 19)
}

function calcStats(rows) {
  stats.total = rows.length
  stats.completed = rows.filter(i => i.status === 'COMPLETED').length
  stats.failed = rows.filter(i => i.status === 'FAILED').length
  stats.waiting = rows.filter(i => i.status === 'WAIT_STUDENT').length
  stats.processing = rows.filter(i => i.status === 'MANUAL_PROCESSING').length
}

function buildPolyline(arr) {
  const width = 700
  const baseX = 20
  const baseY = 205
  const chartH = 150
  const max = Math.max(...arr, 1)
  const step = arr.length > 1 ? width / (arr.length - 1) : width
  return arr
    .map((v, idx) => {
      const x = baseX + step * idx
      const y = baseY - (v / max) * chartH
      return `${x},${y}`
    })
    .join(' ')
}

function refreshData() {
  loading.value = true
  Promise.all([
    listRepairTask({ pageNum: 1, pageSize: 300 }),
    getRepairTrend(7)
  ]).then(([taskRes, trendRes]) => {
    taskRows.value = taskRes.rows || []
    calcStats(taskRows.value)
    trendRows.value = trendRes.data || []
  }).finally(() => {
    loading.value = false
  })
}

refreshData()
</script>

<style lang="scss" scoped>
.dashboard-page {
  padding: 16px;
}

.hero {
  background: linear-gradient(135deg, #e2f3ee, #f6fbf9);
  border-radius: 14px;
  padding: 18px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;

  h2 {
    margin: 0;
    color: #1f4f49;
  }

  p {
    margin: 6px 0 0;
    color: #5a7470;
  }
}

.kpi-row {
  margin-bottom: 14px;
}

.kpi-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 6px 20px rgba(23, 67, 62, 0.06);
  margin-bottom: 12px;

  .kpi-label {
    color: #6f8380;
    font-size: 13px;
  }

  .kpi-value {
    margin-top: 8px;
    font-size: 32px;
    font-weight: 700;
    color: #1f4f49;
  }

  .kpi-sub {
    margin-top: 4px;
    color: #8a9b98;
    font-size: 12px;
  }
}

.kpi-card.success .kpi-value {
  color: #1f7f6f;
}

.panel-row {
  margin-bottom: 14px;
}

.panel {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 6px 20px rgba(23, 67, 62, 0.06);
  margin-bottom: 12px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f4f49;
  margin-bottom: 14px;
}

.status-item {
  margin-bottom: 12px;
}

.status-head {
  display: flex;
  justify-content: space-between;
  color: #4f6663;
  font-size: 13px;
  margin-bottom: 6px;
}

.status-track {
  width: 100%;
  height: 8px;
  border-radius: 999px;
  background: #e9f0ef;
}

.status-fill {
  height: 100%;
  border-radius: 999px;
}

.line-chart {
  width: 100%;
  height: 220px;
}

.legend {
  display: flex;
  gap: 18px;
  font-size: 12px;
  color: #5c7270;

  .dot {
    display: inline-block;
    width: 9px;
    height: 9px;
    border-radius: 50%;
    margin-right: 6px;
  }

  .green {
    background: #1f7f6f;
  }

  .blue {
    background: #4d9ed8;
  }
}

.table-panel {
  margin-bottom: 0;
}
</style>
