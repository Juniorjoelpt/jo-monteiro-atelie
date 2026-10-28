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
