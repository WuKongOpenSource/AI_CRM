import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';
export default defineConfig(function (_a) {
    var _b;
    var mode = _a.mode;
    var env = loadEnv(mode, process.cwd());
    var basePath = env.VITE_BASE_PATH || '/';
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
            proxy: (_b = {},
                _b["".concat(basePath, "crmapi")] = {
                    target: 'http://localhost:8088/',
                    changeOrigin: true,
                    rewrite: function (path) { return path.replace(new RegExp("^".concat(basePath, "crmapi")), ''); }
                },
                _b)
        },
        css: {
            postcss: './postcss.config.js'
        }
    };
});
