import axios from 'axios'

// 创建axios实例
const service = axios.create({
  baseURL: '/api', // 使用代理路径
  timeout: 15000 // 请求超时时间
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    return config
  },
  error => {
    console.log(error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
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
    console.log('请求错误:', error)
    return Promise.reject(error)
  }
)

export default service