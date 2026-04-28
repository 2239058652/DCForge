import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
    plugins: [react()],
    base: '/',
    resolve: {
        alias: {
            '@': path.resolve(__dirname, 'src')
        }
    },
    server: {
        port: 8090,
        proxy: {
            '/api': {
                target: 'http://172.19.22.213:5000',
                changeOrigin: true,
                rewrite: (path) => path
            }
        }
    }
})
