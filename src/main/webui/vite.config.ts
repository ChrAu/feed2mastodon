import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
    plugins: [react()],
    build: {
        outDir: 'build', // Damit es mit dem Standard-Output von Quinoa/CRA kompatibel bleibt
        target: 'es2020', // Entspricht etwa "not dead" und modernen Browsern
    },
    server: {
        host: '0.0.0.0', // Erlaubt Quinoa den Zugriff
        port: 5173,      // Der Port, den Quinoa erwartet
    }
})
