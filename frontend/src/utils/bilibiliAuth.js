// Bilibili认证工具类

class BilibiliAuth {
  // 登录信息存储的键名
  // static LOGIN_INFO_KEY = 'bilibili_login_info'
  
  // 保存登录信息
  static saveLoginInfo(loginInfo) {
    try {
      const info = {
        ...loginInfo,
        timestamp: Date.now()
      }
      localStorage.setItem('bilibili_login_info', JSON.stringify(info))
      console.log('登录信息已保存到本地存储:', info)
      return true
    } catch (error) {
      console.error('保存登录信息失败:', error)
      return false
    }
  }
  
  // 获取登录信息
  static getLoginInfo() {
    try {
      const infoStr = localStorage.getItem('bilibili_login_info')
      if (!infoStr) {
        return null
      }
      
      const info = JSON.parse(infoStr)
      
      // 检查是否过期（24小时）
      const now = Date.now()
      const expireTime = 24 * 60 * 60 * 1000 // 24小时
      if (now - info.timestamp > expireTime) {
        this.clearLoginInfo()
        return null
      }
      
      return info
    } catch (error) {
      console.error('获取登录信息失败:', error)
      return null
    }
  }
  
  // 检查是否已登录
  static isLoggedIn() {
    return this.getLoginInfo() !== null
  }
  
  // 清除登录信息
  static clearLoginInfo() {
    try {
      localStorage.removeItem('bilibili_login_info')
      return true
    } catch (error) {
      console.error('清除登录信息失败:', error)
      return false
    }
  }
  
  // 获取认证cookie
  static getAuthCookies() {
    const loginInfo = this.getLoginInfo()
    if (!loginInfo || !loginInfo.data) {
      return null
    }
    
    // 从登录数据中提取cookie信息
    // 根据实际的响应结构调整提取逻辑
    return this.extractCookiesFromData(loginInfo.data)
  }
  
  // 从数据中提取Cookie
  static extractCookiesFromData(data) {
    try {
      // 根据实际的登录响应结构调整提取逻辑
      if (typeof data === 'string') {
        // 如果data是字符串，可能直接包含了cookie信息
        return data
      } else if (typeof data === 'object') {
        // 如果data是对象，查找cookie字段
        if (data.cookie) {
          return data.cookie
        } else if (data.cookies) {
          return data.cookies
        } else if (data.data && typeof data.data === 'string') {
          // 如果data字段是字符串，可能包含了cookie信息
          return data.data
        }
      }
      return null
    } catch (error) {
      console.error('提取Cookie信息失败:', error)
      return null
    }
  }
}

export default BilibiliAuth