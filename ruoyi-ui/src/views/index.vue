<template>
  <div class="dashboard-page">
    <!-- Hero 横幅 -->
    <div class="hero">
      <div class="hero-text">
        <h2>e起守忆 · 志愿服务看板</h2>
        <p>{{ heroSubtitle }}</p>
      </div>
      <el-button type="primary" plain @click="refreshData" :loading="loading" size="large">刷新数据</el-button>
    </div>

    <!-- KPI 卡片 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :xs="12" :sm="12" :md="6" v-for="card in kpiCards" :key="card.key">
        <div class="kpi-card" :style="{ borderTop: '4px solid ' + card.color }">
          <div class="kpi-top">
            <span class="kpi-icon" :style="{ background: card.color + '1a', color: card.color }">{{ card.icon }}</span>
            <span class="kpi-label">{{ card.label }}</span>
          </div>
          <div class="kpi-value" :style="{ color: card.color }">{{ card.value }}</div>
          <div class="kpi-sub">{{ card.sub }}</div>
        </div>
      </el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="16" class="panel-row">
      <el-col :xs="24" :lg="10">
        <div class="panel">
          <div class="panel-title">📊 任务状态分布</div>
          <div ref="pieRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="14">
        <div class="panel">
          <div class="panel-title">📈 近 7 天服务趋势</div>
          <div ref="barRef" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 最近任务 -->
    <div class="panel table-panel">
      <div class="panel-title">🗂️ 最近服务记录</div>
      <el-table :data="recentTasks" v-loading="loading" stripe>
        <el-table-column prop="taskNo" label="任务编号" min-width="180" />
        <el-table-column prop="userName" label="提交老人" width="110" align="center" />
        <el-table-column label="状态" width="130" align="center">
          <template #default="scope">
            <el-tag :type="statusTagType(scope.row.status)" size="large">{{ mapStatus(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="160" align="center">
          <template #default="scope">
            <el-progress :percentage="scope.row.progress || 0" :stroke-width="10" />
          </template>
        </el-table-column>
        <el-table-column prop="studentName" label="认领志愿者" width="110" align="center" />
        <el-table-column label="提交时间" width="170" align="center">
          <template #default="scope">{{ formatTime(scope.row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup name="Dashboard">
import { computed, reactive, ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { listRepairTask, getRepairTrend } from '@/api/repair/task'
import useUserStore from '@/store/modules/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.roles.includes('admin'))
const isStudent = computed(() => userStore.roles.includes('repair_student'))

const heroSubtitle = computed(() => {
  if (isAdmin.value) return '社区老人照片修复服务运营总览（管理员）'
  if (isStudent.value) return '我的志愿修复任务概览'
  return '我的服务任务概览'
})

const loading = ref(false)
const taskRows = ref([])
const trendRows = ref([])
const pieRef = ref(null)
const barRef = ref(null)
let pieChart = null
let barChart = null

const stats = reactive({ total: 0, completed: 0, failed: 0, waiting: 0, processing: 0 })

const completionRate = computed(() => {
  if (!stats.total) return 0
  return Math.round((stats.completed / stats.total) * 100)
})

const kpiCards = computed(() => [
  { key: 'total',      icon: '📋', label: '累计服务任务', value: stats.total,      sub: '老人累计提交次数', color: '#3eb49f' },
  { key: 'waiting',   icon: '⏳', label: '待认领任务',   value: stats.waiting,    sub: '等待志愿者认领',   color: '#f0ad4e' },
  { key: 'processing',icon: '🔧', label: '修复进行中',   value: stats.processing,  sub: '志愿者正在处理',   color: '#4d9ed8' },
  { key: 'rate',      icon: '✅', label: '服务完成率',   value: completionRate.value + '%', sub: '已修复 / 总任务', color: '#1f7f6f' },
])

const statusLabelMap = {
  WAIT_STUDENT: '待认领',
  MANUAL_PROCESSING: '修复中',
  COMPLETED: '已完成',
  FAILED: '未完成'
}

function mapStatus(status) { return statusLabelMap[status] || status }

function statusTagType(status) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'MANUAL_PROCESSING') return 'warning'
  if (status === 'FAILED') return 'danger'
  return 'info'
}

function formatTime(v) {
  if (!v) return '-'
  return String(v).replace('T', ' ').slice(0, 16)
}

const recentTasks = computed(() => taskRows.value.slice(0, 8))

function calcStats(rows) {
  stats.total      = rows.length
  stats.completed  = rows.filter(i => i.status === 'COMPLETED').length
  stats.failed     = rows.filter(i => i.status === 'FAILED').length
  stats.waiting    = rows.filter(i => i.status === 'WAIT_STUDENT').length
  stats.processing = rows.filter(i => i.status === 'MANUAL_PROCESSING').length
}

function renderPie() {
  if (!pieRef.value) return
  if (!pieChart) pieChart = echarts.init(pieRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 件 ({d}%)' },
    legend: { bottom: 0, left: 'center', itemGap: 20, textStyle: { fontSize: 13 } },
    series: [{
      type: 'pie',
      radius: ['38%', '68%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      label: { show: true, fontSize: 13, formatter: '{b}\n{c}件' },
      data: [
        { name: '待认领',  value: stats.waiting,    itemStyle: { color: '#f0ad4e' } },
        { name: '修复中',  value: stats.processing,  itemStyle: { color: '#4d9ed8' } },
        { name: '已完成',  value: stats.completed,   itemStyle: { color: '#1f7f6f' } },
        { name: '未完成',  value: stats.failed,      itemStyle: { color: '#d9534f' } },
      ].filter(i => i.value > 0)
    }]
  })
}

function renderBar() {
  if (!barRef.value) return
  if (!barChart) barChart = echarts.init(barRef.value)
  const days = trendRows.value.map(i => {
    const d = String(i.day || i.date || '').slice(5)
    return d || '-'
  })
  const totals    = trendRows.value.map(i => i.totalCount || 0)
  const completed = trendRows.value.map(i => i.completedCount || 0)

  barChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['新增任务', '完成任务'], top: 0, textStyle: { fontSize: 13 } },
    grid: { left: 40, right: 20, top: 36, bottom: 30 },
    xAxis: {
      type: 'category',
      data: days,
      axisLabel: { fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { fontSize: 12 }
    },
    series: [
      { name: '新增任务', type: 'bar', data: totals,    barMaxWidth: 36, itemStyle: { color: '#3eb49f', borderRadius: [4,4,0,0] } },
      { name: '完成任务', type: 'bar', data: completed, barMaxWidth: 36, itemStyle: { color: '#1f7f6f', borderRadius: [4,4,0,0] } }
    ]
  })
}

