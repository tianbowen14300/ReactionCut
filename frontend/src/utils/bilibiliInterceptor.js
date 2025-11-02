// Bilibili API拦截器
import axios from 'axios'
import BilibiliAuth from './bilibiliAuth'

// 创建axios实例
const bilibiliApi = axios.create({
  baseURL: '/api', // 使用代理避免跨域问题
  timeout: 10000
})

// 请求拦截器
bilibiliApi.interceptors.request.use(
  config => {
    // 检查是否需要认证
    if (config.needAuth) {
      const loginInfo = BilibiliAuth.getLoginInfo()
      if (loginInfo && loginInfo.data) {
        // 从登录信息中提取cookie并添加到请求头
        const cookies = extractCookiesFromLoginInfo(loginInfo.data)
        if (cookies) {
          config.headers['Cookie'] = cookies
        }
      }
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 从登录信息中提取Cookie
function extractCookiesFromLoginInfo(loginData) {
  try {
    // 根据实际的登录响应结构调整提取逻辑
    if (typeof loginData === 'string') {
      // 如果loginData是字符串，可能直接包含了cookie信息
      return loginData
    } else if (typeof loginData === 'object') {
      // 如果loginData是对象，查找cookie字段
      if (loginData.cookie) {
        return loginData.cookie
      } else if (loginData.cookies) {
        return loginData.cookies
      } else if (loginData.data && typeof loginData.data === 'string') {
        // 如果data字段是字符串，可能包含了cookie信息
        return loginData.data
      }
    }
    return null
  } catch (error) {
    console.error('提取Cookie信息失败:', error)
    return null
  }
}

// 响应拦截器
bilibiliApi.interceptors.response.use(
  response => {
    const res = response.data
    // 修改这里以支持后端返回的code格式 (0表示成功)
    if (res.code !== 0) {
      console.log('请求失败:', res.message)
      return Promise.reject(new Error(res.message || 'Error'))
    } else {
      return res
    }
  },
  error => {
    if (error.response) {
      switch (error.response.status) {
        case 401:
          // 未授权，清除登录信息
          BilibiliAuth.clearLoginInfo()
          // 可以跳转到登录页面或提示重新登录
          break
        case 403:
          // 禁止访问
          console.error('访问被禁止')
          break
        default:
          console.error('请求错误:', error.response.status)
      }
    }
    return Promise.reject(error)
  }
)

export default bilibiliApi