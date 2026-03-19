import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
    plugins: [react()],
    build: {
        outDir: 'dist', // Standard Output-Verzeichnis für Vite und auch erwartet von Quarkus Quinoa
        target: 'es2020', // Entspricht etwa "not dead" und modernen Browsern
        chunkSizeWarningLimit: 1000 // Erhöht das Limit auf 1000 kB (1 MB), um Warnungen bei etwas größeren Chunks zu vermeiden
    },
    server: {
        host: '0.0.0.0', // Erlaubt Quinoa den Zugriff
        port: 5173,      // Der Port, den Quinoa erwartet
    }
})
