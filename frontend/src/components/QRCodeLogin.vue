<template>
  <div class="qrcode-login">
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
  name: 'QRCodeLogin',
  data() {
    return {
      loading: false,
      qrCodeUrl: '',
      qrcodeKey: '',
      qrCodeStatus: '', // 'pending', 'scanned', 'confirmed', 'expired', 'success'
      loginSuccess: false,
      pollInterval: null
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
    },
    statusIcon() {
      switch (this.qrCodeStatus) {
        case 'pending':
          return 'el-icon-time'
        case 'scanned':
          return 'el-icon-check'
        case 'confirmed':
        case 'success':
          return 'el-icon-success'
        case 'expired':
          return 'el-icon-warning'
        default:
          return 'el-icon-info'
      }
    },
    statusColor() {
      switch (this.qrCodeStatus) {
        case 'pending':
          return '#909399'
        case 'scanned':
          return '#409EFF'
        case 'confirmed':
        case 'success':
          return '#67C23A'
        case 'expired':
          return '#F56C6C'
        default:
          return '#909399'
      }
    }
  },
  methods: {
    async generateQRCode() {
      this.loading = true
      try {
        const response = await generateQRCode()
        console.log('二维码生成响应:', response) // 添加日志以便调试
        // 修改这里以处理新的响应格式
        if (response.code === 0) {
          // 注意：这里我们使用data.url而不是data.data.url
          this.qrCodeUrl = response.data.url
          this.qrcodeKey = response.data.qrcode_key
          this.qrCodeStatus = 'pending'
          console.log('二维码URL:', this.qrCodeUrl) // 添加日志以便调试
          console.log('二维码Key:', this.qrcodeKey) // 添加日志以便调试
          
          // 确保DOM更新后再生成二维码
          await this.$nextTick()
          console.log('DOM已更新，准备生成二维码')
          
          // 使用QRCode库生成二维码
          await this.generateQRCodeImage()
          
          this.startPolling()
        } else {
          this.$message.error(response.message || '生成二维码失败4')
        }
      } catch (error) {
        console.error('生成二维码失败:', error) // 添加错误日志
        this.$message.error('生成二维码失败5: ' + error.message)
      } finally {
        this.loading = false
      }
    },
    
    // 使用qrcode-generator库生成二维码
    generateQRCodeImage() {
      return new Promise((resolve, reject) => {
        // 检查必要参数
        console.log('检查生成二维码的必要参数:', { 
          qrCodeUrl: this.qrCodeUrl, 
          hasCanvasRef: !!this.$refs.qrCodeCanvas,
          canvasRef: this.$refs.qrCodeCanvas
        })
        
        if (!this.qrCodeUrl) {
          const error = new Error('二维码URL为空')
          console.warn('缺少生成二维码的必要参数: 二维码URL为空')
          this.$message.warning('二维码URL为空，请重新生成')
          reject(error)
          return
        }
        
        if (!this.$refs.qrCodeCanvas) {
          const error = new Error('二维码容器未找到')
          console.warn('缺少生成二维码的必要参数: 二维码容器未找到')
          this.$message.warning('二维码显示容器未找到，请重新生成')
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
          resolve()
        } catch (error) {
          console.error('生成二维码失败:', error)
          this.$message.error('生成二维码失败: ' + error.message)
          reject(error)
        }
      })
    },
    
    startPolling() {
      // 停止之前的轮询
      if (this.pollInterval) {
        clearInterval(this.pollInterval)
      }
      
      // 开始新的轮询
      this.pollInterval = setInterval(() => {
        this.pollQRCodeStatus()
      }, 3000) // 每3秒轮询一次，避免过于频繁的请求
    },
    
    async pollQRCodeStatus() {
      if (!this.qrcodeKey) {
        console.warn('缺少二维码Key，无法轮询状态')
        return
      }
      
      console.log('开始轮询二维码状态，qrcodeKey:', this.qrcodeKey)
      
      try {
        const response = await pollQRCodeStatus(this.qrcodeKey)
        console.log('轮询响应:', response)
        
        if (response.code === 0) {
          const data = response.data
          const code = data.code
          
          console.log('二维码状态码:', code, '消息:', data.message)
          
          // 添加调试信息，记录时间戳
          console.log('轮询时间:', new Date().toISOString())
          
          switch (code) {
            case 0: // 登录成功
              this.qrCodeStatus = 'success'
              this.loginSuccess = true
              this.stopPolling()
              this.$emit('login-success')
              this.$message.success('登录成功')
              // 添加成功登录的调试信息
              console.log('登录成功，完整响应数据:', data)
              
              // 保存登录信息到本地存储
              this.saveLoginInfo(data)
              break
            case 86101: // 未扫码
              this.qrCodeStatus = 'pending'
              // 添加调试信息
              console.log('用户尚未扫码，继续轮询')
              break
            case 86090: // 已扫描未确认
              this.qrCodeStatus = 'scanned'
              this.$message.info('已扫描，请在手机上确认登录')
              // 添加调试信息
              console.log('用户已扫描但未确认')
              break
            case 86038: // 二维码失效
              this.qrCodeStatus = 'expired'
              this.stopPolling()
              // 自动刷新二维码
              this.$message.warning('二维码已失效，正在自动刷新...')
              setTimeout(() => {
                this.refreshQRCode()
              }, 2000)
              break
            default:
              // 对于其他未知状态，继续轮询
              this.qrCodeStatus = 'pending'
              console.log('未知状态:', code, data.message)
          }
        } else {
          // API调用失败，继续轮询
          this.qrCodeStatus = 'pending'
          console.log('API调用失败:', response.message)
        }
      } catch (error) {
        console.error('轮询二维码状态失败:', error)
        // 发生错误时继续轮询
        this.qrCodeStatus = 'pending'
      }
    },
    
    // 保存登录信息到本地存储
    saveLoginInfo(data) {
      try {
        const loginInfo = {
          data: data,
          timestamp: Date.now()
        }
        
        // 保存到localStorage
        localStorage.setItem('bilibili_login_info', JSON.stringify(loginInfo))
        console.log('登录信息已保存到本地存储')
      } catch (error) {
        console.error('保存登录信息失败:', error)
        this.$message.error('保存登录信息失败: ' + error.message)
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
        // 修改这里以处理新的响应格式
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
.qrcode-login {
  max-width: 90vw;  /* 设置最大宽度为视窗宽度的90% */
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