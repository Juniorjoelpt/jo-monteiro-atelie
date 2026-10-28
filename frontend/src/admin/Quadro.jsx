import { useState } from 'react';
import { api, sessao } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { useToast } from '../componentes/ui.jsx';
import { STATUS_ARTE, diasAte, prazoSelo } from '../util.js';

const ARTE_PENDENTE = ['A_CRIAR', 'AGUARDANDO_CLIENTE', 'AJUSTE_PEDIDO'];

/**
 * Colunas do quadro, na ordem do trabalho. "Arte" e "Produzir" são as duas metades de "Em produção":
 * enquanto a arte não é aprovada a peça espera; aprovada (ou sem arte), vai para a bancada.
 */
const COLUNAS = [
  { chave: 'sinal', titulo: 'Aguardando sinal', cor: 'var(--ocre)', dica: 'Falta o sinal para garantir a data.',
    filtro: (e) => e.status === 'AGUARDANDO_SINAL' },
  { chave: 'arte', titulo: 'Arte em andamento', cor: 'var(--oliva)', dica: 'Criando, com a cliente ou em ajuste.',
    filtro: (e) => e.status === 'EM_PRODUCAO' && ARTE_PENDENTE.includes(e.statusArte) },
  { chave: 'produzir', titulo: 'Produzir', cor: 'var(--terra-vivo)', dica: 'Arte aprovada: pode ir para a bancada.',
    filtro: (e) => e.status === 'EM_PRODUCAO' && !ARTE_PENDENTE.includes(e.statusArte), proximo: 'PRONTA', acao: 'Pronta' },
  { chave: 'pronta', titulo: 'Prontas', cor: '#2F7A4A', dica: 'Embaladas, esperando a entrega.',
    filtro: (e) => e.status === 'PRONTA', proximo: 'ENTREGUE', acao: 'Entregue' },
  { chave: 'entregue', titulo: 'Entregues (7 dias)', cor: 'var(--marrom-3)', dica: 'O que saiu na última semana.',
    filtro: (e) => e.status === 'ENTREGUE' && (diasAte(e.entregueEm) ?? -99) >= -7 },
];

/** Urgentes primeiro; depois a entrega mais próxima; sem data por último. */
function ordenar(a, b) {
  if (a.urgente !== b.urgente) return a.urgente ? -1 : 1;
  const pa = a.prazoEntrega || '9999-12-31';
  const pb = b.prazoEntrega || '9999-12-31';
  if (pa !== pb) return pa < pb ? -1 : 1;
  return (a.criadoEm || '') < (b.criadoEm || '') ? -1 : 1;
}

