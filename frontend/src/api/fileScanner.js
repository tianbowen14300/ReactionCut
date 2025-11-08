import request from './request'

/**
 * 扫描指定路径下的文件和文件夹
 * @param {string} path 要扫描的路径
 * @returns {Promise} 文件和文件夹列表
 */
export function scanPath(path) {
  return request({
    url: '/file-scanner/scan',
    method: 'get',
    params: { path }
  }).then(response => {
    if (response.code === 0) {
      return response.data
    } else {
      return Promise.reject(new Error(response.message || '扫描路径失败'))
    }
  })
}

export default {
  scanPath
}