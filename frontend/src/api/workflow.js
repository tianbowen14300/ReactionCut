import request from './request'

/**
 * 获取任务的工作流状态
 * @param {string} taskId 任务ID
 * @returns {Promise} 工作流状态信息
 */
export function getWorkflowStatus(taskId) {
  return request({
    url: `/api/submission-tasks/${taskId}/workflow/status`,
    method: 'get'
  })
}

/**
 * 暂停工作流
 * @param {string} taskId 任务ID
 * @returns {Promise} 操作结果
 */
export function pauseWorkflow(taskId) {
  return request({
    url: `/api/submission-tasks/${taskId}/workflow/pause`,
    method: 'post'
  })
}

/**
 * 恢复工作流
 * @param {string} taskId 任务ID
 * @returns {Promise} 操作结果
 */
export function resumeWorkflow(taskId) {
  return request({
    url: `/api/submission-tasks/${taskId}/workflow/resume`,
    method: 'post'
  })
}

/**
 * 取消工作流
 * @param {string} taskId 任务ID
 * @returns {Promise} 操作结果
 */
export function cancelWorkflow(taskId) {
  return request({
    url: `/api/submission-tasks/${taskId}/workflow/cancel`,
    method: 'post'
  })
}

/**
 * 获取所有活跃的工作流
 * @returns {Promise} 活跃工作流列表
 */
export function getActiveWorkflows() {
  return request({
    url: '/api/submission-tasks/workflow/active',
    method: 'get'
  })
}