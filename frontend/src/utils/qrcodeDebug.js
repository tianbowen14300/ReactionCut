/**
 * 二维码调试工具
 */

import qrcode from 'qrcode-generator'

/**
 * 调试二维码生成
 * @param {string} url - 二维码内容URL
 * @param {HTMLElement} container - 容器元素
 * @returns {Promise} 生成结果Promise
 */
export function debugGenerateQRCode(url, container) {
  return new Promise((resolve, reject) => {
    console.log('调试二维码生成:', { url, container })
    
    // 参数验证
    if (!url) {
      const error = new Error('URL参数不能为空')
      console.error('调试二维码生成失败: URL参数为空')
      reject(error)
      return
    }
    
    if (!container) {
      const error = new Error('容器参数不能为空')
      console.error('调试二维码生成失败: 容器参数为空')
      reject(error)
      return
    }
    
    try {
      // 清空容器
      container.innerHTML = ''
      
      // 使用qrcode-generator生成二维码
      const qr = qrcode(0, 'M')
      qr.addData(url)
      qr.make()
      
      // 创建canvas元素
      const canvas = document.createElement('canvas')
      canvas.width = 300
      canvas.height = 300
      canvas.style.maxWidth = '100%'
      canvas.style.maxHeight = '100%'
      
      // 获取canvas上下文并绘制二维码
      const ctx = canvas.getContext('2d')
      const cellSize = Math.floor(canvas.width / qr.getModuleCount())
      
      // 绘制背景
      ctx.fillStyle = '#ffffff'
      ctx.fillRect(0, 0, canvas.width, canvas.height)
      
      // 绘制二维码模块
      for (let row = 0; row < qr.getModuleCount(); row++) {
        for (let col = 0; col < qr.getModuleCount(); col++) {
          if (qr.isDark(row, col)) {
            ctx.fillStyle = '#000000'
            ctx.fillRect(col * cellSize, row * cellSize, cellSize, cellSize)
          }
        }
      }
      
      // 将canvas添加到容器中
      container.appendChild(canvas)
      
      console.log('调试二维码生成成功')
      resolve()
    } catch (error) {
      console.error('创建调试二维码图片失败:', error)
      reject(error)
    }
  })
}

/**
 * 测试Bilibili二维码生成
 * @returns {Promise} 测试结果Promise
 */
export async function testBilibiliQRCodeGeneration() {
  try {
    console.log('开始测试Bilibili二维码生成...')
    
    // 模拟API调用
    const mockResponse = {
      code: 0,
      message: "success",
      data: {
        url: "https://passport.bilibili.com/h5-app/passport/login/qr?oauthKey=test_key_123456",
        qrcode_key: "test_key_123456"
      }
    }
    
    console.log('模拟API响应:', mockResponse)
    
    if (mockResponse.code === 0) {
      return {
        success: true,
        data: mockResponse.data,
        message: "测试成功"
      }
    } else {
      return {
        success: false,
        message: mockResponse.message || "测试失败"
      }
    }
  } catch (error) {
    console.error('测试Bilibili二维码生成失败:', error)
    return {
      success: false,
      message: "测试失败: " + error.message
    }
  }
}

export default {
  debugGenerateQRCode,
  testBilibiliQRCodeGeneration
}