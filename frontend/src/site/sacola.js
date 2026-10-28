import { useSyncExternalStore } from 'react';

// Sacola do site: as peças que a cliente escolhe para pedir orçamento.
// Fica só no navegador dela (localStorage) e é compartilhada por todas as partes da página.

const CHAVE = 'jo.sacola';
let itens = ler();
const ouvintes = new Set();

function ler() {
  try {
    const v = JSON.parse(localStorage.getItem(CHAVE) || '[]');
    return Array.isArray(v) ? v.filter((i) => i && Number(i.id) > 0 && Number(i.quantidade) > 0) : [];
  } catch {
    return [];
  }
}

function gravar(novos) {
  itens = novos;
  try { localStorage.setItem(CHAVE, JSON.stringify(itens)); } catch { /* navegador sem armazenamento: fica só na memória */ }
  ouvintes.forEach((f) => f());
}

// Outra aba do mesmo navegador mexeu na sacola
if (typeof window !== 'undefined') {
  window.addEventListener('storage', (e) => { if (e.key === CHAVE) { itens = ler(); ouvintes.forEach((f) => f()); } });
}

export const sacola = {
  adicionar(id, quantidade = 1) {
    const atual = itens.find((i) => i.id === id);
    gravar(atual ? itens.map((i) => (i.id === id ? { ...i, quantidade: Math.min(5000, i.quantidade + quantidade) } : i))
      : [...itens, { id, quantidade }]);
  },
  mudar(id, quantidade) {
    if (quantidade < 1) { sacola.tirar(id); return; }
    gravar(itens.map((i) => (i.id === id ? { ...i, quantidade: Math.min(5000, quantidade) } : i)));
  },
  tirar(id) { gravar(itens.filter((i) => i.id !== id)); },
  limpar() { gravar([]); },
};

/** Itens da sacola ligados às peças do catálogo (peça que saiu do catálogo some da sacola). */
export function useSacola(catalogo) {
  const lista = useSyncExternalStore((f) => { ouvintes.add(f); return () => ouvintes.delete(f); }, () => itens);
  if (!catalogo) return { itens: [], quantidade: 0, total: 0, semPreco: false, tem: () => false };
  const porId = new Map(catalogo.map((p) => [p.id, p]));
  const validos = lista.filter((i) => porId.has(i.id)).map((i) => ({ ...i, peca: porId.get(i.id) }));
  return {
    itens: validos,
    quantidade: validos.reduce((s, i) => s + i.quantidade, 0),
    total: validos.reduce((s, i) => s + (i.peca.preco != null ? Number(i.peca.preco) * i.quantidade : 0), 0),
    semPreco: validos.some((i) => i.peca.preco == null),
    tem: (id) => validos.some((i) => i.id === id),
  };
}
