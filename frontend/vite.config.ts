import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5174,
    host: 'localhost',
    proxy: {
      '/zk': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
