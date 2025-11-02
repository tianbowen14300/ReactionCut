import request from './request'
import bilibiliApi from '../utils/bilibiliInterceptor'

// 下载Bilibili视频
export function downloadVideo(data) {
  return request({
    url: '/video/download',
    method: 'post',
    data
  })
}

// 查询下载任务状态
export function getDownloadStatus(taskId) {
  return request({
    url: `/video/download/${taskId}`,
    method: 'get'
  })
}

// 获取待下载任务列表
export function getPendingDownloads() {
  return request({
    url: '/video/download/pending',
    method: 'get'
  })
}

// 获取下载中任务列表
export function getDownloadingDownloads() {
  return request({
    url: '/video/download/downloading',
    method: 'get'
  })
}

// 获取已完成下载任务列表
export function getCompletedDownloads() {
  return request({
    url: '/video/download/completed',
    method: 'get'
  })
}

// 删除下载记录
export function deleteDownloadRecord(taskId) {
  return request({
    url: `/video/download/${taskId}`,
    method: 'delete'
  })
}

// 创建视频处理任务
export function createProcessTask(data) {
  return request({
    url: '/video/process',
    method: 'post',
    data
  })
}

// 查询处理任务状态
export function getProcessStatus(taskId) {
  return request({
    url: `/video/process/${taskId}`,
    method: 'get'
  })
}

// 获取视频详细信息
export function getVideoDetail(bvid) {
  return bilibiliApi({
    url: '/video/detail',
    method: 'get',
    params: {
      bvid
    },
    needAuth: true
  })
}

// 获取视频详细信息(通过aid)
export function getVideoDetailByAid(aid) {
  return bilibiliApi({
    url: '/video/detail',
    method: 'get',
    params: {
      aid
    },
    needAuth: true
  })
}

// 获取视频流URL（通过bvid）
export function getVideoPlayUrl(bvid, cid, params = {}) {
  return request({
    url: '/video/stream/playurl',
    method: 'get',
    params: {
      bvid,
      cid,
      ...params
    }
  })
}

// 获取视频流URL（通过aid）
export function getVideoPlayUrlByAid(aid, cid, params = {}) {
  return request({
    url: '/video/stream/playurl/aid',
    method: 'get',
    params: {
      aid,
      cid,
      ...params
    }
  })
}