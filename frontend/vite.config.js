import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Em desenvolvimento, /api e /uploads vão para o Spring Boot na porta 8080.
// "npm run build" gera o site dentro do backend (src/main/resources/static),
// então o Spring Boot serve tudo junto no mesmo endereço.
// Celular na mesma rede: http://<IP do computador>:8080 (libere a porta no firewall do Windows).
// Para o navegador, site e API estão no mesmo endereço (o Vite repassa). Sem tirar o cabeçalho Origin,
// o Spring Boot recusaria por CORS quem abre pelo IP da rede (celular): ele só conhece http://localhost:5173.
const backend = {
  target: 'http://localhost:8080',
  configure: (proxy) => proxy.on('proxyReq', (req) => req.removeHeader('origin')),
};

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Aceita o celular na mesma rede Wi-Fi (http://<IP do computador>:5173)
    host: true,
    proxy: {
      '/api': backend,
      '/uploads': backend,
    },
  },
  build: {
    outDir: '../backend/src/main/resources/static',
    emptyOutDir: true,
  },
});
