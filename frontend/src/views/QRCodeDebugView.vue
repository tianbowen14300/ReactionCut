<template>
  <div class="qrcode-debug-view">
    <el-card class="debug-card">
      <h2>二维码生成调试</h2>
      
      <div class="debug-section">
        <el-button type="primary" @click="debugQRCodeGeneration" :loading="loading">
          {{ loading ? '调试中...' : '调试二维码生成' }}
        </el-button>
        <el-button @click="testMockData">使用模拟数据测试</el-button>
        <el-button @click="testQRCodeLibrary">测试QRCode库</el-button>
      </div>
      
      <div class="debug-info" v-if="debugInfo.length > 0">
        <h3>调试信息:</h3>
        <div v-for="(info, index) in debugInfo" :key="index" class="debug-item">
          <el-tag :type="info.type">{{ info.time }}</el-tag>
          <span class="debug-message">{{ info.message }}</span>
        </div>
      </div>
      
      <div v-if="qrCodeUrl" class="qrcode-display">
        <h3>生成的二维码:</h3>
        <div ref="debugQRCodeCanvas" class="qrcode-canvas-wrapper"></div>
        <p>二维码URL: {{ qrCodeUrl }}</p>
      </div>
      
      <div v-if="errorMessage" class="error-message">
        <el-alert type="error" :title="errorMessage" show-icon></el-alert>
      </div>
    </el-card>
  </div>
</template>

<script>
import { generateQRCode } from '@/api/bilibiliAuth'
import { debugGenerateQRCode, testBilibiliQRCodeGeneration } from '@/utils/qrcodeDebug'

export default {
  name: 'QRCodeDebugView',
  data() {
    return {
      loading: false,
      debugInfo: [],
      qrCodeUrl: '',
      errorMessage: ''
    }
  },
  methods: {
    addDebugInfo(message, type = 'info') {
      const time = new Date().toLocaleTimeString()
      this.debugInfo.push({ time, message, type })
      console.log(`[${time}] ${message}`)
    },
    
    async debugQRCodeGeneration() {
      this.loading = true
      this.debugInfo = []
      this.qrCodeUrl = ''
      this.errorMessage = ''
      
      try {
        this.addDebugInfo('开始调试二维码生成...', 'primary')
        
        // 调用API生成二维码
        this.addDebugInfo('调用API生成二维码...')
        const response = await generateQRCode()
        this.addDebugInfo('API响应: ' + JSON.stringify(response, null, 2), 'success')
        
        if (response.code === 0) {
          this.qrCodeUrl = response.data.url
          this.addDebugInfo('二维码URL: ' + this.qrCodeUrl, 'success')
          
          // 等待DOM更新
          await this.$nextTick()
          this.addDebugInfo('DOM已更新，准备生成二维码')
          
          // 生成二维码
          await this.generateDebugQRCode()
        } else {
          this.errorMessage = 'API返回错误: ' + response.message
          this.addDebugInfo('API返回错误: ' + response.message, 'danger')
        }
      } catch (error) {
        console.error('调试二维码生成失败:', error)
        this.errorMessage = '调试失败: ' + error.message
        this.addDebugInfo('调试失败: ' + error.message, 'danger')
      } finally {
        this.loading = false
      }
    },
    
    async testMockData() {
      this.debugInfo = []
      this.qrCodeUrl = ''
      this.errorMessage = ''
      
      try {
        this.addDebugInfo('开始测试模拟数据...', 'primary')
        
        const result = await testBilibiliQRCodeGeneration()
        this.addDebugInfo('测试结果: ' + JSON.stringify(result, null, 2), result.success ? 'success' : 'danger')
        
        if (result.success) {
          this.qrCodeUrl = result.data.url
          this.addDebugInfo('二维码URL: ' + this.qrCodeUrl, 'success')
          
          // 等待DOM更新
          await this.$nextTick()
          this.addDebugInfo('DOM已更新，准备生成二维码')
          
          // 生成二维码
          await this.generateDebugQRCode()
        } else {
          this.errorMessage = result.message
          this.addDebugInfo(result.message, 'danger')
        }
      } catch (error) {
        console.error('测试模拟数据失败:', error)
        this.errorMessage = '测试失败: ' + error.message
        this.addDebugInfo('测试失败: ' + error.message, 'danger')
      }
    },
    
    async testQRCodeLibrary() {
      this.debugInfo = []
      this.qrCodeUrl = 'https://www.bilibili.com'
      this.errorMessage = ''
      
      try {
        this.addDebugInfo('开始测试QRCode库...', 'primary')
        this.addDebugInfo('测试URL: ' + this.qrCodeUrl, 'info')
        
        // 等待DOM更新
        await this.$nextTick()
        this.addDebugInfo('DOM已更新，准备生成二维码')
        
        // 生成二维码
        await this.generateDebugQRCode()
      } catch (error) {
        console.error('测试QRCode库失败:', error)
        this.errorMessage = '测试失败: ' + error.message
        this.addDebugInfo('测试失败: ' + error.message, 'danger')
      }
    },
    
    async generateDebugQRCode() {
      try {
        this.addDebugInfo('开始生成二维码...', 'primary')
        
        if (!this.qrCodeUrl) {
          const error = new Error('二维码URL为空')
          this.addDebugInfo('缺少生成二维码的必要参数: 二维码URL为空', 'warning')
          throw error
        }
        
        if (!this.$refs.debugQRCodeCanvas) {
          const error = new Error('二维码Canvas容器未找到')
          this.addDebugInfo('缺少生成二维码的必要参数: 二维码Canvas容器未找到', 'warning')
          throw error
        }
        
        // 使用调试工具生成二维码
        await debugGenerateQRCode(this.qrCodeUrl, this.$refs.debugQRCodeCanvas)
        this.addDebugInfo('二维码生成成功', 'success')
      } catch (error) {
        console.error('生成调试二维码失败:', error)
        this.errorMessage = '生成二维码失败: ' + error.message
        this.addDebugInfo('生成二维码失败: ' + error.message, 'danger')
        throw error
      }
    }
  }
}
</script>

<style scoped>
.qrcode-debug-view {
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: calc(100vh - 84px);
}

.debug-card {
  width: 100%;
  max-width: 800px;
}

.debug-section {
  margin-bottom: 20px;
}

.debug-section .el-button {
  margin-right: 10px;
}

.debug-info {
  margin: 20px 0;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  max-height: 300px;
  overflow-y: auto;
}

.debug-info h3 {
  margin-top: 0;
}

.debug-item {
  margin-bottom: 8px;
  display: flex;
  align-items: center;
}

.debug-item .el-tag {
  margin-right: 10px;
  flex-shrink: 0;
}

.debug-message {
  word-break: break-all;
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