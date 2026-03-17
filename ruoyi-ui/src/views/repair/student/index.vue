<template>
  <div class="app-container">
    <el-alert title="学生修复工作台：先认领任务，再上传修复结果，最后完成回传。" type="info" :closable="false" class="mb12" />

    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="任务编号" prop="taskNo">
        <el-input v-model="queryParams.taskNo" placeholder="请输入任务编号" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 180px">
          <el-option label="等待认领" value="WAIT_STUDENT" />
          <el-option label="人工处理中" value="MANUAL_PROCESSING" />
          <el-option label="已完成" value="COMPLETED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />

    <el-table v-loading="loading" :data="taskList">
      <el-table-column label="编号" prop="taskId" width="80" align="center" />
      <el-table-column label="任务编号" prop="taskNo" min-width="200" />
      <el-table-column label="提交用户" prop="userName" width="120" align="center" />
      <el-table-column label="源文件" min-width="220">
        <template #default="scope">
          <el-link :href="buildDownload(scope.row.sourceUrls)" type="primary" target="_blank">查看原文件</el-link>
        </template>
      </el-table-column>
      <el-table-column label="老人需求" prop="remark" min-width="260" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ scope.row.remark || '未填写' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" prop="status" width="140" align="center" />
      <el-table-column label="当前进度" width="170" align="center">
        <template #default="scope">
          <el-progress :percentage="scope.row.progress || 0" :stroke-width="12" />
        </template>
      </el-table-column>
      <el-table-column label="结果文件" min-width="220">
        <template #default="scope">
          <el-link v-if="scope.row.resultUrls" :href="buildDownload(scope.row.resultUrls)" type="success" target="_blank">查看结果</el-link>
          <span v-else class="text-grey">未上传</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="300">
        <template #default="scope">
          <el-button link type="primary" @click="handleClaim(scope.row)" v-hasPermi="['repair:task:claim']">认领</el-button>
          <el-button link type="warning" @click="handleOpenUpload(scope.row)" v-hasPermi="['repair:task:edit']">上传结果</el-button>
          <el-button link type="success" @click="handleFinish(scope.row)" v-hasPermi="['repair:task:edit']">完成回传</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="上传修复结果" v-model="openUpload" width="680px" append-to-body>
      <el-form label-width="100px">
        <el-form-item label="任务编号">{{ uploadForm.taskNo }}</el-form-item>
        <el-form-item label="修复图片">
          <image-upload v-model="uploadForm.resultUrls" :limit="5" :file-size="20" :drag="true" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="openUpload = false">取消</el-button>
        <el-button type="primary" @click="submitUpload">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="RepairStudent">
import { listRepairTask, claimRepairTask, uploadManualResult, finishManualTask } from '@/api/repair/task'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const taskList = ref([])
const openUpload = ref(false)

const uploadForm = reactive({
  taskId: undefined,
  taskNo: '',
  resultUrls: ''
})

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    taskNo: undefined,
    status: undefined,
    repairMode: 'MANUAL'
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

function handleClaim(row) {
  claimRepairTask(row.taskId).then(() => {
    proxy.$modal.msgSuccess('认领成功')
    getList()
  })
}

function handleOpenUpload(row) {
  uploadForm.taskId = row.taskId
  uploadForm.taskNo = row.taskNo
  uploadForm.resultUrls = row.resultUrls || ''
  openUpload.value = true
}

function submitUpload() {
  if (!uploadForm.resultUrls) {
    proxy.$modal.msgError('请先上传修复后的图片')
    return
  }
  uploadManualResult({
    taskId: uploadForm.taskId,
    resultUrls: uploadForm.resultUrls
  }).then(() => {
    proxy.$modal.msgSuccess('上传成功')
    openUpload.value = false
    getList()
  })
}

function handleFinish(row) {
  finishManualTask(row.taskId).then(() => {
    proxy.$modal.msgSuccess('已完成并回传社区端')
    getList()
  })
}

function buildDownload(urls) {
  const first = (urls || '').split(',')[0]
  if (!first) {
    return '#'
  }
  if (first.startsWith('http://') || first.startsWith('https://')) {
    return first
  }
  return import.meta.env.VITE_APP_BASE_API + first
}

getList()
</script>

<style scoped>
.mb12 {
  margin-bottom: 12px;
}
.text-grey {
  color: #909399;
}
</style>
