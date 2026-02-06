import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';
export default defineConfig({
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
                target: 'http://localhost:8088/',
                changeOrigin: true,
                rewrite: function (path) { return path.replace(/^\/crmapi/, ''); }
            }
        }
    },
    css: {
        postcss: './postcss.config.js'
    }
});
