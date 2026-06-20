import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
    plugins: [vue()],
    server: {
        port: 3000 // Se ejecutará en el puerto 3000 para no chocar con el backend
    }
})