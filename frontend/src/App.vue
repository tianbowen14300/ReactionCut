<template>
  <div id="app">
    <el-container style="height: 100vh;">
      <!-- 顶部导航栏 -->
      <el-header style="background-color: #409EFF; color: white; display: flex; justify-content: space-between; align-items: center; padding: 0 20px;">
        <h2 style="margin: 0;">Bilibili视频处理系统</h2>
        <!-- 用户头像区域 -->
        <div class="user-info" @click="handleUserClick">
          <el-avatar 
            :src="userAvatar" 
            size="large" 
            shape="circle"
            style="cursor: pointer; border: 2px solid white;"
          ></el-avatar>
        </div>
      </el-header>
      
      <el-container>
        <!-- 侧边栏菜单 -->
        <el-aside width="200px" style="background-color: #545c64">
          <el-menu
            :default-active="$route.path"
            class="el-menu-vertical-demo"
            background-color="#545c64"
            text-color="#fff"
            active-text-color="#ffd04b"
            router
            style="height: 100%"
          >
            <el-menu-item index="/anchor">
              <i class="el-icon-user"></i>
              <span slot="title">主播订阅</span>
            </el-menu-item>
            <el-menu-item index="/download">
              <i class="el-icon-download"></i>
              <span slot="title">视频下载</span>
            </el-menu-item>
            <el-menu-item index="/process">
              <i class="el-icon-video-camera"></i>
              <span slot="title">视频处理</span>
            </el-menu-item>
            <el-menu-item index="/submission">
              <i class="el-icon-upload"></i>
              <span slot="title">视频投稿</span>
            </el-menu-item>
            <el-menu-item index="/video-stream-test">
              <i class="el-icon-video-play"></i>
              <span slot="title">视频流测试</span>
            </el-menu-item>
            <el-menu-item index="/qrcode-login">
              <i class="el-icon-lock"></i>
              <span slot="title">二维码登录</span>
            </el-menu-item>
          </el-menu>
        </el-aside>

        <!-- 主内容区域 -->
        <el-main>
          <router-view></router-view>
        </el-main>
      </el-container>
    </el-container>
    
    <!-- 登录二维码弹窗 -->
    <el-dialog
      title="Bilibili扫码登录"
      :visible.sync="qrCodeDialogVisible"
      width="80%"
      :before-close="handleQRCodeDialogClose"
      :close-on-click-modal="false">
      <div style="text-align: center;">
        <div v-if="qrCodeUrl" id="qrcode" ref="qrcode" style="display: flex; justify-content: center; align-items: center; min-height: 80vh;">
          <!-- 使用canvas显示二维码 -->
          <div ref="qrCodeCanvas" class="qrcode-canvas-wrapper"></div>
          <p style="margin-top: 15px; font-size: 14px; color: #666;">
            请使用Bilibili手机客户端扫描二维码登录
          </p>
        </div>
        <div v-else style="padding: 20px 0;">
          <i class="el-icon-loading" style="font-size: 24px;"></i>
          <p style="margin-top: 10px;">正在生成二维码...</p>
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="handleQRCodeDialogClose">取消</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { generateQRCode, pollQRCodeStatus } from './api/bilibiliAuth'
import qrcode from 'qrcode-generator'
import BilibiliAuth from './utils/bilibiliAuth'

