module.exports = {
  productionSourceMap: false,
  configureWebpack: {
    optimization: {
      splitChunks: {
        chunks: 'all',
        maxInitialRequests: 8,
        maxAsyncRequests: 10,
        cacheGroups: {
          elementPlus: {
            test: /[\\/]node_modules[\\/]element-plus[\\/]/,
            name: 'chunk-element-plus',
            priority: 20
          },
          echarts: {
            test: /[\\/]node_modules[\\/]echarts[\\/]/,
            name: 'chunk-echarts',
            priority: 15
          },
          vendors: {
            test: /[\\/]node_modules[\\/]/,
            name: 'chunk-vendors',
            priority: 10
          }
        }
      }
    }
  },
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
