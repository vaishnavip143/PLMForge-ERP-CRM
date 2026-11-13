import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// The Spring Boot backend allows http://localhost:5173 in its CORS configuration.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
  },
})
