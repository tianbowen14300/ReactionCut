import request from './request'

/**
 * 获取用户的所有合集列表
 * @param {number} mid 用户mid
 * @returns {Promise} 合集列表
 */
export function getUserCollections(mid) {
  return request({
    url: '/bilibili/collections',
    method: 'get',
    params: { mid }
  }).then(response => {
    if (response.code === 0) {
      return response.data
    } else {
      return Promise.reject(new Error(response.message || '获取合集列表失败'))
    }
  })
}

/**
 * 获取所有视频分区
 * @returns {Promise} 分区列表
 */
export function getAllPartitions() {
  return request({
    url: '/bilibili/partitions',
    method: 'get'
  }).then(response => {
    if (response.code === 0) {
      return response.data
    } else {
      return Promise.reject(new Error(response.message || '获取分区列表失败'))
    }
  })
}

export default {
  getUserCollections,
  getAllPartitions
}