function buildQuery() {
  const q = { pageNum: 1, pageSize: 300 }
  if (!isAdmin.value) {
    if (isStudent.value) q.studentId = userStore.id
    else q.userId = userStore.id
  }
  return q
}

function refreshData() {
  loading.value = true
  Promise.all([listRepairTask(buildQuery()), getRepairTrend(7)])
    .then(([taskRes, trendRes]) => {
      taskRows.value = taskRes.rows || []
      calcStats(taskRows.value)
      trendRows.value = trendRes.data || []
      nextTick(() => { renderPie(); renderBar() })
    })
    .finally(() => { loading.value = false })
}

onMounted(() => { refreshData() })
onUnmounted(() => {
  pieChart?.dispose()
  barChart?.dispose()
})

window.addEventListener('resize', () => {
  pieChart?.resize()
  barChart?.resize()
})
</script>

<style lang="scss" scoped>
.dashboard-page {
  padding: 16px;
}

.hero {
  background: linear-gradient(135deg, #1a6b5e 0%, #3eb49f 100%);
  border-radius: 16px;
  padding: 22px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;

  h2 {
    margin: 0;
    font-size: 22px;
    color: #fff;
  }

  p {
    margin: 6px 0 0;
    color: rgba(255,255,255,0.8);
    font-size: 13px;
  }
}

.kpi-row {
  margin-bottom: 16px;
}

.kpi-card {
  background: #fff;
  border-radius: 14px;
  padding: 18px 16px 14px;
  box-shadow: 0 4px 18px rgba(23, 67, 62, 0.07);
  margin-bottom: 14px;
  transition: transform 0.15s;

  &:hover {
    transform: translateY(-2px);
  }
}

.kpi-top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.kpi-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.kpi-label {
  font-size: 13px;
  color: #6f8380;
  font-weight: 500;
}

.kpi-value {
  font-size: 38px;
  font-weight: 800;
  line-height: 1;
  margin-bottom: 6px;
}

.kpi-sub {
  font-size: 12px;
  color: #9aaca9;
}

.panel-row {
  margin-bottom: 16px;
}

.panel {
  background: #fff;
  border-radius: 14px;
  padding: 18px;
  box-shadow: 0 4px 18px rgba(23, 67, 62, 0.07);
  margin-bottom: 14px;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
  color: #1f4f49;
  margin-bottom: 12px;
}

.chart-box {
  width: 100%;
  height: 300px;
}

.table-panel {
  margin-bottom: 0;
}
</style>
