/**
 * Bilibili登录服务
 * 模仿BiliTools的实现方式
 */

import qrcode from 'qrcode-generator'
import { generateQRCode, pollQRCodeStatus } from '@/api/bilibiliAuth'

/**
 * 生成二维码
 * @param {HTMLCanvasElement} canvas - Canvas元素
 * @returns {Promise<string>} 二维码key
 */
export async function genQrcode(canvas) {
  try {
    // 调用后端API生成二维码
    const response = await generateQRCode()
    
    if (response.code !== 0) {
      throw new Error(response.message || '生成二维码失败')
    }
    
    const { url, qrcode_key } = response.data
    
    // 使用qrcode-generator生成二维码
    const qr = qrcode(0, 'M')
    qr.addData(url)
    qr.make()
    
    // 设置canvas尺寸
    const size = 300
    canvas.width = size
    canvas.height = size
    
    // 获取canvas上下文并绘制二维码
    const ctx = canvas.getContext('2d')
    const cellSize = Math.floor(size / qr.getModuleCount())
    
    // 绘制背景
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, size, size)
    
    // 绘制二维码模块
    for (let row = 0; row < qr.getModuleCount(); row++) {
      for (let col = 0; col < qr.getModuleCount(); col++) {
        if (qr.isDark(row, col)) {
          ctx.fillStyle = '#000000'
          ctx.fillRect(col * cellSize, row * cellSize, cellSize, cellSize)
        }
      }
    }
    
    return qrcode_key
  } catch (error) {
    console.error('生成二维码失败:', error)
    throw error
  }
}

/**
 * 扫码登录
 * @param {string} qrcode_key - 二维码key
 * @param {function} onEvent - 状态更新回调函数
 * @returns {Promise<number>} 登录状态码
 */
export async function scanLogin(qrcode_key, onEvent) {
  try {
    let attempts = 0
    const maxAttempts = 30 // 最大轮询次数
    
    while (attempts < maxAttempts) {
      attempts++
      
      console.log(`轮询尝试 ${attempts}/${maxAttempts}`)
      
      // 轮询二维码状态
      const response = await pollQRCodeStatus(qrcode_key)
      console.log('轮询响应:', response)
      
      if (response.code === 0) {
        const { code, message } = response.data
        console.log('二维码状态码:', code, '消息:', message)
        
        // 调用回调函数通知状态更新
        if (onEvent) {
          onEvent(code)
        }
        
        // 根据状态码处理不同情况
        switch (code) {
          case 0: // 登录成功
            console.log('登录成功')
            return 0
            
          case 86101: // 未扫码
            // 继续轮询
            await new Promise(resolve => setTimeout(resolve, 2000)) // 每2秒轮询一次
            break
            
          case 86090: // 已扫描未确认
            console.log('已扫描，请在手机上确认登录')
            // 继续轮询
            await new Promise(resolve => setTimeout(resolve, 2000)) // 每2秒轮询一次
            break
            
          case 86038: // 二维码已失效
            console.log('二维码已失效')
            return 86038
            
          default:
            // 对于未知状态，继续轮询
            console.log('未知状态码:', code, message)
            await new Promise(resolve => setTimeout(resolve, 2000)) // 每2秒轮询一次
            break
        }
      } else {
        // API调用失败，继续轮询
        console.log('API调用失败:', response.message)
        await new Promise(resolve => setTimeout(resolve, 2000)) // 每2秒轮询一次
      }
    }
    
    // 超时
    console.log('轮询超时')
    return -1
  } catch (error) {
    console.error('扫码登录失败:', error)
    // 发生错误时继续轮询
    return -1
  }
}

/**
 * 退出登录
 * @returns {Promise<number>} 状态码
 */
export async function exitLogin() {
  try {
    // 这里应该调用后端的注销接口
    // 暂时返回成功状态码
    return 0
  } catch (error) {
    console.error('退出登录失败:', error)
    throw error
  }
}

/**
 * 检查并刷新Cookie
 * @returns {Promise<number>} 状态码
 */
export async function checkRefresh() {
  try {
    // 这里应该调用后端的检查刷新接口
    // 暂时返回成功状态码
    return 0
  } catch (error) {
    console.error('检查刷新失败:', error)
    throw error
  }
}

export default {
  genQrcode,
  scanLogin,
  exitLogin,
  checkRefresh
}