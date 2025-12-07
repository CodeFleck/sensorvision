import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3001,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
    },
  },
  build: {
    // Pilot program performance optimizations
    rollupOptions: {
      output: {
        manualChunks: {
          // Vendor chunk for stable dependencies
          vendor: ['react', 'react-dom', 'react-router-dom'],
          
          // Charts chunk for visualization libraries
          charts: ['chart.js', 'react-chartjs-2', 'chartjs-adapter-date-fns'],
          
          // UI chunk for UI components and styling
          ui: [
            '@headlessui/react', 
            '@heroicons/react', 
            'tailwindcss',
            'clsx'
          ],
          
          // Utils chunk for utility libraries
          utils: [
            'date-fns',
            'lodash',
            'axios'
          ],
          
          // Map chunk for mapping libraries (if used)
          maps: ['leaflet', 'react-leaflet']
        }
      }
    },
    
    // Optimize chunk size warnings
    chunkSizeWarningLimit: 1000,
    
    // Enable source maps for production debugging (pilot program)
    sourcemap: true,
    
    // Optimize for pilot program deployment
    target: 'es2020',
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: false, // Keep console logs for pilot debugging
        drop_debugger: true,
        pure_funcs: ['console.debug'] // Remove debug logs only
      }
    }
  },
  
  // Optimize dependencies for pilot program
  optimizeDeps: {
    include: [
      'react',
      'react-dom',
      'react-router-dom',
      'chart.js',
      'react-chartjs-2',
      '@headlessui/react',
      '@heroicons/react/24/outline',
      '@heroicons/react/24/solid',
      'date-fns',
      'axios'
    ]
  }
})