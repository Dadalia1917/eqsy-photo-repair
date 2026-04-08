import upload from '@/utils/upload'
import request from '@/utils/request'

const UPLOAD_TIMEOUT = 90000
const SUBMIT_TIMEOUT = 45000

export function uploadSourceFile(filePath) {
  return upload({
    url: '/common/upload',
    name: 'file',
    filePath: filePath,
    timeout: UPLOAD_TIMEOUT
  })
}

export function submitRepairTask(data) {
  return request({
    url: '/app/repair/task/submit',
    method: 'post',
    data: data,
    timeout: SUBMIT_TIMEOUT
  })
}

export function listRepairTask(params) {
  return request({
    url: '/app/repair/task/list',
    method: 'get',
    params: params
  })
}

export function getRepairTask(taskId) {
  return request({
    url: '/app/repair/task/' + taskId,
    method: 'get'
  })
}
