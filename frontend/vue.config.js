module.exports = {
  devServer: {
    port: 3000,
    proxy: {
      '/practical-training': {
        target: 'http://localhost:3001',
        changeOrigin: true
      }
    }
  }
}
