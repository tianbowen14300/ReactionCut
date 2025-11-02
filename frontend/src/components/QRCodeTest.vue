<template>
  <div class="qrcode-test">
    <h2>二维码生成测试</h2>
    
    <div class="test-section">
      <el-button type="primary" @click="testQRCodeGeneration" :loading="loading">
        {{ loading ? '测试中...' : '测试二维码生成' }}
      </el-button>
      <el-button @click="testWithSampleData">使用示例数据测试</el-button>
    </div>
    
    <div v-if="testResult" class="test-result">
      <h3>测试结果:</h3>
      <pre>{{ JSON.stringify(testResult, null, 2) }}</pre>
    </div>
    
    <div v-if="qrCodeUrl" class="qrcode-display">
      <h3>生成的二维码:</h3>
      <div ref="testQRCodeCanvas" class="qrcode-canvas-wrapper"></div>
      <p>二维码URL: {{ qrCodeUrl }}</p>
    </div>
    
    <div v-if="errorMessage" class="error-message">
      <el-alert type="error" :title="errorMessage" show-icon></el-alert>
    </div>
  </div>
</template>

<script>
import qrcode from 'qrcode-generator'
import { generateQRCode } from '@/api/bilibiliAuth'

export default {
  name: 'QRCodeTest',
  data() {
    return {
      loading: false,
      testResult: null,
      qrCodeUrl: '',
      errorMessage: ''
    }
  },
  methods: {
    async testQRCodeGeneration() {
      this.loading = true
      this.testResult = null
      this.qrCodeUrl = ''
      this.errorMessage = ''
      
      try {
        console.log('开始测试二维码生成...')
        const response = await generateQRCode()
        console.log('二维码生成响应:', response)
        
        this.testResult = response
        
        if (response.code === 0) {
          this.qrCodeUrl = response.data.url
          console.log('二维码URL:', this.qrCodeUrl)
          
          // 等待DOM更新
          await this.$nextTick()
          console.log('DOM已更新，准备生成二维码')
          
          // 生成二维码
          await this.generateTestQRCode()
        } else {
          this.errorMessage = 'API返回错误: ' + response.message
        }
      } catch (error) {
        console.error('测试二维码生成失败:', error)
        this.errorMessage = '测试失败: ' + error.message
      } finally {
        this.loading = false
      }
    },
    
    async testWithSampleData() {
      this.testResult = {
        code: 0,
        message: "success",
        data: {
          url: "https://passport.bilibili.com/h5-app/passport/login/qr?oauthKey=sample_key_123456",
          qrcode_key: "sample_key_123456"
        }
      }
      
      this.qrCodeUrl = this.testResult.data.url
      this.errorMessage = ''
      
      // 等待DOM更新
      await this.$nextTick()
      console.log('DOM已更新，准备生成二维码')
      
      // 生成二维码
      await this.generateTestQRCode()
    },
    
    generateTestQRCode() {
      return new Promise((resolve, reject) => {
        if (!this.qrCodeUrl) {
          const error = new Error('二维码URL为空')
          console.warn('缺少生成二维码的必要参数: 二维码URL为空')
          this.errorMessage = '二维码URL为空'
          reject(error)
          return
        }
        
        if (!this.$refs.testQRCodeCanvas) {
          const error = new Error('二维码Canvas容器未找到')
          console.warn('缺少生成二维码的必要参数: 二维码Canvas容器未找到')
          this.errorMessage = '二维码显示容器未找到'
          reject(error)
          return
        }
        
        try {
          console.log('开始使用qrcode-generator生成测试二维码:', { url: this.qrCodeUrl, canvas: this.$refs.testQRCodeCanvas })
          
          // 清空容器
          this.$refs.testQRCodeCanvas.innerHTML = ''
          
          // 使用qrcode-generator生成二维码
          const qr = qrcode(0, 'M')
          qr.addData(this.qrCodeUrl)
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
          this.$refs.testQRCodeCanvas.appendChild(canvas)
          
          console.log('测试二维码生成成功')
          resolve()
        } catch (error) {
          console.error('创建测试二维码图片失败:', error)
          this.errorMessage = '生成二维码失败: ' + error.message
          reject(error)
        }
      })
    }
  }
}
</script>

<style scoped>
.qrcode-test {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.test-section {
  margin-bottom: 20px;
}

.test-section .el-button {
  margin-right: 10px;
}

.test-result {
  margin: 20px 0;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #ebeef5;
}

.test-result h3 {
  margin-top: 0;
}

.test-result pre {
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
  font-size: 12px;
}

.qrcode-display {
  margin: 20px 0;
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.qrcode-display h3 {
  margin-top: 0;
}

.qrcode-canvas-wrapper {
  display: flex;
  justify-content: center;
  margin: 20px 0;
  min-height: 300px;
  align-items: center;
  border: 1px dashed #dcdfe6;
  border-radius: 4px;
}

.error-message {
  margin: 20px 0;
}
</style>