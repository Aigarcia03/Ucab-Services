import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
    plugins: [vue()],
    server: {
        host: '127.0.0.1',
        port: 5173 // Usar puerto 5173 por defecto de Vite para evitar bloqueos en 3000
    }
})