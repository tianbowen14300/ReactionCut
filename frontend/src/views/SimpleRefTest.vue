<template>
  <div>
    <h2>简单Ref测试</h2>
    <el-button @click="testRef">测试Ref</el-button>
    <input ref="testInput" type="file" style="display: none;" webkitdirectory />
    <p>测试结果: {{ testResult }}</p>
  </div>
</template>

<script>
export default {
  name: 'SimpleRefTest',
  data() {
    return {
      testResult: ''
    }
  },
  methods: {
    testRef() {
      console.log('测试refs:', this.$refs);
      if (this.$refs.testInput) {
        this.testResult = '成功找到ref';
        console.log('成功找到ref');
      } else {
        this.testResult = '未找到ref';
        console.log('未找到ref');
      }
    }
  },
  mounted() {
    console.log('SimpleRefTest挂载完成，检查refs:', this.$refs);
  }
}
</script>