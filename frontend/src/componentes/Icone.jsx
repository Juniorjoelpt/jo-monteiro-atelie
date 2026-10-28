// Ícones em traço, desenhados para o ateliê (sem dependências externas).
const CAMINHOS = {
  painel: <path d="M3 11 12 3l9 8M5 9.5V20h5v-6h4v6h5V9.5" />,
  orcamento: <><path d="M6 3h9l4 4v14H6z" /><path d="M14 3v5h5M9 13h7M9 17h5" /></>,
  encomenda: <path d="M5 4h14M8 4v9M12 4v13M16 4v9M8 13l4 6 4-6" />,
  oficina: <><circle cx="8" cy="8" r="3" /><circle cx="17" cy="9" r="2.5" /><path d="M2 20c0-3.5 2.7-6 6-6s6 2.5 6 6M15 20c0-2.6 1.3-5 2.5-5S21 17 21 20" /></>,
  cliente: <path d="M12 20.5s-8-5.2-8-11a4.5 4.5 0 0 1 8-2.8 4.5 4.5 0 0 1 8 2.8c0 5.8-8 11-8 11z" />,
  financeiro: <path d="M3 12h4l3-7 4 14 3-7h4" />,
  material: <><circle cx="12" cy="12" r="8" /><path d="M5 9c4 1 10 1 14 0M5 15c4-1 10-1 14 0" /></>,
  portfolio: <><rect x="3" y="5" width="18" height="14" rx="3" /><circle cx="9" cy="10" r="1.8" /><path d="m21 16-5-5-8 8" /></>,
  config: <><circle cx="12" cy="12" r="3" /><path d="M12 2v3M12 19v3M4.2 4.2l2.1 2.1M17.7 17.7l2.1 2.1M2 12h3M19 12h3M4.2 19.8l2.1-2.1M17.7 6.3l2.1-2.1" /></>,
  sair: <path d="M15 4h4v16h-4M10 8l-4 4 4 4M6 12h10" />,
  site: <><circle cx="12" cy="12" r="9" /><path d="M3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18" /></>,
  mais: <path d="M12 5v14M5 12h14" />,
  busca: <><circle cx="11" cy="11" r="7" /><path d="m20 20-4-4" /></>,
  fechar: <path d="M6 6l12 12M18 6 6 18" />,
  menu: <path d="M4 7h16M4 12h16M4 17h16" />,
  whats: <path d="M3.5 20.5 5 16a8.5 8.5 0 1 1 3.5 3.5zM9 8.5c0 3.6 2.9 6.5 6.5 6.5l1-1.5-2-1-1 1c-1.4-.5-2.5-1.6-3-3l1-1-1-2z" />,
  editar: <path d="M4 20h4L19 9l-4-4L4 16zM13.5 6.5l4 4" />,
  lixo: <path d="M4 7h16M10 7V4h4v3M6 7l1 13h10l1-13M10 11v6M14 11v6" />,
  check: <path d="m5 12 5 5 9-10" />,
  dinheiro: <><rect x="3" y="6" width="18" height="12" rx="3" /><circle cx="12" cy="12" r="2.5" /></>,
  calendario: <><rect x="3" y="5" width="18" height="16" rx="3" /><path d="M3 10h18M8 3v4M16 3v4" /></>,
  pix: <path d="m12 3 4 4-4 4-4-4zM12 13l4 4-4 4-4-4zM3 12l4-4 4 4-4 4zM13 12l4-4 4 4-4 4z" />,
  alerta: <><path d="M12 3 2 20h20z" /><path d="M12 10v4M12 17v.5" /></>,
  seta: <path d="m9 5 7 7-7 7" />,
  play: <path d="M7 5v14l12-7z" />,
  grafico: <path d="M4 20V10M10 20V4M16 20v-7M22 20H2" />,
  sino: <><path d="M6 16V11a6 6 0 0 1 12 0v5l2 2H4z" /><path d="M10 21h4" /></>,
  pdf: <><path d="M6 3h9l4 4v14H6z" /><path d="M14 3v5h5M9 14h6M9 17h4" /></>,
  baixar: <path d="M12 4v11M7 10l5 5 5-5M5 20h14" />,
  etiqueta: <><path d="M3 12V4h8l10 10-8 8z" /><circle cx="7.5" cy="8.5" r="1.5" /></>,
  sacola: <><path d="M5 8h14l-1 13H6z" /><path d="M9 8V6a3 3 0 0 1 6 0v2" /></>,
  menos: <path d="M5 12h14" />,
  recibo: <><path d="M6 3h12v18l-3-2-3 2-3-2-3 2z" /><path d="M9 8h6M9 12h6M9 16h3" /></>,
  carrinho: <><path d="M3 4h2.5l2.2 11h10.6L20.5 7H7" /><circle cx="9.5" cy="19.5" r="1.5" /><circle cx="17" cy="19.5" r="1.5" /></>,
  aspas: <path d="M5 17c2.5 0 4-1.8 4-4.5V7H4v5.5h3M15 17c2.5 0 4-1.8 4-4.5V7h-5v5.5h3" />,
  estrela: <path d="m12 3.5 2.6 5.3 5.9.9-4.3 4.1 1 5.8L12 16.9l-5.2 2.7 1-5.8L3.5 9.7l5.9-.9z" />,
  certificado: <><rect x="3" y="4" width="18" height="12" rx="2" /><circle cx="12" cy="17" r="3" /><path d="M10.5 19.5 10 22l2-1 2 1-.5-2.5M7 8h10M8 11h8" /></>,
  link: <path d="M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1" />,
  equipe: <><circle cx="9" cy="8" r="3.2" /><path d="M3 20c0-3.4 2.7-6 6-6s6 2.6 6 6" /><path d="M16 4.5a3 3 0 0 1 0 6M18 14.5c1.9.8 3 2.8 3 5.5" /></>,
  historico: <><path d="M3.5 12a8.5 8.5 0 1 0 2.5-6L3.5 8.5" /><path d="M3.5 3.5v5h5M12 8v4.5l3 2" /></>,
  cadeado: <><rect x="5" y="11" width="14" height="10" rx="2.5" /><path d="M8 11V8a4 4 0 0 1 8 0v3" /></>,
  video: <><rect x="3" y="5" width="13" height="14" rx="3" /><path d="m16 10 5-3v10l-5-3" /></>,
  setaCima: <path d="m6 15 6-6 6 6" />,
  setaBaixo: <path d="m6 9 6 6 6-6" />,
  foto: <><rect x="3" y="6" width="18" height="14" rx="3" /><circle cx="12" cy="13" r="3.5" /><path d="M8 6l1.5-2h5L16 6" /></>,
  pincel: <><path d="M14.5 3.5 20.5 9.5 11 19l-6-6z" /><path d="M8 16c-2 0-4 1.5-4 4.5 2.5 0 5-1 5.5-3" /><path d="M12.5 5.5 18.5 11.5" /></>,
  caderno: <><rect x="5" y="3" width="14" height="18" rx="2.5" /><path d="M9 3v18M3.5 7h3M3.5 12h3M3.5 17h3M12 8h4" /></>,
  caneca: <><path d="M4 7h12v9a4 4 0 0 1-4 4H8a4 4 0 0 1-4-4z" /><path d="M16 9h1.5a2.5 2.5 0 0 1 0 5H16M8 3v2M12 3v2" /></>,
  presente: <><rect x="3.5" y="8" width="17" height="4" rx="1" /><path d="M5 12v8h14v-8M12 8v12M12 8c-1.5-3-5-4-5-1.5S10 8 12 8zM12 8c1.5-3 5-4 5-1.5S14 8 12 8z" /></>,
  olho: <><path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z" /><circle cx="12" cy="12" r="3" /></>,
  copiar: <><rect x="8" y="8" width="12" height="12" rx="2.5" /><path d="M16 8V5.5A1.5 1.5 0 0 0 14.5 4h-9A1.5 1.5 0 0 0 4 5.5v9A1.5 1.5 0 0 0 5.5 16H8" /></>,
};

export default function Icone({ nome, tamanho = 22, cor = 'currentColor', largura = 2, ...resto }) {
  return (
    <svg aria-hidden="true" width={tamanho} height={tamanho} viewBox="0 0 24 24" fill="none" stroke={cor}
      strokeWidth={largura} strokeLinecap="round" strokeLinejoin="round" {...resto}>
      {CAMINHOS[nome]}
    </svg>
  );
}
