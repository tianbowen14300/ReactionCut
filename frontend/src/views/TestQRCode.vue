<template>
  <div class="test-qrcode">
    <h2>二维码生成测试</h2>
    
    <div class="test-section">
      <el-button type="primary" @click="testQRCodeGeneration">测试二维码生成</el-button>
      <el-button @click="testWithSampleURL">使用示例URL测试</el-button>
    </div>
    
    <div v-if="qrCodeUrl" class="qrcode-display">
      <h3>生成的二维码URL:</h3>
      <p>{{ qrCodeUrl }}</p>
      
      <h3>生成的二维码:</h3>
      <div ref="testQRCodeCanvas" class="qrcode-canvas-wrapper"></div>
    </div>
    
    <div v-if="testMessage" class="test-message">
      <el-alert :type="testMessageType" :title="testMessage" show-icon></el-alert>
    </div>
  </div>
</template>

<script>
import QRCode from 'qrcode'

export default {
  name: 'TestQRCode',
  data() {
    return {
      qrCodeUrl: '',
      testMessage: '',
      testMessageType: 'info'
    }
  },
  methods: {
    async testQRCodeGeneration() {
      try {
        // 这里应该调用实际的API来生成二维码
        // 由于我们无法直接测试后端，我们使用一个示例URL
        this.qrCodeUrl = 'https://passport.bilibili.com/h5-app/passport/login/qr?oauthKey=sample_key_12345'
        this.testMessage = '模拟二维码生成成功'
        this.testMessageType = 'success'
        
        // 生成二维码
        this.$nextTick(() => {
          this.generateTestQRCode()
        })
      } catch (error) {
        console.error('测试二维码生成失败:', error)
        this.testMessage = '测试二维码生成失败: ' + error.message
        this.testMessageType = 'error'
      }
    },
    
    testWithSampleURL() {
      this.qrCodeUrl = 'https://www.bilibili.com'
      this.testMessage = '使用示例URL测试'
      this.testMessageType = 'info'
      
      // 生成二维码
      this.$nextTick(() => {
        this.generateTestQRCode()
      })
    },
    
    generateTestQRCode() {
      if (this.qrCodeUrl && this.$refs.testQRCodeCanvas) {
        try {
          console.log('开始生成测试二维码:', { url: this.qrCodeUrl, canvas: this.$refs.testQRCodeCanvas })
          
          // 清空容器
          this.$refs.testQRCodeCanvas.innerHTML = ''
          
          // 使用QRCode库生成二维码
          QRCode.toCanvas(this.$refs.testQRCodeCanvas, this.qrCodeUrl, {
            width: 300,
            height: 300,
            margin: 2,
            color: {
              dark: '#000000',
              light: '#ffffff'
            }
          }, (error) => {
            if (error) {
              console.error('生成测试二维码失败:', error)
              this.testMessage = '生成测试二维码失败: ' + error.message
              this.testMessageType = 'error'
            } else {
              console.log('测试二维码生成成功')
              this.testMessage = '测试二维码生成成功'
              this.testMessageType = 'success'
              
              // 验证二维码是否正确生成
              const canvas = this.$refs.testQRCodeCanvas.querySelector('canvas')
              if (canvas) {
                console.log('测试二维码Canvas元素已创建:', { width: canvas.width, height: canvas.height })
              } else {
                console.warn('未找到测试二维码Canvas元素')
              }
            }
          })
        } catch (error) {
          console.error('创建测试二维码图片失败:', error)
          this.testMessage = '创建测试二维码图片失败: ' + error.message
          this.testMessageType = 'error'
        }
      } else {
        console.warn('缺少生成测试二维码的必要参数:', { qrCodeUrl: this.qrCodeUrl, canvas: this.$refs.testQRCodeCanvas })
        this.testMessage = '缺少生成测试二维码的必要参数'
        this.testMessageType = 'warning'
      }
    }
  }
}
</script>

<style scoped>
.test-qrcode {
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

.qrcode-display {
  margin-top: 20px;
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.qrcode-display h3 {
  margin-top: 0;
}

.qrcode-display p {
  word-break: break-all;
  background-color: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
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

.test-message {
  margin-top: 20px;
}
</style>