import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api, sessao } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Confirmar, Estado, Modal, useCarregar, useToast, Vazio } from '../componentes/ui.jsx';
import { CANAIS, CORES_GRAFICO, FORMAS, data, hojeIso, moeda } from '../util.js';
import { Topo } from './Layout.jsx';

const MESES = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'];

export default function Vendas() {
  return sessao.ajudante() ? <CaixaAjudante /> : <VendasAdmin />;
}

/** A ajudante usa só o caixa (sem o resumo do mês nem o cancelamento). */
function CaixaAjudante() {
  const [pdv, setPdv] = useState(true);
  const [ultimas, setUltimas] = useState([]);
  const toast = useToast();
  return (
    <>
      <Topo titulo="Vendas">
        <button type="button" className="btn btn-terra" onClick={() => setPdv(true)}><Icone nome="sacola" tamanho={18} />Nova venda</button>
      </Topo>
      <section className="cartao" aria-labelledby="t-hoje">
        <div className="cartao-titulo"><h2 id="t-hoje">Vendas que você registrou agora</h2></div>
        {ultimas.length === 0 ? <Vazio>Toque em "Nova venda" para abrir o caixa. Errou alguma venda? Avise a Jô para cancelar.</Vazio> : (
          <ul className="lista-linhas">
            {ultimas.map((v) => (
              <li key={v.id}><div className="info"><p>{v.itens.map((i) => `${i.quantidade}× ${i.descricao}`).join(', ')}</p><p>{CANAIS[v.canal]} · {FORMAS[v.formaPagamento]}</p></div>
                <strong className="num">{moeda(v.total)}</strong></li>
            ))}
          </ul>
        )}
      </section>
      {pdv && <Pdv aoFechar={() => setPdv(false)} aoVender={(v) => { toast(`Venda de ${moeda(v.total)} registrada.`); setPdv(false); setUltimas((l) => [v, ...l]); }} />}
    </>
  );
}