export default function Quadro({ encomendas, aoAbrir, aoMudar }) {
  const [ocupado, setOcupado] = useState(null);
  const [erro, setErro] = useState(null);
  const toast = useToast();
  const ajudante = sessao.ajudante();

  async function avancar(e, status, msg) {
    setErro(null);
    setOcupado(e.id);
    try {
      await api.patch(`/api/encomendas/${e.id}/status`, { status });
      toast(msg);
      aoMudar();
    } catch (err) { setErro(err.message); } finally { setOcupado(null); }
  }

  async function urgente(e) {
    setErro(null);
    setOcupado(e.id);
    try {
      await api.patch(`/api/encomendas/${e.id}/urgente`, { urgente: !e.urgente });
      toast(e.urgente ? 'Tirou a urgência.' : 'Marcada como urgente.');
      aoMudar();
    } catch (err) { setErro(err.message); } finally { setOcupado(null); }
  }

  const atrasadas = encomendas.filter((e) => ['AGUARDANDO_SINAL', 'EM_PRODUCAO', 'PRONTA'].includes(e.status) && (diasAte(e.prazoEntrega) ?? 0) < 0).length;

  return (
    <>
      {erro && <div className="aviso aviso-erro" role="alert" style={{ marginBottom: 12 }}>{erro}</div>}
      {atrasadas > 0 && (
        <div className="aviso aviso-erro" style={{ marginBottom: 12, display: 'flex', alignItems: 'center', gap: 10 }}>
          <Icone nome="alerta" tamanho={20} />{atrasadas === 1 ? '1 encomenda passou da data de entrega.' : `${atrasadas} encomendas passaram da data de entrega.`}
        </div>
      )}
      <div className="quadro" role="list" aria-label="Quadro de produção">
        {COLUNAS.map((c) => {
          const cartoes = encomendas.filter(c.filtro).sort(ordenar);
          return (
            <section key={c.chave} className="quadro-coluna" role="listitem" aria-labelledby={`col-${c.chave}`}>
              <header style={{ '--cor-coluna': c.cor }}>
                <h2 id={`col-${c.chave}`}>{c.titulo}<span className="quadro-conta">{cartoes.length}</span></h2>
                <p>{c.dica}</p>
              </header>
              <div className="quadro-cartoes">
                {cartoes.length === 0 && <p className="quadro-vazio">Nada por aqui.</p>}
                {cartoes.map((e) => {
                  const prazo = e.status === 'ENTREGUE' ? null : prazoSelo(e.prazoEntrega);
                  const pode = c.proximo && (!ajudante || ['PRONTA', 'ENTREGUE'].includes(c.proximo));
                  return (
                    <article key={e.id} className={`quadro-cartao ${e.urgente ? 'urgente' : ''}`}>
                      <button type="button" className="quadro-abrir" onClick={() => aoAbrir(e)} aria-label={`Abrir ${e.descricao}, de ${e.clienteNome}`}>
                        <span className="quadro-selos">
                          {e.urgente && <span className="selo selo-perigo selo-p"><Icone nome="raio" tamanho={12} />Urgente</span>}
                          {prazo && <span className={`selo selo-${prazo[1]} selo-p`}>{prazo[0]}</span>}
                          {!e.prazoEntrega && e.status !== 'ENTREGUE' && <span className="selo selo-areia selo-p">Sem data</span>}
                        </span>
                        <strong className="quadro-titulo">{e.descricao}</strong>
                        <span className="quadro-cliente">{e.clienteNome}</span>
                        {(e.quantidade > 1 || e.produtoNome) && <span className="quadro-peca">{e.quantidade}× {e.produtoNome || 'unidades'}</span>}
                        {c.chave === 'arte' && (
                          <span className="quadro-arte">
                            {STATUS_ARTE[e.statusArte][0]}
                            {e.statusArte === 'AGUARDANDO_CLIENTE' && ` · ${e.arteVistaEm ? 'já viu' : 'não abriu'}`}
                          </span>
                        )}
                      </button>
                      {(pode || !ajudante) && c.chave !== 'entregue' && (
                        <div className="quadro-acoes">
                          {!ajudante && (
                            <button type="button" className={`btn btn-icone btn-p ${e.urgente ? 'btn-perigo' : 'btn-fantasma'}`} disabled={ocupado === e.id}
                              aria-pressed={e.urgente} aria-label={e.urgente ? `Tirar urgência de ${e.descricao}` : `Marcar ${e.descricao} como urgente`}
                              title={e.urgente ? 'Tirar urgência' : 'Marcar como urgente'} onClick={() => urgente(e)}>
                              <Icone nome="raio" tamanho={16} />
                            </button>
                          )}
                          {pode && (
                            <button type="button" className="btn btn-claro btn-p" disabled={ocupado === e.id}
                              onClick={() => avancar(e, c.proximo, c.proximo === 'PRONTA' ? 'Encomenda pronta.' : 'Encomenda entregue.')}>
                              <Icone nome="check" tamanho={16} largura={3} />{c.acao}
                            </button>
                          )}
                        </div>
                      )}
                    </article>
                  );
                })}
              </div>
            </section>
          );
        })}
      </div>
    </>
  );
}
