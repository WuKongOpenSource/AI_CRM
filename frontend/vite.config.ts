import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

const devProxyTarget = process.env.VITE_DEV_PROXY_TARGET || 'http://192.168.1.116:8088'
const syncProxyTarget = process.env.VITE_SYNC_DEV_PROXY_TARGET || 'http://127.0.0.1:10456'

export default defineConfig({
  base: './',
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, './src')
    }
  },
  server: {
    port: 5173,
    host: true,
    allowedHosts: true,
    proxy: {
      '/crmapi': {
        target: devProxyTarget,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/crmapi/, '')
      },
      '/syncapi': {
        target: syncProxyTarget,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/syncapi/, '')
      }
    }
  },
  css: {
    postcss: './postcss.config.js'
  }
})
