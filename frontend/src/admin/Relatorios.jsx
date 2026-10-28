import { useState } from 'react';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Estado, useCarregar, useToast, Vazio } from '../componentes/ui.jsx';
import { data, hojeIso, moeda, moedaCurta, qtdUn } from '../util.js';
import { Topo } from './Layout.jsx';

// Par validado para daltonismo (protan/deutan/tritan) sobre o fundo cru dos cartões
const COR_ENTRADA = '#56701F';
const COR_SAIDA = '#E0884A';

const iso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

function periodo(chave) {
  const h = new Date();
  switch (chave) {
    case 'mes-passado': return [iso(new Date(h.getFullYear(), h.getMonth() - 1, 1)), iso(new Date(h.getFullYear(), h.getMonth(), 0))];
    case '3-meses': return [iso(new Date(h.getFullYear(), h.getMonth() - 2, 1)), hojeIso()];
    case 'ano': return [iso(new Date(h.getFullYear(), 0, 1)), hojeIso()];
    default: return [iso(new Date(h.getFullYear(), h.getMonth(), 1)), hojeIso()];
  }
}

const PRESETS = [['mes', 'Este mês'], ['mes-passado', 'Mês passado'], ['3-meses', 'Últimos 3 meses'], ['ano', 'Este ano']];

