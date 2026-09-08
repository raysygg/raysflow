import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

export default defineConfig(({ mode }) => {
  const frontendRoot = path.dirname(fileURLToPath(import.meta.url))
  const projectRoot = path.resolve(frontendRoot, '..')
  // 同时读取项目根目录和 frontend 目录，后者优先，兼容两种启动位置。
  const env = {
    ...loadEnv(mode, projectRoot, ''),
    ...loadEnv(mode, frontendRoot, '')
  }
  const backendUrl = env.VITE_DEV_BACKEND_URL || 'http://localhost:8090'

  return {
    plugins: [vue()],
    server: {
      port: 5176,
      host: '0.0.0.0',
      proxy: {
        '/api': {
          target: backendUrl,
          changeOrigin: true
        },
        '/ws': {
          target: backendUrl,
          changeOrigin: true,
          ws: true
        }
      }
    }
  }
})
