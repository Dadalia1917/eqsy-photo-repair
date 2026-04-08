<template>
  <div class="app-container">
    <el-alert title="志愿服务工作台：先认领任务，再上传服务结果，最后完成回传。" type="info" :closable="false" class="mb12" />

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
          <el-link v-if="buildDownload(scope.row.sourceUrls) !== '#'" :href="buildDownload(scope.row.sourceUrls)" type="primary" target="_blank">查看原文件</el-link>
          <span v-else class="text-grey">文件不可用</span>
        </template>
      </el-table-column>
      <el-table-column label="用户需求" prop="remark" min-width="260" show-overflow-tooltip>
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
      <el-form label-width="110px">
        <el-form-item label="任务编号">{{ uploadForm.taskNo }}</el-form-item>
        <el-form-item label="修复图片">
          <image-upload v-model="uploadForm.resultUrls" :limit="5" :file-size="20" :drag="true" />
        </el-form-item>
        <el-form-item label="动态视频">
          <el-upload
            :action="uploadVideoUrl"
            :headers="uploadHeaders"
            :before-upload="beforeVideoUpload"
            :on-success="handleVideoSuccess"
            :on-error="handleVideoError"
            accept="video/mp4,video/quicktime"
            :show-file-list="false"
          >
            <el-button type="primary" plain>点击上传视频（≤10MB，mp4/mov）</el-button>
          </el-upload>
          <div v-if="uploadForm.resultVideoUrl" class="video-preview">
            <video :src="buildVideoUrl(uploadForm.resultVideoUrl)" controls style="max-width:100%;max-height:200px;margin-top:8px;"></video>
            <el-link type="danger" style="margin-left:8px;" @click="uploadForm.resultVideoUrl = ''">移除</el-link>
          </div>
          <div class="el-upload__tip" style="color:#909399;">可选，用于生成老照片动起来的短视频效果</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="openUpload = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="submitting" @click="submitUpload">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="RepairStudent">
import { listRepairTask, claimRepairTask, uploadManualResult, uploadResultVideo, finishManualTask } from '@/api/repair/task'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const taskList = ref([])
const openUpload = ref(false)
const submitting = ref(false)

const uploadForm = reactive({
  taskId: undefined,
  taskNo: '',
  resultUrls: '',
  resultVideoUrl: ''
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

const uploadVideoUrl = import.meta.env.VITE_APP_BASE_API + '/common/upload'
const uploadHeaders = { Authorization: 'Bearer ' + localStorage.getItem('Admin-Token') }

function beforeVideoUpload(file) {
  const isVideo = file.type === 'video/mp4' || file.type === 'video/quicktime'
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isVideo) {
    proxy.$modal.msgError('只支持 mp4/mov 格式视频')
    return false
  }
  if (!isLt10M) {
    proxy.$modal.msgError('视频大小不能超过 10MB')
    return false
  }
  return true
}

function handleVideoSuccess(res) {
  if (res.code === 200) {
    uploadForm.resultVideoUrl = res.fileName
    proxy.$modal.msgSuccess('视频上传成功')
  } else {
    proxy.$modal.msgError(res.msg || '视频上传失败')
  }
}

function handleVideoError() {
  proxy.$modal.msgError('视频上传失败，请重试')
}

function buildVideoUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return import.meta.env.VITE_APP_BASE_API + url
}

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
  submitting.value = false
  uploadForm.taskId = row.taskId
  uploadForm.taskNo = row.taskNo
  uploadForm.resultUrls = row.resultUrls || ''
  uploadForm.resultVideoUrl = row.resultVideoUrl || ''
  openUpload.value = true
}

async function submitUpload() {
  if (submitting.value) {
    return
  }
  if (!uploadForm.resultUrls) {
    proxy.$modal.msgError('请先上传修复后的图片')
    return
  }
  submitting.value = true
  try {
    await uploadManualResult({
      taskId: uploadForm.taskId,
      resultUrls: uploadForm.resultUrls
    })
    if (uploadForm.resultVideoUrl) {
      await uploadResultVideo({
        taskId: uploadForm.taskId,
        resultVideoUrl: uploadForm.resultVideoUrl
      })
    }
    proxy.$modal.msgSuccess('上传成功')
    openUpload.value = false
    getList()
  } catch (error) {
    if (error && error.message === '数据正在处理，请勿重复提交') {
      proxy.$modal.msgWarning('数据正在处理，请勿重复提交')
    }
  } finally {
    submitting.value = false
  }
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
  // 有效的服务器相对路径（如 /profile/upload/...）
  if (first.startsWith('/profile/')) {
    return import.meta.env.VITE_APP_BASE_API + first
  }
  // 微信小程序临时路径（http://tmp/...）或其他无效路径，标记为不可用
  if (first.startsWith('http://tmp/') || first.startsWith('wxfile://')) {
    return '#'
  }
  // 其他完整URL直接返回
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
