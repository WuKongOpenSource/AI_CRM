import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';
var devProxyTarget = process.env.VITE_DEV_PROXY_TARGET || 'http://127.0.0.1:8088';
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
                rewrite: function (path) { return path.replace(/^\/crmapi/, ''); }
            }
        }
    },
    css: {
        postcss: './postcss.config.js'
    }
});
