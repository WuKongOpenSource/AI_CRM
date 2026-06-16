import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { readFileSync } from 'fs'
import { resolve } from 'path'

const devProxyTarget = process.env.VITE_DEV_PROXY_TARGET || 'http://127.0.0.1:8088'
const syncProxyTarget = process.env.VITE_SYNC_DEV_PROXY_TARGET || 'http://127.0.0.1:10456'
const packageJson = JSON.parse(readFileSync(resolve(__dirname, './package.json'), 'utf-8')) as { version?: string }
const appVersion = typeof packageJson.version === 'string' && packageJson.version.trim()
  ? packageJson.version.trim()
  : '1.0.0'

export default defineConfig({
  base: './',
  plugins: [vue()],
  define: {
    'import.meta.env.VITE_APP_VERSION': JSON.stringify(appVersion)
  },
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
