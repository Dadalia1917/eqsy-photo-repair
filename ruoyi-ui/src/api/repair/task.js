import request from '@/utils/request'

export function listRepairTask(query) {
  return request({
    url: '/repair/task/list',
    method: 'get',
    params: query
  })
}

export function getRepairTask(taskId) {
  return request({
    url: '/repair/task/' + taskId,
    method: 'get'
  })
}

export function claimRepairTask(taskId) {
  return request({
    url: '/repair/task/claim/' + taskId,
    method: 'post'
  })
}

export function uploadManualResult(data) {
  return request({
    url: '/repair/task/manual/result',
    method: 'put',
    data: data
  })
}

export function uploadResultVideo(data) {
  return request({
    url: '/repair/task/manual/video',
    method: 'put',
    data: data
  })
}

export function finishManualTask(taskId) {
  return request({
    url: '/repair/task/manual/finish/' + taskId,
    method: 'put'
  })
}

export function triggerAiTask(taskId) {
  return request({
    url: '/repair/task/ai/trigger/' + taskId,
    method: 'post'
  })
}

export function getRepairTrend(days) {
  return request({
    url: '/repair/task/trend',
    method: 'get',
    params: { days }
  })
}
