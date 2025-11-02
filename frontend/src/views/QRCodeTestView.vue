<template>
  <div class="qrcode-test-view">
    <el-card class="test-card">
      <QRCodeTest />
    </el-card>
  </div>
</template>

<script>
import QRCodeTest from '@/components/QRCodeTest.vue'

export default {
  name: 'QRCodeTestView',
  components: {
    QRCodeTest
  }
}
</script>

<style scoped>
.qrcode-test-view {
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: calc(100vh - 84px);
}

.test-card {
  width: 100%;
  max-width: 800px;
}
</style>