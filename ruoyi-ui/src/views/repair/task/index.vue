<template>
  <div>
    <div class="page-hero">
      <div class="hero-text">
        <div class="hero-title">服务任务管理</div>
        <div class="hero-sub">查看和管理所有社区老人的照片修复任务</div>
      </div>
      <div class="hero-badge">共 {{ total }} 条任务</div>
    </div>

  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="任务编号" prop="taskNo">
        <el-input v-model="queryParams.taskNo" placeholder="请输入任务编号" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="提交用户" prop="userName">
        <el-input v-model="queryParams.userName" placeholder="请输入用户名" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="修复模式" prop="repairMode">
        <el-select v-model="queryParams.repairMode" placeholder="全部" clearable style="width: 160px">
          <el-option label="志愿者修复" value="MANUAL" />
        </el-select>
      </el-form-item>
      <el-form-item label="任务状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 180px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="taskList" stripe>
      <el-table-column label="编号" align="center" prop="taskId" width="80" />
      <el-table-column label="任务编号" align="center" prop="taskNo" width="210" />
      <el-table-column label="用户" align="center" prop="userName" width="120" />
      <el-table-column label="模式" align="center" width="100">
        <template #default="scope">
          <el-tag type="warning">志愿者修复</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="文件类型" align="center" prop="sourceType" width="100" />
      <el-table-column label="状态" align="center" width="140">
        <template #default="scope">
          <el-tag>{{ formatStatus(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="进度" align="center" width="160">
        <template #default="scope">
          <el-progress :percentage="scope.row.progress || 0" :stroke-width="12" />
        </template>
      </el-table-column>
      <el-table-column label="修复结果" align="center" min-width="220">
        <template #default="scope">
          <div v-if="parseDownloadUrls(scope.row.resultUrls).length > 0" class="file-links">
            <el-link
              v-for="(url, idx) in parseDownloadUrls(scope.row.resultUrls)"
              :key="`result-${scope.row.taskId}-${idx}`"
              type="primary"
              :href="url"
              target="_blank"
            >结果{{ idx + 1 }}</el-link>
          </div>
          <span class="text-grey" v-else>暂无</span>
        </template>
      </el-table-column>
      <el-table-column label="认领志愿者" align="center" prop="studentName" width="120" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="170">
        <template #default="scope">{{ parseTime(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="110">
        <template #default="scope">
          <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['repair:task:remove', 'repair:task:list']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
  </div>
</template>

<script setup name="RepairTask">
import { listRepairTask, delRepairTask } from '@/api/repair/task'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const taskList = ref([])

const statusOptions = [
  { label: '\u7b49\u5f85\u5fd7\u613f\u8005\u8ba4\u9886', value: 'WAIT_STUDENT' },
  { label: '人工处理中', value: 'MANUAL_PROCESSING' },
  { label: '已完成', value: 'COMPLETED' }
]

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    taskNo: undefined,
    userName: undefined,
    repairMode: undefined,
    status: undefined
  }
})

const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listRepairTask(queryParams.value).then((res) => {
    taskList.value = res.rows
    total.value = res.total
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function formatStatus(status) {
  const found = statusOptions.find(i => i.value === status)
  return found ? found.label : status
}

function buildDownloadUrl(url) {
  const value = (url || '').trim()
  if (!value) {
    return '#'
  }
  if (value.startsWith('/profile/')) {
    return import.meta.env.VITE_APP_BASE_API + value
  }
  if (value.startsWith('http://tmp/') || value.startsWith('wxfile://')) {
    return '#'
  }
  if (value.startsWith('http://') || value.startsWith('https://')) {
    return value
  }
  return import.meta.env.VITE_APP_BASE_API + value
}

function parseDownloadUrls(urls) {
  return (urls || '')
    .split(',')
    .map(item => buildDownloadUrl(item))
    .filter(item => item && item !== '#')
}

function handleDelete(row) {
  const taskId = row.taskId
  proxy.$modal.confirm('是否确认删除任务编号为"' + taskId + '"的数据项？').then(function () {
    return delRepairTask(taskId)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

getList()
</script>

<style scoped>
.page-wrap {
  padding: 0;
}

.page-hero {
  background: linear-gradient(135deg, #1a6b5e 0%, #3eb49f 100%);
  padding: 24px 24px 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 0 0 16px 16px;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
}

.page-hero::before {
  content: '';
  position: absolute;
  width: 200px; height: 200px;
  border-radius: 50%;
  background: rgba(255,255,255,0.06);
  top: -60px; right: 80px;
}

.hero-text { position: relative; z-index: 1; }

.hero-title {
  font-size: 22px;
  font-weight: bold;
  color: #fff;
  margin-bottom: 6px;
}

.hero-sub {
  font-size: 13px;
  color: rgba(255,255,255,0.8);
}

.hero-badge {
  background: rgba(255,255,255,0.2);
  color: #fff;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  position: relative;
  z-index: 1;
}

.page-body {
  padding: 0 16px 20px;
}

.search-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 20px 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 12px rgba(23, 67, 62, 0.06);
}

.table-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(23, 67, 62, 0.06);
}

.text-grey {
  color: #909399;
}

.file-links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
