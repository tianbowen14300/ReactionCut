<template>
  <div class="bili-tools-qrcode-login-view">
    <el-card class="login-card">
      <BiliToolsQRCodeLogin />
    </el-card>
  </div>
</template>

<script>
import BiliToolsQRCodeLogin from '@/components/BiliToolsQRCodeLogin.vue'

export default {
  name: 'BiliToolsQRCodeLoginView',
  components: {
    BiliToolsQRCodeLogin
  }
}
</script>

<style scoped>
.bili-tools-qrcode-login-view {
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: calc(100vh - 84px);
}

.login-card {
  width: 100%;
  max-width: 800px;
}
</style>