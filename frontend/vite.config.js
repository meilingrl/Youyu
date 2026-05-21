import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import fs from 'node:fs'
import path from 'node:path'

const rootDir = fs.realpathSync.native(process.cwd())

if (process.cwd() !== rootDir) {
  process.chdir(rootDir)
}

export default defineConfig({
  root: rootDir,
  cacheDir: `${rootDir}/node_modules/.vite`,
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.join(rootDir, 'src')
    }
  },
  build: {
    outDir: 'dist',
    rollupOptions: {
      input: {
        app: path.join(rootDir, 'index.html')
      },
      output: {
        manualChunks(id) {
          if (id.includes('node_modules/element-plus')) {
            return 'element-plus'
          }

          if (id.includes('node_modules/vue') || id.includes('node_modules/pinia') || id.includes('node_modules/vue-router')) {
            return 'vue-core'
          }

          if (id.includes('node_modules')) {
            return 'vendor'
          }
        }
      }
    }
  },
  test: {
    environment: 'happy-dom',
    exclude: ['e2e/**']
  },
  server: {
    host: '0.0.0.0',
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
