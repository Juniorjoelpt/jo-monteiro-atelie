// Formatação e rótulos em português

const moedaFmt = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
export const moeda = (v) => moedaFmt.format(Number(v || 0));
export const moedaCurta = (v) => {
  const n = Number(v || 0);
  if (n >= 1000) return `R$ ${(n / 1000).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} mil`;
  return moedaFmt.format(n).replace(',00', '');
};

/** "2026-10-24" → "24/10/2026" sem problemas de fuso. */
export function data(iso) {
  if (!iso) return '—';
  const [a, m, d] = iso.slice(0, 10).split('-');
  return `${d}/${m}/${a}`;
}

export function dataCurta(iso) {
  if (!iso) return '—';
  const d = new Date(iso.length === 10 ? `${iso}T12:00:00` : iso);
  return d.toLocaleDateString('pt-BR', { weekday: 'short', day: '2-digit', month: '2-digit' }).replace('.', '');
}

export function dataHora(iso) {
  if (!iso) return '—';
  const d = new Date(iso);
  return d.toLocaleString('pt-BR', { weekday: 'short', day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' }).replace('.', '');
}

export function hora(iso) {
  if (!iso) return '';
  return new Date(iso).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

export const hojeIso = () => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
};

/** "5598988793891" → "(98) 98879-3891" */
export function telefone(w) {
  if (!w) return '—';
  const d = w.replace(/\D/g, '').replace(/^55/, '');
  if (d.length === 11) return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
  if (d.length === 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
  return w;
}

/** Link de orçamento que a Jô já usa no Instagram: vale enquanto o WhatsApp não for cadastrado no sistema. */
export const LINK_ORCAMENTO_INSTAGRAM = 'https://abre.ai/jomonteirodesigner';

export function linkWhats(numero, texto) {
  if (!numero) return null;
  const n = numero.replace(/\D/g, '');
  return `https://wa.me/${n}${texto ? `?text=${encodeURIComponent(texto)}` : ''}`;
}

/** WhatsApp do ateliê (no site): sem número cadastrado, cai no link de orçamento do Instagram. */
export const whatsAtelie = (numero, texto) => linkWhats(numero, texto) || LINK_ORCAMENTO_INSTAGRAM;

export const primeiroNome = (nome) => (nome || '').trim().split(/\s+/)[0] || '';

// ---------- Rótulos e cores dos status ----------

export const STATUS_ORCAMENTO = {
  NOVO: ['Novo', 'terra'],
  RESPONDIDO: ['Respondido', 'ocre'],
  APROVADO: ['Aprovado', 'oliva'],
  RECUSADO: ['Recusado', 'areia'],
};

export const STATUS_ENCOMENDA = {
  AGUARDANDO_SINAL: ['Aguardando sinal', 'ocre'],
  EM_PRODUCAO: ['Em produção', 'terra'],
  PRONTA: ['Pronta', 'oliva'],
  ENTREGUE: ['Entregue', 'areia'],
  CANCELADA: ['Cancelada', 'perigo'],
};

/** Andamento da arte das peças personalizadas. [rótulo, cor, explicação curta] */
export const STATUS_ARTE = {
  SEM_ARTE: ['Sem arte', 'areia', 'Não passa por aprovação'],
  A_CRIAR: ['Arte a criar', 'ocre', 'Falta fazer a arte'],
  AGUARDANDO_CLIENTE: ['Arte com a cliente', 'oliva', 'Esperando a aprovação pelo link'],
  AJUSTE_PEDIDO: ['Ajuste pedido', 'terra', 'A cliente pediu mudança'],
  APROVADA: ['Arte aprovada', 'oliva', 'Pode produzir'],
};

export const FRENTES = { PAPELARIA: 'Papelaria', PORCELANA: 'Porcelana (canecas)', PRESENTE: 'Presentes' };

export const TIPOS_CLIENTE = { CLIENTE: 'Cliente', ESCOLA: 'Escola', EMPRESA: 'Empresa', PARCEIRO: 'Parceiro(a)' };

export const CATEGORIAS_LANC = {
  PAPELARIA: 'Papelaria', PORCELANA: 'Porcelana (canecas)', PRESENTE: 'Presentes',
  VENDA: 'Pronta-entrega', MATERIAL: 'Materiais', CONTAS_FIXAS: 'Contas fixas', OUTROS: 'Outros',
};

export const CANAIS = { ATELIE: 'Ateliê', INSTAGRAM: 'Instagram', WHATSAPP: 'WhatsApp', FEIRA: 'Feira/evento', OUTRO: 'Outro' };

/** Quantidade com até 3 casas: 0,25 · 40 · 1,125 */
export const qtd = (n) => Number(n || 0).toLocaleString('pt-BR', { maximumFractionDigits: 3 });

/** "90 metros", "1 unidade", "2,5 rolos", "300 g" (abreviações ficam como estão). */
export function qtdUn(n, unidade) {
  const u = unidade || '';
  const plural = Math.abs(Number(n)) > 1 && u.length > 2 && /[aeiou]$/i.test(u);
  return `${qtd(n)} ${plural ? `${u}s` : u}`;
}

/** Custo unitário de material, com mais casas para valores pequenos (R$ 0,035/g). */
export const moedaUnit = (v) => {
  const n = Number(v || 0);
  return n.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: 2, maximumFractionDigits: n > 0 && n < 1 ? 4 : 2 });
};

const r2 = (n) => Math.round((n + Number.EPSILON) * 100) / 100;

/**
 * Mesmo cálculo do servidor, para mostrar a composição do preço enquanto ela digita.
 * prec = { valorHora, custoFixoHora, margemPadrao }
 */
export function calcularFicha({ horas, outros, margem, precoVenda, itens }, materiaisPorId, prec) {
  const materiais = r2(itens.reduce((s, i) => s + (Number(i.quantidade) || 0) * Number(materiaisPorId[i.materialId]?.custoUnitario || 0), 0));
  const h = Number(horas) || 0;
  const maoObra = r2(h * Number(prec.valorHora || 0));
  const fixo = r2(h * Number(prec.custoFixoHora || 0));
  const o = Number(outros) || 0;
  const total = r2(materiais + maoObra + fixo + o);
  const m = margem === '' || margem == null ? Number(prec.margemPadrao || 0) : Number(margem);
  const sugerido = total > 0 ? Math.ceil(Number((total * (1 + m / 100)).toFixed(6))) : 0;
  const atual = precoVenda !== '' && precoVenda != null && Number(precoVenda) > 0 ? Number(precoVenda) : sugerido;
  return {
    custoMateriais: materiais, custoMaoObra: maoObra, custoFixo: fixo, outrosCustos: o, custoTotal: total,
    margem: m, precoSugerido: sugerido, precoAtual: atual, lucroUnitario: r2(atual - total),
    ganhoPorHora: h > 0 ? r2((atual - materiais - fixo - o) / h) : null,
  };
}

export const FORMAS = { PIX: 'Pix', DINHEIRO: 'Dinheiro', CARTAO: 'Cartão', OUTRO: 'Outro' };

export const CATEGORIAS_PORTFOLIO = {
  PAPELARIA: 'Papelaria', CANECAS: 'Canecas', PRESENTES: 'Presentes', DATAS_ESPECIAIS: 'Datas especiais',
};

/** O que a cliente escolhe no formulário de orçamento do site. */
export const TIPOS_PECA = [
  'Caderneta ou agenda',
  'Caneca',
  'Garrafa ou squeeze',
  'Bíblia ou capa personalizada',
  'Kit ou cesta de presente',
  'Lembrancinhas (festa, escola, empresa)',
  'Outro',
];

/** Cores dos gráficos: laranja da marca, roxo e azul-jeans da mascote, amarelo da caderneta… (validadas para daltonismo). */
export const CORES_GRAFICO = ['#D8501A', '#6A47A8', '#1F6E8C', '#E3A21B', '#C2507A', '#3E8E5A'];

/** Dias de hoje até a data (negativo = já passou). */
export function diasAte(iso) {
  if (!iso) return null;
  const [a, m, d] = iso.slice(0, 10).split('-').map(Number);
  const alvo = Date.UTC(a, m - 1, d);
  const h = new Date();
  const hoje = Date.UTC(h.getFullYear(), h.getMonth(), h.getDate());
  return Math.round((alvo - hoje) / 86400000);
}

/** Prazo para os cartões do quadro: [texto, cor do selo] ou null sem data. */
export function prazoSelo(iso) {
  const n = diasAte(iso);
  if (n == null) return null;
  if (n < 0) return [n === -1 ? 'Atrasada 1 dia' : `Atrasada ${-n} dias`, 'perigo'];
  if (n === 0) return ['Entrega hoje', 'terra'];
  if (n === 1) return ['Entrega amanhã', 'ocre'];
  if (n <= 7) return [`Em ${n} dias`, 'ocre'];
  return [data(iso), 'areia'];
}

/** "2026-11-14" → "sexta-feira, 14 de novembro" */
export function dataExtensa(iso) {
  if (!iso) return '';
  return new Date(`${iso.slice(0, 10)}T12:00:00`).toLocaleDateString('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' });
}

