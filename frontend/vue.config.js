module.exports = {
  transpileDependencies: [],
  devServer: {
    port: 8081,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  lintOnSave: false
}