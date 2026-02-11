import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  const basePath = env.VITE_BASE_PATH || '/'

  return {
    base: basePath,
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
        [`${basePath}crmapi`]: {
          target: 'http://localhost:8088/',
          changeOrigin: true,
          rewrite: (path) => path.replace(new RegExp(`^${basePath}crmapi`), '')
        }
      }
    },
    css: {
      postcss: './postcss.config.js'
    }
  }
})