export default {
  name: 'App',
  data() {
    return {
      userAvatar: 'https://cube.elemecdn.com/3/7c/3ea3bd2e8b32f34c30c0b6c0b9e9e0jpeg.jpeg', // 默认头像
      qrCodeDialogVisible: false,
      qrCodeUrl: '',
      qrCodeKey: '',
      pollTimer: null,
      isLoggedIn: false
    }
  },
  async mounted() {
    // 检查登录状态
    await this.checkLoginStatus()
  },
  methods: {
    // 检查登录状态
    async checkLoginStatus() {
      try {
        // 优先检查本地存储
        if (BilibiliAuth.isLoggedIn()) {
          this.isLoggedIn = true
          // 设置用户头像（这里使用默认头像，实际应该从用户信息中获取）
          this.userAvatar = 'https://i0.hdslb.com/bfs/face/member/noface.jpg' // B站默认头像
        } else {
          // 未登录使用默认头像
          this.isLoggedIn = false
          this.userAvatar = 'https://cube.elemecdn.com/3/7c/3ea3bd2e8b32f34c30c0b6c0b9e9e0jpeg.jpeg'
        }
      } catch (error) {
        console.error('检查登录状态失败:', error)
        this.isLoggedIn = false
        this.userAvatar = 'https://cube.elemecdn.com/3/7c/3ea3bd2e8b32f34c30c0b6c0b9e9e0jpeg.jpeg'
      }
    },
    
    // 处理用户头像点击
    async handleUserClick() {
      if (this.isLoggedIn) {
        // 已登录，显示注销选项
        try {
          await this.$confirm('确定要注销登录吗?', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          })
          
          // 清除本地登录信息
          BilibiliAuth.clearLoginInfo()
          this.isLoggedIn = false
          this.userAvatar = 'https://cube.elemecdn.com/3/7c/3ea3bd2e8b32f34c30c0b6c0b9e9e0jpeg.jpeg'
          this.$message.success('注销成功')
        } catch (error) {
          if (error !== 'cancel') {
            this.$message.error('注销失败: ' + error)
          }
        }
      } else {
        // 未登录，显示登录选项
        this.showLoginDialog()
      }
    },
    
    // 显示登录对话框
    async showLoginDialog() {
      this.qrCodeDialogVisible = true
      this.qrCodeUrl = ''
      this.qrCodeKey = ''
      
      try {
        // 生成二维码
        const response = await generateQRCode()
        console.log('二维码生成响应:', response) // 添加日志以便调试
        if (response.code === 0) {
          // 注意：这里我们使用data.url而不是data.data.url
          this.qrCodeUrl = response.data.url
          this.qrCodeKey = response.data.qrcode_key
          console.log('二维码URL:', this.qrCodeUrl) // 添加日志以便调试
          console.log('二维码Key:', this.qrCodeKey) // 添加日志以便调试
          
          // 确保DOM更新后再生成二维码
          this.$nextTick(() => {
            // 使用QRCode库生成二维码
            this.generateQRCodeImage()
          })
          
          // 开始轮询
          this.startPolling()
        } else {
          this.$message.error('生成二维码失败2: ' + response.message)
        }
      } catch (error) {
        console.error('生成二维码失败:', error) // 添加错误日志
        this.$message.error('生成二维码失败3: ' + error)
      }
    },
    
    // 使用qrcode-generator库生成二维码
    generateQRCodeImage() {
      if (this.qrCodeUrl && this.$refs.qrCodeCanvas) {
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
        } catch (error) {
          console.error('生成二维码失败:', error)
          this.$message.error('生成二维码失败: ' + error.message)
        }
      } else {
        console.warn('缺少生成二维码的必要参数:', { qrCodeUrl: this.qrCodeUrl, canvas: this.$refs.qrCodeCanvas })
        this.$message.warning('缺少生成二维码的必要参数')
      }
    },
    
    // 开始轮询二维码状态
    startPolling() {
      this.stopPolling() // 先停止之前的轮询
      
      this.pollTimer = setInterval(async () => {
        try {
          // 只有在对话框仍然打开时才继续轮询
          if (!this.qrCodeDialogVisible) {
            this.stopPolling()
            return
          }
          
          if (!this.qrCodeKey) {
            console.warn('缺少二维码Key，无法轮询状态')
            return
          }
          
          console.log('App.vue开始轮询二维码状态，qrcodeKey:', this.qrCodeKey)
          
          const response = await pollQRCodeStatus(this.qrCodeKey)
          console.log('App.vue轮询响应:', response)
          
          if (response.code === 0) {
            const data = response.data
            const code = data.code
            
            console.log('App.vue二维码状态码:', code, '消息:', data.message)
            
            switch (code) {
              case 0: // 登录成功
                this.$message.success('登录成功')
                this.stopPolling()
                this.qrCodeDialogVisible = false
                this.isLoggedIn = true
                // 保存登录信息到本地存储
                BilibiliAuth.saveLoginInfo(response)
                // 更新用户头像
                this.userAvatar = 'https://i0.hdslb.com/bfs/face/member/noface.jpg' // B站默认头像
                break
              case 86101: // 未扫码，继续轮询
                console.log('等待用户扫码...')
                break
              case 86090: // 已扫描未确认
                console.log('已扫描，请在手机上确认登录')
                this.$message.info('已扫描，请在手机上确认登录')
                break
              case 86038: // 二维码失效
                this.$message.warning('二维码已失效，正在自动刷新...')
                this.stopPolling()
                // 自动刷新二维码
                setTimeout(() => {
                  this.showLoginDialog()
                }, 2000)
                break
              default:
                console.log('未知状态:', code, response.message)
            }
          } else {
            console.log('轮询API调用失败:', response.message)
          }
        } catch (error) {
          console.error('轮询二维码状态失败:', error)
          // 如果轮询失败，也停止轮询
          this.stopPolling()
        }
      }, 2000) // 每2秒轮询一次，提高响应速度
    },
    
    // 停止轮询
    stopPolling() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },
    
    // 处理二维码对话框关闭
    handleQRCodeDialogClose(done) {
      this.stopPolling()
      done()
    }
  },
  
  beforeDestroy() {
    this.stopPolling()
  }
}
</script>

<style>
#app {
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  height: 100%;
}

.user-info {
  cursor: pointer;
}

.user-info:hover {
  opacity: 0.8;
}

.el-header {
  box-shadow: 0 2px 4px rgba(0,0,0,.1);
}

.qrcode-canvas-wrapper {
  display: flex;
  justify-content: center;
  margin: 20px 0;
  min-height: 300px;
  align-items: center;
}
</style>