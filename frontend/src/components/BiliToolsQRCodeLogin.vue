<template>
  <div class="bili-tools-qrcode-login">
    <h2>Bilibili 二维码登录</h2>
    
    <div v-if="!isLogin" class="login-section">
      <div class="qrcode-section">
        <h3>扫码登录</h3>
        <div class="qrcode-container">
          <div
            v-if="scanStatus === -2"
            class="loading-overlay"
          >
            <div class="loading-spinner"></div>
            <span>加载中...</span>
          </div>
          <div
            v-if="scanStatus === 86038 || scanStatus === 86090"
            class="refresh-overlay"
            @click="initScan"
          >
            <i
              class="refresh-icon"
              :class="{
                'el-icon-refresh': scanStatus === 86038,
                'el-icon-check': scanStatus === 86090,
              }"
            ></i>
            <span class="refresh-text">{{
              scanStatus === 86038 ? '二维码已过期\n请点击刷新' : '扫码成功\n请在手机上确认'
            }}</span>
          </div>
          <canvas ref="qrcodeCanvas" class="qrcode-canvas"></canvas>
        </div>
        <span class="scan-tip">请使用Bilibili手机客户端扫描二维码</span>
      </div>
    </div>
    
    <div v-else class="user-info-section">
      <div class="user-header">
        <div class="user-top-photo">
          <div class="placeholder-top-photo"></div>
        </div>
        <div class="user-details">
          <div class="user-avatar">
            <img :src="user.avatar" :alt="user.name" />
            <div
              v-if="user.vipLabel"
              class="vip-badge"
            >
              <span class="vip-text">VIP</span>
            </div>
          </div>
          <div class="user-info">
            <h1 class="user-name">{{ user.name }}</h1>
            <span class="user-desc">{{ user.desc }}</span>
          </div>
          <div class="user-stats">
            <div
              v-for="(value, key) in user.stat"
              :key="key"
              class="stat-item"
            >
              <span class="stat-label">{{ getStatLabel(key) }}</span>
              <span class="stat-value">{{ value }}</span>
            </div>
          </div>
          <button class="logout-btn" @click="exitLogin">
            <i class="el-icon-switch-button"></i>
            <span>退出登录</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { genQrcode, scanLogin, exitLogin } from '@/services/bilibiliLogin'

export default {
  name: 'BiliToolsQRCodeLogin',
  data() {
    return {
      isLogin: false,
      scanStatus: -1, // -2: 加载中, -1: 初始化, 86101: 未扫码, 86090: 已扫描未确认, 86038: 二维码已失效, 0: 登录成功
      user: {
        avatar: '',
        name: '',
        desc: '',
        topPhoto: '',
        vipLabel: '',
        stat: {
          following: 0,
          follower: 0,
          dynamic: 0,
          coins: 0
        }
      }
    }
  },
  mounted() {
    this.initScan()
  },
  methods: {
    getStatLabel(key) {
      const labels = {
        following: '关注数',
        follower: '粉丝数',
        dynamic: '动态数',
        coins: '硬币数'
      }
      return labels[key] || key
    },
    
    async initScan() {
      if (!this.$refs.qrcodeCanvas) return
      
      try {
        this.scanStatus = -2 // 设置为加载状态
        this.$refs.qrcodeCanvas.height = 300 // 重置canvas
        
        // 生成二维码
        const key = await genQrcode(this.$refs.qrcodeCanvas)
        this.scanStatus = -1 // 设置为初始状态
        
        // 开始轮询扫码状态
        const status = await scanLogin(key, (code) => {
          this.scanStatus = code
        })
        
        // 注意：scanLogin在登录成功时会返回0并结束执行
        if (status === 0) {
          // 登录成功
          this.scanStatus = 0
          this.isLogin = true
          this.fetchUserInfo()
        } else if (status === 86038) {
          // 二维码已失效
          this.scanStatus = 86038
        }
      } catch (error) {
        console.error('初始化扫码失败:', error)
        this.$message.error('初始化扫码失败: ' + error.message)
      }
    },
    
    async fetchUserInfo() {
      try {
        // 这里应该调用后端API获取用户信息
        // 暂时使用模拟数据
        this.user = {
          avatar: 'https://i0.hdslb.com/bfs/face/member/noface.jpg',
          name: 'Bilibili用户',
          desc: 'Bilibili用户签名',
          topPhoto: '',
          vipLabel: '',
          stat: {
            following: 100,
            follower: 500,
            dynamic: 20,
            coins: 10
          }
        }
      } catch (error) {
        console.error('获取用户信息失败:', error)
        this.$message.error('获取用户信息失败: ' + error.message)
      }
    },
    
    async exitLogin() {
      try {
        const status = await exitLogin()
        if (status === 0) {
          this.isLogin = false
          this.user = {
            avatar: '',
            name: '',
            desc: '',
            topPhoto: '',
            vipLabel: '',
            stat: {
              following: 0,
              follower: 0,
              dynamic: 0,
              coins: 0
            }
          }
          this.$message.success('退出登录成功')
        }
      } catch (error) {
        console.error('退出登录失败:', error)
        this.$message.error('退出登录失败: ' + error.message)
      }
    }
  }
}
</script>

<style scoped>
.bili-tools-qrcode-login {
  max-width: 90vw;
  margin: 0 auto;
  padding: 20px;
}

.login-section {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  gap: 20px;
}

.qrcode-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.qrcode-container {
  position: relative;
  display: flex;
  width: 300px;
  padding: 10px;
  border-radius: 10px;
  background-color: white;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.loading-overlay {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 300px;
  height: 300px;
  background-color: rgba(255, 255, 255, 0.9);
  z-index: 2;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #00a1d6;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.refresh-overlay {
  position: absolute;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  width: 300px;
  height: 300px;
  background-color: rgba(255, 255, 255, 0.9);
  cursor: pointer;
  z-index: 2;
  border-radius: 10px;
}

.refresh-icon {
  font-size: 24px;
  color: #00a1d6;
}

.refresh-text {
  margin: 0;
  text-align: center;
  color: #666;
  font-size: 14px;
  white-space: pre-line;
}

.qrcode-canvas {
  width: 300px;
  height: 300px;
}

.scan-tip {
  color: #999;
  font-size: 14px;
}

.user-info-section {
  width: 100%;
}

.user-header {
  position: relative;
}

.placeholder-top-photo {
  width: 100%;
  height: 200px;
  background: linear-gradient(45deg, #00a1d6, #00b5e5);
  border-radius: 10px 10px 0 0;
}

.user-details {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 20px;
  background-color: white;
  border-radius: 0 0 10px 10px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.user-avatar {
  position: relative;
}

.user-avatar img {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  border: 3px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.vip-badge {
  position: absolute;
  width: 30px;
  height: 30px;
  right: 0;
  bottom: 0;
  background: linear-gradient(45deg, #ff9800, #ff5722);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.vip-text {
  color: white;
  font-size: 10px;
  font-weight: bold;
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}

.user-name {
  margin: 0;
  font-size: 24px;
  font-weight: bold;
  color: #333;
}

.user-desc {
  color: #999;
  font-size: 14px;
}

.user-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-label {
  color: #999;
  font-size: 12px;
}

.stat-value {
  font-size: 16px;
  font-weight: bold;
  color: #333;
}

.logout-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 10px 20px;
  background-color: #00a1d6;
  color: white;
  border: none;
  border-radius: 5px;
  cursor: pointer;
  font-size: 14px;
}

.logout-btn:hover {
  background-color: #008cca;
}

.logout-btn i {
  font-size: 16px;
}
</style>