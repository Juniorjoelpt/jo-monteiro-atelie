import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Em desenvolvimento, /api e /uploads vão para o Spring Boot na porta 8080.
// "npm run build" gera o site dentro do backend (src/main/resources/static),
// então o Spring Boot serve tudo junto no mesmo endereço.
// Celular na mesma rede: http://<IP do computador>:8080 (libere a porta no firewall do Windows).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Aceita o celular na mesma rede Wi-Fi (http://<IP do computador>:5173)
    host: true,
    proxy: {
      '/api': 'http://localhost:8080',
      '/uploads': 'http://localhost:8080',
    },
  },
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true,
  },
});
