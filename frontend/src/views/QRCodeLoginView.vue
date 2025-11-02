<template>
  <div class="qrcode-login-view">
    <el-card class="login-card">
      <QRCodeLogin @login-success="onLoginSuccess" />
    </el-card>
  </div>
</template>

<script>
import QRCodeLogin from '@/components/QRCodeLogin.vue'

export default {
  name: 'QRCodeLoginView',
  components: {
    QRCodeLogin
  },
  methods: {
    onLoginSuccess() {
      this.$message.success('登录成功！')
      // 可以在这里添加登录成功后的逻辑，比如跳转到其他页面
      // this.$router.push('/dashboard')
    }
  }
}
</script>

<style scoped>
.qrcode-login-view {
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: calc(100vh - 84px);
}

.login-card {
  width: 100%;
  max-width: 500px;
}
</style>