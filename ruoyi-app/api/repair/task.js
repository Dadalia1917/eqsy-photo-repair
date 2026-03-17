import upload from '@/utils/upload'
import request from '@/utils/request'

export function uploadSourceFile(filePath) {
  return upload({
    url: '/common/upload',
    name: 'file',
    filePath: filePath
  })
}

export function submitRepairTask(data) {
  return request({
    url: '/app/repair/task/submit',
    method: 'post',
    data: data
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
