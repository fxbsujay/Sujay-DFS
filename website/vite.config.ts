import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import Components from 'unplugin-vue-components/vite'
import { AntDesignVueResolver } from 'unplugin-vue-components/resolvers'
import * as path from "path";

export default defineConfig({
  resolve: {
    alias: {
        '@': path.resolve(__dirname,'./src')
    }
  },
  plugins: [
      vue(),
      vueJsx({

      }),
      Components({
        resolvers: [AntDesignVueResolver()]
      })
  ],
  server: {
      open: true,
      port: 4000,
      proxy: {
          '/api': {
              target: 'http://localhost:8001',
              changeOrigin: true,
              rewrite: (path) => path.replace(/^\/api/, '')
          }
      }
  },
  css: {
    preprocessorOptions: {
      less: {
        lessOptions: {
          modifyVars: {
            'primary-color': '#1DA57A',
            'link-color': '#1DA57A',
            'border-radius-base': '2px',
          },
          javascriptEnabled: true,
        }
      }
    }
  }
})
