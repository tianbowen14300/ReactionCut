<template>
  <div>
    <h2>Ref测试</h2>
    <input ref="testInput" type="file" style="display: none;" @change="handleFileSelect" />
    <button @click="triggerFileSelect">触发文件选择</button>
    <p>文件名: {{ fileName }}</p>
  </div>
</template>

<script>
export default {
  name: 'TestRef',
  data() {
    return {
      fileName: ''
    }
  },
  methods: {
    triggerFileSelect() {
      console.log('Refs:', this.$refs);
      if (this.$refs.testInput) {
        console.log('找到ref元素');
        this.$refs.testInput.click();
      } else {
        console.error('未找到ref元素');
      }
    },
    handleFileSelect(event) {
      const files = event.target.files;
      if (files && files.length > 0) {
        this.fileName = files[0].name;
      }
    }
  }
}
</script>