function VendasAdmin() {
  const [params, setParams] = useSearchParams();
  const hoje = new Date();
  const [ref, setRef] = useState({ ano: hoje.getFullYear(), mes: hoje.getMonth() + 1 });
  const [pdv, setPdv] = useState(!!params.get('nova'));
  const [cancelar, setCancelar] = useState(null);
  const toast = useToast();
  const { dados: r, erro, carregando, recarregar } = useCarregar(
    () => api.get(`/api/vendas?ano=${ref.ano}&mes=${ref.mes}`), [ref.ano, ref.mes]);

  function mover(delta) {
    const d = new Date(ref.ano, ref.mes - 1 + delta, 1);
    setRef({ ano: d.getFullYear(), mes: d.getMonth() + 1 });
  }

  function fecharPdv() {
    setPdv(false);
    if (params.get('nova')) setParams({});
  }

  return (
    <>
      <Topo titulo="Vendas">
        <div className="chips" style={{ alignItems: 'center' }}>
          <button type="button" className="chip" onClick={() => mover(-1)} aria-label="Mês anterior"><Icone nome="seta" tamanho={18} style={{ transform: 'rotate(180deg)' }} /></button>
          <span className="forte" style={{ padding: '0 8px', minWidth: 150, textAlign: 'center' }} aria-live="polite">{MESES[ref.mes - 1]} {ref.ano}</span>
          <button type="button" className="chip" onClick={() => mover(1)} aria-label="Próximo mês"><Icone nome="seta" tamanho={18} /></button>
        </div>
        <button type="button" className="btn btn-terra" onClick={() => setPdv(true)}><Icone nome="sacola" tamanho={18} />Nova venda</button>
      </Topo>
      <p className="mudo forte" style={{ marginTop: -10 }}>Peças de pronta-entrega vendidas no ateliê, em feiras e pelas redes. Cada venda tira as peças do estoque e entra no financeiro.</p>

      <Estado carregando={carregando} erro={erro}>
        {r && (
          <>
            <section className="grade-resumo" aria-label="Resumo do mês">
              {[['Vendido', moeda(r.totalVendido), 'var(--oliva-claro)'], ['Peças vendidas', r.pecas, 'var(--terra-claro)'],
                ['Custo dos materiais', moeda(r.custoDireto), 'var(--areia)'], ['Sobra das vendas', moeda(r.sobra), 'var(--ocre-claro)']].map(([rot, v, f]) => (
                <div key={rot} className="resumo" style={{ background: f }}>
                  <p className="rotulo" style={{ marginTop: 0 }}>{rot}</p>
                  <p className="valor" style={{ fontSize: 28 }}>{v}</p>
                </div>
              ))}
            </section>

            {r.porCanal.length > 0 && (
              <section className="cartao" aria-labelledby="t-canal">
                <div className="cartao-titulo"><h2 id="t-canal">Onde mais vendeu</h2></div>
                <ul className="legenda">
                  {r.porCanal.map((c, i) => (
                    <li key={c.nome} style={{ flexDirection: 'column', alignItems: 'stretch', gap: 6 }}>
                      <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
                        <span className="bolinha" style={{ background: CORES_GRAFICO[i % CORES_GRAFICO.length] }} />
                        <span style={{ flex: 1 }}>{c.nome}</span><span>{moeda(c.valor)}</span>
                        <span className="mudo" style={{ width: 44, textAlign: 'right' }}>{c.percentual}%</span>
                      </div>
                      <div className="progresso" style={{ height: 12 }}><div style={{ width: `${c.percentual}%`, background: CORES_GRAFICO[i % CORES_GRAFICO.length] }} /></div>
                    </li>
                  ))}
                </ul>
              </section>
            )}

            <section className="cartao" aria-labelledby="t-vendas">
              <div className="cartao-titulo"><h2 id="t-vendas">Vendas de {MESES[ref.mes - 1].toLowerCase()}</h2></div>
              {r.vendas.length === 0 ? <Vazio>Nenhuma venda neste mês.</Vazio> : (
                <div className="tabela-caixa">
                  <table className="tabela">
                    <thead><tr><th>Data</th><th>Peças</th><th>Onde</th><th>Forma</th><th className="num">Total</th><th><span className="sr">Ações</span></th></tr></thead>
                    <tbody>
                      {r.vendas.map((v) => (
                        <tr key={v.id} style={{ opacity: v.status === 'CANCELADA' ? 0.55 : 1 }}>
                          <td>{data(v.data)}</td>
                          <td>
                            {v.itens.map((i) => `${i.quantidade}× ${i.descricao}`).join(', ')}
                            {v.clienteNome && <><br /><span className="mudo" style={{ fontSize: 14 }}>{v.clienteNome}</span></>}
                          </td>
                          <td>{CANAIS[v.canal]}</td>
                          <td>{FORMAS[v.formaPagamento]}</td>
                          <td className="num forte">
                            {v.status === 'CANCELADA' ? <s>{moeda(v.total)}</s> : moeda(v.total)}
                            {Number(v.desconto) > 0 && <><br /><span className="mudo" style={{ fontSize: 13 }}>desc. {moeda(v.desconto)}</span></>}
                          </td>
                          <td style={{ textAlign: 'right' }}>
                            {v.status === 'CANCELADA'
                              ? <span className="selo selo-perigo">cancelada</span>
                              : <button type="button" className="btn btn-fantasma btn-p" onClick={() => setCancelar(v)}>Cancelar</button>}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>
          </>
        )}
      </Estado>

      {pdv && <Pdv aoFechar={fecharPdv} aoVender={(v) => { toast(`Venda de ${moeda(v.total)} registrada.`); fecharPdv(); recarregar(); }} />}
      {cancelar && <Confirmar titulo="Cancelar venda" perigo textoBotao="Cancelar venda"
        texto={`As peças voltam para o estoque e a entrada de ${moeda(cancelar.total)} sai do financeiro.`}
        aoFechar={() => setCancelar(null)}
        aoConfirmar={async () => { await api.post(`/api/vendas/${cancelar.id}/cancelar`); toast('Venda cancelada.'); setCancelar(null); recarregar(); }} />}
    </>
  );
}

function lembrado(chave, padrao) {
  try { return localStorage.getItem(chave) || padrao; } catch { return padrao; }
}
function lembrar(chave, valor) {
  try { localStorage.setItem(chave, valor); } catch { /* sem armazenamento: tudo bem */ }
}

/** Caixa rápido: toca na peça para somar, escolhe onde vendeu e a forma de pagamento. */
function Pdv({ aoFechar, aoVender }) {
  const { dados: produtos, erro: erroProd, carregando } = useCarregar(() => api.get('/api/produtos'));
  const [carrinho, setCarrinho] = useState([]);
  const [canal, setCanal] = useState(() => lembrado('atelie.canal', 'ATELIE'));
  const [forma, setForma] = useState(() => lembrado('atelie.forma', 'PIX'));
  const [desconto, setDesconto] = useState('');
  const [dataVenda, setDataVenda] = useState(hojeIso());
  const [avulso, setAvulso] = useState(null);
  const [erro, setErro] = useState(null);
  const [enviando, setEnviando] = useState(false);

  const vitrine = (produtos || []).filter((p) => p.ativo && p.prontaEntrega);
  const noCarrinho = (id) => carrinho.filter((c) => c.produtoId === id).reduce((s, c) => s + c.quantidade, 0);

  function somar(p) {
    if (noCarrinho(p.id) >= p.estoque) return;
    setCarrinho((c) => {
      const i = c.findIndex((x) => x.produtoId === p.id);
      if (i >= 0) return c.map((x, j) => (j === i ? { ...x, quantidade: x.quantidade + 1 } : x));
      return [...c, { chave: `p${p.id}`, produtoId: p.id, descricao: p.nome, preco: Number(p.custo.precoAtual), quantidade: 1, max: p.estoque }];
    });
  }
  function mudarQtd(chave, delta) {
    setCarrinho((c) => c.flatMap((x) => {
      if (x.chave !== chave) return [x];
      const q = x.quantidade + delta;
      if (q <= 0) return [];
      if (x.max != null && q > x.max) return [x];
      return [{ ...x, quantidade: q }];
    }));
  }

  const subtotal = carrinho.reduce((s, c) => s + c.preco * c.quantidade, 0);
  const desc = Number(String(desconto).replace(',', '.')) || 0;
  const total = Math.max(0, subtotal - desc);

  async function finalizar() {
    setErro(null);
    if (!carrinho.length) { setErro('Toque nas peças para adicionar à venda.'); return; }
    if (desc > subtotal) { setErro('O desconto é maior que o valor da venda.'); return; }
    setEnviando(true);
    lembrar('atelie.canal', canal);
    lembrar('atelie.forma', forma);
    try {
      const v = await api.post('/api/vendas', {
        data: dataVenda, canal, formaPagamento: forma, desconto: desc,
        itens: carrinho.map((c) => c.produtoId
          ? { produtoId: c.produtoId, quantidade: c.quantidade }
          : { descricao: c.descricao, quantidade: c.quantidade, precoUnitario: c.preco }),
      });
      aoVender(v);
    } catch (e) {
      setErro(e.message);
      setEnviando(false);
    }
  }

  return (
    <Modal titulo="Nova venda" aoFechar={aoFechar} largo>
      {erroProd && <div className="aviso aviso-erro" role="alert">{erroProd}</div>}
      <div className="pdv">
        <div style={{ flex: '999 1 340px', minWidth: 0, display: 'flex', flexDirection: 'column', gap: 12 }}>
          {carregando ? <div className="carregando">Carregando peças…</div> : vitrine.length === 0 ? (
            <div className="aviso aviso-info">
              Nenhuma peça de pronta-entrega ainda. Em <Link to="/admin/produtos">Produtos</Link>, marque "Vendo como pronta-entrega" e registre quantas estão prontas. Você também pode vender um item avulso.
            </div>
          ) : (
            <div className="pdv-pecas">
              {vitrine.map((p) => {
                const resta = p.estoque - noCarrinho(p.id);
                return (
                  <button key={p.id} type="button" className="peca-botao" disabled={resta <= 0} onClick={() => somar(p)}
                    aria-label={`Adicionar ${p.nome}, ${moeda(p.custo.precoAtual)}, ${resta} disponíveis`}>
                    <strong>{p.nome}</strong>
                    <span className="estoque">{p.estoque === 0 ? 'sem peças prontas' : `${resta} de ${p.estoque} disponíveis`}</span>
                    <span className="preco">{moeda(p.custo.precoAtual)}</span>
                  </button>
                );
              })}
            </div>
          )}
          {avulso ? (
            <form className="cartao" style={{ padding: 16, display: 'flex', flexWrap: 'wrap', gap: 10, alignItems: 'flex-end' }}
              onSubmit={(e) => {
                e.preventDefault();
                const preco = Number(String(avulso.preco).replace(',', '.'));
                if (!avulso.descricao.trim() || !(preco >= 0) || avulso.preco === '') return;
                setCarrinho((c) => [...c, { chave: `a${Date.now()}`, produtoId: null, descricao: avulso.descricao.trim(), preco, quantidade: 1, max: null }]);
                setAvulso(null);
              }}>
              <div className="campo" style={{ flex: '2 1 180px' }}>
                <label htmlFor="av-desc">Item avulso</label>
                <input id="av-desc" className="entrada" value={avulso.descricao} maxLength={160} autoFocus
                  onChange={(e) => setAvulso({ ...avulso, descricao: e.target.value })} placeholder="Marcador de página" />
              </div>
              <div className="campo" style={{ flex: '1 1 100px' }}>
                <label htmlFor="av-preco">Preço (R$)</label>
                <input id="av-preco" className="entrada" type="number" min="0" step="0.01" value={avulso.preco}
                  onChange={(e) => setAvulso({ ...avulso, preco: e.target.value })} />
              </div>
              <button type="submit" className="btn btn-oliva">Adicionar</button>
              <button type="button" className="btn btn-fantasma" onClick={() => setAvulso(null)}>Cancelar</button>
            </form>
          ) : (
            <button type="button" className="btn btn-claro" style={{ alignSelf: 'flex-start' }} onClick={() => setAvulso({ descricao: '', preco: '' })}>
              <Icone nome="mais" tamanho={16} largura={3} />Item avulso (sem cadastro)
            </button>
          )}
        </div>

        <section className="carrinho" aria-label="Itens da venda">
          {carrinho.length === 0 ? <p className="mudo forte">Toque nas peças para somar à venda.</p> : (
            carrinho.map((c) => (
              <div key={c.chave} className="carrinho-linha">
                <div className="nome">{c.descricao}<small>{moeda(c.preco)} cada</small></div>
                <div className="contador-qtd">
                  <button type="button" className="btn btn-claro btn-icone btn-p" aria-label={`Tirar um ${c.descricao}`} onClick={() => mudarQtd(c.chave, -1)}><Icone nome="menos" tamanho={16} largura={3} /></button>
                  <span aria-live="polite">{c.quantidade}</span>
                  <button type="button" className="btn btn-claro btn-icone btn-p" aria-label={`Somar um ${c.descricao}`} disabled={c.max != null && c.quantidade >= c.max} onClick={() => mudarQtd(c.chave, 1)}><Icone nome="mais" tamanho={16} largura={3} /></button>
                </div>
              </div>
            ))
          )}

          <div className="campo">
            <span className="rotulo">Onde vendeu</span>
            <div className="chips chips-p" role="group" aria-label="Onde vendeu">
              {Object.entries(CANAIS).map(([k, r]) => <button key={k} type="button" className="chip" aria-pressed={canal === k} onClick={() => setCanal(k)}>{r}</button>)}
            </div>
          </div>
          <div className="campo">
            <span className="rotulo">Pagamento</span>
            <div className="chips chips-p" role="group" aria-label="Forma de pagamento">
              {Object.entries(FORMAS).map(([k, r]) => <button key={k} type="button" className="chip" aria-pressed={forma === k} onClick={() => setForma(k)}>{r}</button>)}
            </div>
          </div>
          <div className="form-grade">
            <div className="campo">
              <label htmlFor="pdv-desc">Desconto (R$)</label>
              <input id="pdv-desc" className="entrada" type="number" min="0" step="0.01" value={desconto} onChange={(e) => setDesconto(e.target.value)} placeholder="0,00" />
            </div>
            <div className="campo">
              <label htmlFor="pdv-data">Data</label>
              <input id="pdv-data" className="entrada" type="date" max={hojeIso()} value={dataVenda} onChange={(e) => setDataVenda(e.target.value)} />
            </div>
          </div>
          {erro && <div className="aviso aviso-erro" role="alert">{erro}</div>}
          <div className="total-venda"><span>Total</span><strong>{moeda(total)}</strong></div>
          <button type="button" className="btn btn-terra" style={{ minHeight: 54, fontSize: 18 }} disabled={enviando || !carrinho.length} onClick={finalizar}>
            <Icone nome="check" tamanho={20} largura={3} />{enviando ? 'Registrando…' : 'Finalizar venda'}
          </button>
        </section>
      </div>
    </Modal>
  );
}
