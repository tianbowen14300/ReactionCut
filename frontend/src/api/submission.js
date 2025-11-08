import request from './request'

/**
 * 创建投稿任务
 * @param {Object} taskData 任务数据
 * @returns {Promise} 任务ID
 */
export function createTask(taskData) {
  return request({
    url: '/submission-tasks',
    method: 'post',
    data: taskData
  }).then(response => {
    if (response.code === 0) {
      return response.data
    } else {
      return Promise.reject(new Error(response.message || '创建任务失败'))
    }
  })
}

/**
 * 获取所有任务列表（按创建时间倒序）
 * @returns {Promise} 任务列表
 */
export function getAllTasks() {
  return request({
    url: `/submission-tasks`,
    method: 'get'
  }).then(response => {
    console.log('response', response)
    if (response.code === 0) {
      return response
    } else {
      return Promise.reject(new Error(response.message || '获取任务列表失败'))
    }
  })
}

/**
 * 根据状态获取任务列表（按创建时间倒序）
 * @param {string} status 任务状态
 * @returns {Promise} 任务列表
 */
export function getTasksByStatus(status) {
  return request({
    url: `/submission-tasks/status/${status}`,
    method: 'get'
  }).then(response => {
    console.log('response', response)
    if (response.code === 0) {
      return response
    } else {
      return Promise.reject(new Error(response.message || '获取任务列表失败'))
    }
  })
}

/**
 * 根据任务ID获取任务详情
 * @param {string} taskId 任务ID
 * @returns {Promise} 任务详情
 */
export function getTaskById(taskId) {
  return request({
    url: `/submission-tasks/${taskId}`,
    method: 'get'
  }).then(response => {
    if (response.code === 0) {
      return response
    } else {
      return Promise.reject(new Error(response.message || '获取任务详情失败'))
    }
  })
}

/**
 * 视频剪辑
 * @param {string} taskId 任务ID
 * @returns {Promise} 剪辑后的文件路径列表
 */
export function clipVideos(taskId) {
  return request({
    url: `/video-process/${taskId}/clip`,
    method: 'post'
  }).then(response => {
    if (response.code === 0) {
      return response.data
    } else {
      return Promise.reject(new Error(response.message || '视频剪辑失败'))
    }
  })
}

/**
 * 视频合并
 * @param {string} taskId 任务ID
 * @returns {Promise} 合并后的文件路径
 */
export function mergeVideos(taskId) {
  return request({
    url: `/video-process/${taskId}/merge`,
    method: 'post'
  }).then(response => {
    if (response.code === 0) {
      return response.data
    } else {
      return Promise.reject(new Error(response.message || '视频合并失败'))
    }
  })
}

/**
 * 获取合并后的视频信息
 * @param {string} taskId 任务ID
 * @returns {Promise} 合并后的视频信息列表
 */
export function getMergedVideos(taskId) {
  return request({
    url: `/video-process/${taskId}/merged-videos`,
    method: 'get'
  }).then(response => {
    if (response.code === 0) {
      return response.data
    } else {
      return Promise.reject(new Error(response.message || '获取合并视频信息失败'))
    }
  })
}

/**
 * 视频分段
 * @param {string} taskId 任务ID
 * @returns {Promise} 分段后的文件路径列表
 */
export function segmentVideo(taskId) {
  return request({
    url: `/video-process/${taskId}/segment`,
    method: 'post'
  }).then(response => {
    if (response.code === 0) {
      return response.data
    } else {
      return Promise.reject(new Error(response.message || '视频分段失败'))
    }
  })
}

/**
 * 执行投稿任务
 * @param {string} taskId 任务ID
 * @returns {Promise} 执行结果
 */
export function executeTask(taskId) {
  return request({
    url: `/submission-tasks/${taskId}/execute`,
    method: 'post'
  }).then(response => {
    if (response.code === 0) {
      return response.data
    } else {
      return Promise.reject(new Error(response.message || '执行投稿任务失败'))
    }
  })
}

export default {
  createTask,
  getAllTasks,
  getTasksByStatus,
  getTaskById,
  clipVideos,
  mergeVideos,
  segmentVideo,
  executeTask
}