export default function Relatorios() {
  const [preset, setPreset] = useState('mes');
  const [[de, ate], setPeriodo] = useState(periodo('mes'));
  const [baixando, setBaixando] = useState(null);
  const toast = useToast();
  const rel = useCarregar(() => api.get(`/api/relatorios?de=${de}&ate=${ate}`), [de, ate]);
  const prec = useCarregar(() => api.get('/api/configuracoes/precificacao'));
  const r = rel.dados;

  function escolher(chave) {
    setPreset(chave);
    setPeriodo(periodo(chave));
  }

  async function exportar(formato) {
    setBaixando(formato);
    try {
      const nome = await api.baixar(`/api/relatorios/exportar?formato=${formato}&de=${de}&ate=${ate}`);
      toast(`${nome} baixado.`);
    } catch (e) {
      toast(e.message, 'erro');
    } finally {
      setBaixando(null);
    }
  }

  return (
    <>
      <Topo titulo="Relatórios">
        <button type="button" className="btn btn-claro" disabled={!!baixando || !r} onClick={() => exportar('xlsx')}>
          <Icone nome="baixar" tamanho={18} />{baixando === 'xlsx' ? 'Gerando…' : 'Excel'}
        </button>
        <button type="button" className="btn btn-terra" disabled={!!baixando || !r} onClick={() => exportar('pdf')}>
          <Icone nome="pdf" tamanho={18} />{baixando === 'pdf' ? 'Gerando…' : 'PDF'}
        </button>
      </Topo>

      <div className="filtros">
        <div className="chips" role="group" aria-label="Período">
          {PRESETS.map(([k, rot]) => <button key={k} type="button" className="chip" aria-pressed={preset === k} onClick={() => escolher(k)}>{rot}</button>)}
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
          <label className="sr" htmlFor="rel-de">De</label>
          <input id="rel-de" type="date" className="entrada" style={{ width: 'auto', minHeight: 44 }} value={de} max={ate}
            onChange={(e) => { setPreset(''); setPeriodo([e.target.value, ate]); }} />
          <span className="forte">a</span>
          <label className="sr" htmlFor="rel-ate">Até</label>
          <input id="rel-ate" type="date" className="entrada" style={{ width: 'auto', minHeight: 44 }} value={ate} min={de}
            onChange={(e) => { setPreset(''); setPeriodo([de, e.target.value]); }} />
        </div>
      </div>

      <Estado carregando={rel.carregando && !r} erro={rel.erro}>
        {r && (
          <>
            <section className="grade-resumo" aria-label="Resumo do período">
              <Indicador rotulo="Entradas" c={r.entradas} fundo="var(--oliva-claro)" bomSubir />
              <Indicador rotulo="Saídas" c={r.saidas} fundo="var(--terra-claro)" />
              <Indicador rotulo="Resultado" c={r.resultado} fundo="var(--ocre-claro)" bomSubir />
            </section>
            <p className="mudo forte" style={{ marginTop: -8, fontSize: 14 }}>
              Comparado com {data(r.deAnterior)} a {data(r.ateAnterior)}.
              {' '}{r.encomendasEntregues} {r.encomendasEntregues === 1 ? 'encomenda entregue' : 'encomendas entregues'}
              {r.encomendasEntregues > 0 && ` (valor médio ${moeda(r.ticketMedio)})`}.
            </p>

            <section className="cartao" aria-labelledby="t-evo">
              <div className="cartao-titulo"><h2 id="t-evo">Entradas e saídas nos últimos 12 meses</h2></div>
              <GraficoMeses meses={r.evolucao} />
            </section>

            <div className="grade-2">
              <section className="cartao" aria-labelledby="t-frente-rel">
                <div className="cartao-titulo"><h2 id="t-frente-rel">Entradas por frente</h2></div>
                {r.porFrente.length === 0 ? <Vazio>Nenhuma entrada no período.</Vazio> : <BarrasHorizontais itens={r.porFrente} />}
              </section>
              <section className="cartao" aria-labelledby="t-top">
                <div className="cartao-titulo"><h2 id="t-top">Mais vendidos</h2></div>
                {r.maisVendidos.length === 0 ? <Vazio>Nenhuma peça vendida ou entregue no período.</Vazio> : (
                  <ol className="lista-linhas ranking">
                    {r.maisVendidos.map((m, i) => (
                      <li key={m.nome}>
                        <span className="posicao" aria-hidden="true">{i + 1}</span>
                        <div className="info">
                          <p>{m.nome}</p>
                          <p>{[m.emEncomendas && `${m.emEncomendas} em encomendas`, m.emVendas && `${m.emVendas} na pronta-entrega`].filter(Boolean).join(' · ')}</p>
                        </div>
                        <div style={{ textAlign: 'right' }}>
                          <strong>{m.pecas} {m.pecas === 1 ? 'peça' : 'peças'}</strong>
                          <p className="mudo" style={{ fontSize: 13 }}>{moeda(m.faturamento)}</p>
                        </div>
                      </li>
                    ))}
                  </ol>
                )}
              </section>
            </div>

            <section className="cartao" aria-labelledby="t-peca">
              <div className="cartao-titulo"><h2 id="t-peca">Encomendas entregues: quanto rendeu cada peça</h2></div>
              {r.porPeca.length === 0 ? <Vazio>Nenhuma encomenda entregue no período.</Vazio> : (
                <div className="tabela-caixa">
                  <table className="tabela">
                    <thead><tr><th>Peça</th><th className="num">Peças</th><th className="num">Faturamento</th><th className="num">Materiais</th><th className="num">Seu ganho</th><th className="num">Por hora</th></tr></thead>
                    <tbody>
                      {r.porPeca.map((p) => {
                        const meta = Number(prec.dados?.valorHora || 0);
                        const abaixo = p.ganhoPorHora != null && meta > 0 && Number(p.ganhoPorHora) < meta;
                        return (
                          <tr key={p.nome}>
                            <td><strong>{p.nome}</strong></td>
                            <td className="num">{p.pecas}</td>
                            <td className="num">{moeda(p.faturamento)}</td>
                            <td className="num">{moeda(p.materiais)}</td>
                            <td className="num forte">{moeda(p.ganho)}</td>
                            <td className="num">
                              {p.ganhoPorHora == null ? '—' : <>{moeda(p.ganhoPorHora)}{abaixo && <span className="selo selo-terra" style={{ marginLeft: 6 }}>abaixo da meta</span>}</>}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              )}
              {prec.dados?.configurada && <p className="mudo" style={{ fontSize: 13, marginTop: 8 }}>Meta da sua hora: {moeda(prec.dados.valorHora)}. "Seu ganho" = valor cobrado − materiais − custos fixos − embalagem.</p>}
            </section>

            <div className="grade-2">
              <section className="cartao" aria-labelledby="t-pe">
                <div className="cartao-titulo"><h2 id="t-pe">Pronta-entrega</h2></div>
                {r.prontaEntrega.length === 0 ? <Vazio>Nenhuma venda no período.</Vazio> : (
                  <div className="tabela-caixa">
                    <table className="tabela" style={{ minWidth: 420 }}>
                      <thead><tr><th>Peça</th><th className="num">Peças</th><th className="num">Vendido</th><th className="num">Sobra</th></tr></thead>
                      <tbody>
                        {r.prontaEntrega.map((v) => (
                          <tr key={v.nome}><td><strong>{v.nome}</strong></td><td className="num">{v.pecas}</td><td className="num">{moeda(v.faturamento)}</td><td className="num forte">{moeda(v.sobra)}</td></tr>
                        ))}
                        {Number(r.descontosVendas) > 0 && <tr><td className="mudo">Descontos dados</td><td /><td className="num mudo">−{moeda(r.descontosVendas)}</td><td /></tr>}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>
              <section className="cartao" aria-labelledby="t-mat">
                <div className="cartao-titulo"><h2 id="t-mat">Materiais mais usados</h2></div>
                {r.materiais.length === 0 ? <Vazio>Nenhum material baixado no período.</Vazio> : (
                  <ul className="lista-linhas">
                    {r.materiais.map((m) => (
                      <li key={m.nome}>
                        <div className="info"><p>{m.nome}</p><p>{qtdUn(m.quantidade, m.unidade)}</p></div>
                        <strong className="num">{moeda(m.custo)}</strong>
                      </li>
                    ))}
                  </ul>
                )}
              </section>
            </div>
          </>
        )}
      </Estado>
    </>
  );
}

function Indicador({ rotulo, c, fundo, bomSubir = false }) {
  const v = c.variacaoPercentual;
  const melhorou = v != null && (bomSubir ? v > 0 : v < 0);
  return (
    <div className="resumo" style={{ background: fundo }}>
      <p className="rotulo" style={{ marginTop: 0 }}>{rotulo}</p>
      <p className="valor" style={{ fontSize: 28 }}>{moeda(c.atual)}</p>
      <p className="extra" style={{ color: v == null || v === 0 ? 'var(--marrom-2)' : melhorou ? 'var(--oliva-texto)' : 'var(--perigo)' }}>
        {v == null ? `antes: ${moeda(c.anterior)}` : `${v > 0 ? '▲ +' : v < 0 ? '▼ ' : ''}${v}% · antes ${moeda(c.anterior)}`}
      </p>
    </div>
  );
}

/** Barras agrupadas por mês, com legenda, valores ao tocar e tabela embaixo. */
function GraficoMeses({ meses }) {
  const [ativo, setAtivo] = useState(null);
  const [tabela, setTabela] = useState(false);
  const max = Math.max(1, ...meses.flatMap((m) => [Number(m.entradas), Number(m.saidas)]));
  const passo = max > 5000 ? 2000 : max > 2000 ? 1000 : max > 500 ? 250 : 100;
  const topo = Math.ceil(max / passo) * passo;
  const vazio = meses.every((m) => Number(m.entradas) === 0 && Number(m.saidas) === 0);
  const sel = ativo != null ? meses[ativo] : null;

  if (vazio) return <Vazio>Sem movimento nos últimos 12 meses.</Vazio>;

  return (
    <div>
      <div className="legenda-linha" aria-hidden="true">
        <span><i style={{ background: COR_ENTRADA }} />Entradas</span>
        <span><i style={{ background: COR_SAIDA }} />Saídas</span>
        <button type="button" className="btn btn-fantasma btn-p" style={{ marginLeft: 'auto' }} onClick={() => setTabela((t) => !t)} aria-expanded={tabela}>
          {tabela ? 'Esconder tabela' : 'Ver tabela'}
        </button>
      </div>
      <div className="grafico-meses" onMouseLeave={() => setAtivo(null)}>
        <div className="eixo-y" aria-hidden="true">
          {[topo, topo / 2, 0].map((v) => <span key={v}>{moedaCurta(v)}</span>)}
        </div>
        <div className="area">
          <div className="grade-linhas" aria-hidden="true"><i /><i /><i /></div>
          {meses.map((m, i) => (
            <button key={m.mes} type="button" className={`mes ${ativo === i ? 'ativo' : ''}`}
              onMouseEnter={() => setAtivo(i)} onFocus={() => setAtivo(i)} onBlur={() => setAtivo(null)} onClick={() => setAtivo(i)}
              aria-label={`${m.mes}: entradas ${moeda(m.entradas)}, saídas ${moeda(m.saidas)}`}>
              <span className="par">
                <span className="barra-v" style={{ height: `${(Number(m.entradas) / topo) * 100}%`, background: COR_ENTRADA }} />
                <span className="barra-v" style={{ height: `${(Number(m.saidas) / topo) * 100}%`, background: COR_SAIDA }} />
              </span>
              <span className="rotulo-mes">{m.mes.split('/')[0]}</span>
            </button>
          ))}
          {sel && (
            <div className="tooltip-grafico" role="status" style={ativo >= meses.length - 2
              ? { right: 0, transform: 'none' } : ativo <= 1 ? { left: 0, transform: 'none' } : { left: `${((ativo + 0.5) / meses.length) * 100}%` }}>
              <strong>{sel.mes}</strong>
              <span><i style={{ background: COR_ENTRADA }} />Entradas {moeda(sel.entradas)}</span>
              <span><i style={{ background: COR_SAIDA }} />Saídas {moeda(sel.saidas)}</span>
              <span className="forte">Resultado {moeda(Number(sel.entradas) - Number(sel.saidas))}</span>
            </div>
          )}
        </div>
      </div>
      {tabela && (
        <div className="tabela-caixa" style={{ marginTop: 12 }}>
          <table className="tabela" style={{ minWidth: 420 }}>
            <thead><tr><th>Mês</th><th className="num">Entradas</th><th className="num">Saídas</th><th className="num">Resultado</th></tr></thead>
            <tbody>
              {meses.map((m) => (
                <tr key={m.mes}><td>{m.mes}</td><td className="num">{moeda(m.entradas)}</td><td className="num">{moeda(m.saidas)}</td>
                  <td className="num forte">{moeda(Number(m.entradas) - Number(m.saidas))}</td></tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

function BarrasHorizontais({ itens }) {
  const max = Math.max(...itens.map((i) => Number(i.valor)));
  return (
    <ul className="barras-h">
      {itens.map((i) => (
        <li key={i.nome}>
          <div className="barras-h-rotulo"><span>{i.nome}</span><span className="forte">{moeda(i.valor)} <span className="mudo">· {i.percentual}%</span></span></div>
          <div className="barras-h-trilho"><div style={{ width: `${(Number(i.valor) / max) * 100}%` }} /></div>
        </li>
      ))}
    </ul>
  );
}
