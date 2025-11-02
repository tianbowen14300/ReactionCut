import request from './request'
import bilibiliApi from '../utils/bilibiliInterceptor'

// 生成二维码
export function generateQRCode() {
  return request({
    url: '/bilibili/auth/qrcode/generate',
    method: 'get'
  })
}

// 轮询二维码状态
export function pollQRCodeStatus(qrcodeKey) {
  console.log('调用轮询接口，qrcodeKey:', qrcodeKey)
  return request({
    url: '/bilibili/auth/qrcode/poll',
    method: 'get',
    params: {
      qrcodeKey
    }
  })
}

// 执行完整的二维码登录流程
export function performQRCodeLogin() {
  return request({
    url: '/bilibili/qrcode/login',
    method: 'post'
  })
}

// 检查登录状态
export function checkLoginStatus() {
  return request({
    url: '/bilibili/auth/status',
    method: 'get'
  })
}

// 注销登录
export function logout() {
  return request({
    url: '/bilibili/auth/logout',
    method: 'post'
  })
}

// 获取用户信息
export function getUserInfo(userId) {
  return bilibiliApi({
    url: '/live/anchor/info',
    method: 'get',
    params: {
      userId
    },
    needAuth: true
  })
}

// 获取视频信息
export function getVideoInfo(bvid) {
  return bilibiliApi({
    url: '/video/info',
    method: 'get',
    params: {
      bvid
    },
    needAuth: true
  })
}

// 搜索视频
export function searchVideos(keyword, page = 1, pageSize = 20) {
  return bilibiliApi({
    url: '/video/search',
    method: 'get',
    params: {
      keyword,
      page,
      pageSize
    },
    needAuth: true
  })
}