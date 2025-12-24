'use strict'
const path = require('path')

const name = 'G-im-chat'
module.exports = {
  publicPath: process.env.NODE_ENV === 'development' ? '/' : '/',
  outputDir: 'dist',
  assetsDir: 'static',
  lintOnSave: process.env.NODE_ENV === 'development',
  productionSourceMap: false,
  devServer: {
    proxy: {
      ['/apis']: {
        target: process.env.VUE_APP_BASE_API,
        ws: true,
        changeOrigin: true,
        pathRewrite: {
          ['^' + process.env.VUE_APP_NGINX_PATH]: ''
        }
      }
    }
  },
  configureWebpack: (config) => {
    config.resolve.alias = {
      '@': path.resolve(__dirname, './src'),
      _v: path.resolve(__dirname, './src/views'),
      _c: path.resolve(__dirname, './src/components'),
      _a: path.resolve(__dirname, './src/assets'),
      _r: path.resolve(__dirname, './src/api'),
      _public: path.resolve(__dirname, './public'),
      '@babel/runtime/helpers/extends': '@babel/runtime/helpers/esm/extends'
    }
  }
}
