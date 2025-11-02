import request from './request'

// 批量订阅主播
export function subscribeAnchors(data) {
  return request({
    url: '/anchor/subscribe',
    method: 'post',
    data
  })
}

// 获取所有订阅的主播
export function getAllAnchors() {
  return request({
    url: '/anchor/list',
    method: 'get'
  })
}

// 手动检查主播直播状态
export function checkLiveStatus() {
  return request({
    url: '/anchor/check',
    method: 'post'
  })
}