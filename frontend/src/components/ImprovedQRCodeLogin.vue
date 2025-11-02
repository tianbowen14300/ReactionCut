<template>
  <div class="improved-qrcode-login">
    <h2>Bilibili 二维码登录</h2>
    
    <div v-if="!qrCodeUrl" class="generate-section">
      <el-button type="primary" @click="generateQRCode" :loading="loading">
        {{ loading ? '生成中...' : '生成二维码' }}
      </el-button>
    </div>
    
    <div v-else class="qrcode-section">
      <!-- 使用canvas显示二维码 -->
      <div ref="qrCodeCanvas" class="qrcode-canvas-wrapper"></div>
      
      <div class="qrcode-info">
        <p>请使用 Bilibili 手机客户端扫描二维码</p>
        <p class="status-text">{{ statusMessage }}</p>
      </div>
      
      <div class="qrcode-actions">
        <el-button @click="refreshQRCode">刷新二维码</el-button>
        <el-button type="primary" @click="performAutoLogin">自动登录</el-button>
      </div>
    </div>
    
    <div v-if="loginSuccess" class="success-section">
      <el-alert
        title="登录成功！"
        type="success"
        description="您已成功登录 Bilibili 账户"
        show-icon
      />
    </div>
  </div>
</template>

<script>
import { generateQRCode, pollQRCodeStatus, performQRCodeLogin } from '@/api/bilibiliAuth'
import qrcode from 'qrcode-generator'

export default {
  name: 'ImprovedQRCodeLogin',
  data() {
    return {
      loading: false,
      qrCodeUrl: '',
      qrcodeKey: '',
      qrCodeStatus: '', // 'pending', 'scanned', 'confirmed', 'expired', 'success'
      loginSuccess: false,
      pollInterval: null,
      retryCount: 0,
      maxRetries: 3
    }
  },
  computed: {
    statusMessage() {
      switch (this.qrCodeStatus) {
        case 'pending':
          return '等待扫描...'
        case 'scanned':
          return '已扫描，请在手机上确认'
        case 'confirmed':
          return '登录成功'
        case 'expired':
          return '二维码已过期'
        case 'success':
          return '登录成功'
        default:
          return '请扫描二维码'
      }
    }
  },
  methods: {
    async generateQRCode() {
      this.loading = true
      this.retryCount = 0
      
      try {
        await this._generateQRCodeWithRetry()
      } catch (error) {
        console.error('生成二维码失败:', error)
        this.$message.error('生成二维码失败: ' + error.message)
      } finally {
        this.loading = false
      }
    },
    
    async _generateQRCodeWithRetry() {
      try {
        const response = await generateQRCode()
        console.log('二维码生成响应:', response)
        
        if (response.code === 0) {
          // 验证返回的数据
          if (!response.data || !response.data.url || !response.data.qrcode_key) {
            throw new Error('返回的二维码数据格式不正确')
          }
          
          this.qrCodeUrl = response.data.url
          this.qrcodeKey = response.data.qrcode_key
          this.qrCodeStatus = 'pending'
          
          console.log('二维码URL:', this.qrCodeUrl)
          console.log('二维码Key:', this.qrcodeKey)
          
          // 确保DOM更新后再生成二维码
          await this.$nextTick()
          
          // 使用QRCode库生成二维码
          await this.generateQRCodeImage()
          
          // 开始轮询
          this.startPolling()
        } else {
          throw new Error(response.message || '生成二维码失败')
        }
      } catch (error) {
        this.retryCount++
        if (this.retryCount <= this.maxRetries) {
          console.log(`生成二维码失败，正在重试 (${this.retryCount}/${this.maxRetries})...`)
          await new Promise(resolve => setTimeout(resolve, 1000))
          return this._generateQRCodeWithRetry()
        } else {
          throw error
        }
      }
    },
    
    generateQRCodeImage() {
      return new Promise((resolve, reject) => {
        if (!this.qrCodeUrl || !this.$refs.qrCodeCanvas) {
          const error = new Error('缺少生成二维码的必要参数')
          console.warn('缺少生成二维码的必要参数:', { qrCodeUrl: this.qrCodeUrl, canvas: this.$refs.qrCodeCanvas })
          reject(error)
          return
        }
        
        try {
          console.log('开始使用qrcode-generator生成二维码:', { url: this.qrCodeUrl })
          
          // 清空容器
          this.$refs.qrCodeCanvas.innerHTML = ''
          
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
          this.$refs.qrCodeCanvas.appendChild(canvas)
          
          console.log('二维码生成成功')
          // 验证二维码是否正确生成
          resolve()
        } catch (error) {
          console.error('创建二维码图片失败:', error)
          reject(error)
        }
      })
    },
    
    startPolling() {
      // 停止之前的轮询
      this.stopPolling()
      
      // 开始新的轮询
      this.pollInterval = setInterval(() => {
        this.pollQRCodeStatus()
      }, 3000) // 每3秒轮询一次
    },
    
    async pollQRCodeStatus() {
      if (!this.qrcodeKey) return
      
      try {
        const response = await pollQRCodeStatus(this.qrcodeKey)
        
        if (response.code === 0) {
          const data = response.data
          const code = data.code
          
          switch (code) {
            case 0: // 登录成功
              this.qrCodeStatus = 'success'
              this.loginSuccess = true
              this.stopPolling()
              this.$emit('login-success')
              break
            case 1: // 未扫码
              this.qrCodeStatus = 'pending'
              break
            case 2: // 已扫描未确认
              this.qrCodeStatus = 'scanned'
              break
            case 3: // 二维码失效
              this.qrCodeStatus = 'expired'
              this.stopPolling()
              break
            default:
              this.qrCodeStatus = 'pending'
          }
        } else {
          console.warn('轮询二维码状态失败:', response.message)
        }
      } catch (error) {
        console.error('轮询二维码状态失败:', error)
      }
    },
    
    stopPolling() {
      if (this.pollInterval) {
        clearInterval(this.pollInterval)
        this.pollInterval = null
      }
    },
    
    refreshQRCode() {
      this.stopPolling()
      this.qrCodeUrl = ''
      this.qrcodeKey = ''
      this.qrCodeStatus = ''
      this.loginSuccess = false
      this.generateQRCode()
    },
    
    async performAutoLogin() {
      try {
        const response = await performQRCodeLogin()
        
        if (response.code === 0) {
          this.$message.success(response.message || '登录流程已启动')
        } else {
          this.$message.error(response.message || '启动登录流程失败')
        }
      } catch (error) {
        this.$message.error('启动登录流程失败: ' + error.message)
      }
    }
  },
  
  beforeDestroy() {
    // 组件销毁前停止轮询
    this.stopPolling()
  }
}
</script>

<style scoped>
.improved-qrcode-login {
  max-width: 90vw;
  margin: 0 auto;
  padding: 20px;
}

.generate-section {
  text-align: center;
  padding: 40px 0;
}

.qrcode-section {
  text-align: center;
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

.qrcode-info {
  margin: 20px 0;
}

.status-text {
  font-weight: bold;
  color: #409EFF;
}

.qrcode-actions {
  margin: 20px 0;
}

.success-section {
  margin-top: 20px;
}
</style>