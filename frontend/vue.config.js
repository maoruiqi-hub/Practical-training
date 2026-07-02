module.exports = {
  devServer: {
    port: 3000,
    proxy: {
      '/practical-training': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
}
