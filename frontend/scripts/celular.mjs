// npm run celular: abre o site e o sistema para o celular na mesma rede Wi-Fi.
// Mostra o endereço com o IP do computador e um QR Code; o backend continua no NetBeans (porta 8080).
import { spawn } from 'node:child_process';
import { networkInterfaces } from 'node:os';
import { createRequire } from 'node:module';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const require = createRequire(import.meta.url);
const qrcode = require('qrcode-terminal');
const PORTA = 5173;
const pasta = join(dirname(fileURLToPath(import.meta.url)), '..');

/** IP do computador na rede local (prefere o Wi-Fi 192.168.x.x). */
function ipLocal() {
  const lista = [];
  for (const [nome, ends] of Object.entries(networkInterfaces())) {
    for (const e of ends || []) {
      if (e.family === 'IPv4' && !e.internal) lista.push({ nome, ip: e.address });
    }
  }
  const nota = (x) => (x.ip.startsWith('192.168.') ? 0 : x.ip.startsWith('10.') ? 1 : 2)
    + (/wi-?fi|wlan|sem fio/i.test(x.nome) ? -0.5 : 0) + (/vethernet|virtual|vmware|vbox|docker|wsl/i.test(x.nome) ? 5 : 0);
  return lista.sort((a, b) => nota(a) - nota(b))[0]?.ip;
}

async function backendNoAr() {
  try {
    const r = await fetch('http://localhost:8080/api/publico/site', { signal: AbortSignal.timeout(2500) });
    return r.status < 500;
  } catch {
    return false;
  }
}

const ip = ipLocal();
const base = ip ? `http://${ip}:${PORTA}` : null;

console.log('\n  Ateliê Jô Monteiro · abrir no celular\n');
if (!base) {
  console.log('  Não achei o IP deste computador na rede. Confira se o Wi-Fi está conectado.\n');
} else {
  console.log(`  Site:     ${base}`);
  console.log(`  Sistema:  ${base}/admin\n`);
  console.log('  Aponte a câmera do celular (no mesmo Wi-Fi) para abrir o sistema:\n');
  qrcode.generate(`${base}/admin`, { small: true }, (q) => console.log(q.replace(/^/gm, '  ')));
}
if (!(await backendNoAr())) {
  console.log('  Atenção: o backend não respondeu em http://localhost:8080.');
  console.log('  Rode o projeto "backend" no NetBeans antes, senão as telas ficam sem dados.\n');
}
console.log('  Não abriu no celular? Libere a porta no Windows (PowerShell como administrador, uma vez só):');
console.log(`  New-NetFirewallRule -DisplayName "Atelie Jo Monteiro" -Direction Inbound -Protocol TCP -LocalPort 8080,${PORTA} -Action Allow -Profile Private`);
console.log('  e deixe a rede Wi-Fi como Privada. Para parar: Ctrl+C.\n');

const vite = spawn(process.execPath, [join(pasta, 'node_modules', 'vite', 'bin', 'vite.js'), '--host', '--port', String(PORTA), '--strictPort'],
  { cwd: pasta, stdio: 'inherit' });
vite.on('exit', (codigo) => process.exit(codigo ?? 0));
for (const sinal of ['SIGINT', 'SIGTERM']) process.on(sinal, () => vite.kill(sinal));