/**
 * Etapas do pedido como a cliente vê: confirmado (sinal), arte (se tiver), produção, pronta, entregue.
 * Cada etapa: { chave, rotulo, feito, texto }. A etapa atual é a primeira não feita.
 */
export function etapasPedido({ status, statusArte, valorSinal }) {
  const depois = (lista) => lista.includes(status);
  const confirmado = status !== 'AGUARDANDO_SINAL';
  const temArte = statusArte && statusArte !== 'SEM_ARTE';
  const arteOk = !temArte || statusArte === 'APROVADA';
  const etapas = [
    { chave: 'pedido', rotulo: 'Pedido confirmado', feito: confirmado,
      texto: confirmado ? 'Sinal recebido: a sua data está garantida.'
        : Number(valorSinal) > 0 ? `Falta o sinal de ${moeda(valorSinal)} para garantir a sua data.` : 'Esperando a confirmação do pedido.' },
  ];
  if (temArte) {
    etapas.push({ chave: 'arte', rotulo: 'Arte aprovada', feito: statusArte === 'APROVADA',
      texto: {
        A_CRIAR: 'Estou criando a arte da sua peça.',
        AGUARDANDO_CLIENTE: 'A sua arte está pronta! Confira e responda logo abaixo.',
        AJUSTE_PEDIDO: 'Estou fazendo os ajustes que você pediu.',
        APROVADA: 'Você aprovou a arte.',
      }[statusArte] });
  }
  etapas.push(
    { chave: 'producao', rotulo: 'Em produção', feito: depois(['PRONTA', 'ENTREGUE']),
      texto: depois(['PRONTA', 'ENTREGUE']) ? 'Produzida à mão, com carinho.'
        : status === 'EM_PRODUCAO' && arteOk ? 'A sua peça está sendo feita agora.' : 'Começa assim que as etapas acima ficarem prontas.' },
    { chave: 'pronta', rotulo: 'Pronta', feito: depois(['PRONTA', 'ENTREGUE']),
      texto: status === 'PRONTA' ? 'Prontinha e embalada! Vou combinar a entrega com você.' : status === 'ENTREGUE' ? 'Ficou pronta.' : 'Você recebe uma mensagem quando ficar pronta.' },
    { chave: 'entregue', rotulo: 'Entregue', feito: status === 'ENTREGUE', texto: status === 'ENTREGUE' ? 'Entregue. Espero que você ame!' : '' },
  );
  const atual = etapas.findIndex((e) => !e.feito);
  return etapas.map((e, i) => ({ ...e, atual: i === atual }));
}
