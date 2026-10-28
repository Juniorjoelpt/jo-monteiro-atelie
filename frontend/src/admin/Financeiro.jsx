import { useState } from 'react';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Campo, Confirmar, Estado, ModalForm, lerForm, useCarregar, useToast, Vazio } from '../componentes/ui.jsx';
import { CATEGORIAS_LANC, CORES_GRAFICO, FORMAS, data, hojeIso, moeda } from '../util.js';
import { Abas, Topo } from './Layout.jsx';
import { ABAS_FINANCEIRO } from './ContasFixas.jsx';

const MESES = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'];

export default function Financeiro() {
  const hoje = new Date();
  const [ref, setRef] = useState({ ano: hoje.getFullYear(), mes: hoje.getMonth() + 1 });
  const [form, setForm] = useState(null);
  const [excluir, setExcluir] = useState(null);
  const toast = useToast();
  const { dados: r, erro, carregando, recarregar } = useCarregar(
    () => api.get(`/api/financeiro?ano=${ref.ano}&mes=${ref.mes}`), [ref.ano, ref.mes]);

  function mover(delta) {
    const d = new Date(ref.ano, ref.mes - 1 + delta, 1);
    setRef({ ano: d.getFullYear(), mes: d.getMonth() + 1 });
  }

  return (
    <>
      <Topo titulo="Financeiro">
        <div className="chips" style={{ alignItems: 'center' }}>
          <button type="button" className="chip" onClick={() => mover(-1)} aria-label="Mês anterior"><Icone nome="seta" tamanho={18} style={{ transform: 'rotate(180deg)' }} /></button>
          <span className="forte" style={{ padding: '0 8px', minWidth: 150, textAlign: 'center' }} aria-live="polite">{MESES[ref.mes - 1]} {ref.ano}</span>
          <button type="button" className="chip" onClick={() => mover(1)} aria-label="Próximo mês"><Icone nome="seta" tamanho={18} /></button>
        </div>
        <button type="button" className="btn btn-claro" onClick={() => setForm({ tipo: 'SAIDA' })}>Nova despesa</button>
        <button type="button" className="btn btn-terra" onClick={() => setForm({ tipo: 'ENTRADA' })}><Icone nome="mais" tamanho={18} largura={3} />Nova entrada</button>
      </Topo>
      <Abas itens={ABAS_FINANCEIRO} />

      <Estado carregando={carregando} erro={erro}>
        {r && (
          <>
            <section className="grade-resumo" aria-label="Resumo do mês">
              {[['Entradas', r.entradas, 'var(--oliva-claro)', 'var(--oliva-texto)'], ['Despesas', r.saidas, 'var(--terra-claro)', 'var(--terra-texto)'],
                ['Saldo do mês', r.saldo, 'var(--areia)', 'var(--marrom)'], ['A receber (total)', r.aReceber, 'var(--ocre-claro)', 'var(--ocre-texto)']].map(([rot, v, f, c]) => (
                <div key={rot} className="resumo" style={{ background: f }}>
                  <p className="rotulo" style={{ marginTop: 0 }}>{rot}</p>
                  <p className="valor" style={{ color: c, fontSize: 28 }}>{moeda(v)}</p>
                </div>
              ))}
            </section>

            <div className="grade-2">
              <section className="cartao" aria-labelledby="t-cat">
                <div className="cartao-titulo"><h2 id="t-cat">Entradas por frente</h2></div>
                {r.entradasPorCategoria.length === 0 ? <Vazio>Nenhuma entrada neste mês.</Vazio> : (
                  <ul className="legenda">
                    {r.entradasPorCategoria.map((c, i) => (
                      <li key={c.nome} style={{ flexDirection: 'column', alignItems: 'stretch', gap: 6 }}>
                        <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
                          <span className="bolinha" style={{ background: CORES_GRAFICO[i % CORES_GRAFICO.length] }} />
                          <span style={{ flex: 1 }}>{c.nome}</span><span>{moeda(c.valor)}</span><span className="mudo" style={{ width: 44, textAlign: 'right' }}>{c.percentual}%</span>
                        </div>
                        <div className="progresso" style={{ height: 12 }}><div style={{ width: `${c.percentual}%`, background: CORES_GRAFICO[i % CORES_GRAFICO.length] }} /></div>
                      </li>
                    ))}
                  </ul>
                )}
              </section>

              <section className="cartao" aria-labelledby="t-rec">
                <div className="cartao-titulo"><h2 id="t-rec">Contas a receber</h2></div>
                {r.contasAReceber.length === 0 ? <Vazio>Nenhum saldo em aberto.</Vazio> : (
                  <ul className="lista-linhas">
                    {r.contasAReceber.map((e) => (
                      <li key={e.id}>
                        <div className="info"><p>{e.clienteNome}</p><p>{e.descricao} · entrega {data(e.prazoEntrega)}</p></div>
                        <strong className="num" style={{ color: 'var(--terra-texto)' }}>{moeda(e.saldo)}</strong>
                      </li>
                    ))}
                  </ul>
                )}
              </section>
            </div>

            <section className="cartao" aria-labelledby="t-lanc">
              <div className="cartao-titulo"><h2 id="t-lanc">Lançamentos de {MESES[ref.mes - 1].toLowerCase()}</h2></div>
              {r.lancamentos.length === 0 ? <Vazio>Nenhum lançamento neste mês.</Vazio> : (
                <div className="tabela-caixa">
                  <table className="tabela">
                    <thead><tr><th>Data</th><th>Descrição</th><th>Categoria</th><th>Forma</th><th className="num">Valor</th><th><span className="sr">Ações</span></th></tr></thead>
                    <tbody>
                      {r.lancamentos.map((l) => (
                        <tr key={l.id}>
                          <td>{data(l.data)}</td>
                          <td>{l.descricao}</td>
                          <td><span className="selo selo-areia">{CATEGORIAS_LANC[l.categoria]}</span></td>
                          <td>{FORMAS[l.formaPagamento]}</td>
                          <td className="num" style={{ fontWeight: 800, color: l.tipo === 'ENTRADA' ? 'var(--oliva-texto)' : 'var(--perigo)' }}>
                            {l.tipo === 'ENTRADA' ? '+' : '−'} {moeda(l.valor)}
                          </td>
                          <td style={{ whiteSpace: 'nowrap', textAlign: 'right' }}>
                            {!l.automatico && !l.encomendaId && <>
                              <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label="Editar" onClick={() => setForm(l)}><Icone nome="editar" tamanho={18} /></button>
                              <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label="Excluir" onClick={() => setExcluir(l)}><Icone nome="lixo" tamanho={18} /></button>
                            </>}
                            {l.encomendaId && <span className="mudo" style={{ fontSize: 13 }}>encomenda</span>}
                            {l.automatico && <span className="mudo" style={{ fontSize: 13 }}>{l.vendaId ? 'venda' : l.compraId ? 'compra' : l.contaFixaId ? 'conta fixa' : ''}</span>}
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

      {form && (
        <ModalForm titulo={form.id ? 'Editar lançamento' : form.tipo === 'ENTRADA' ? 'Nova entrada' : 'Nova despesa'}
          aoFechar={() => setForm(null)} aoSalvar={async (fd) => {
            const d = lerForm(fd, ['valor']);
            d.tipo = form.tipo;
            if (form.id) await api.put(`/api/lancamentos/${form.id}`, d);
            else await api.post('/api/lancamentos', d);
            toast('Lançamento salvo.');
            setForm(null);
            recarregar();
          }}>
          <Campo rotulo="Descrição" nome="descricao" defaultValue={form.descricao} required maxLength={200} inteiro
            placeholder={form.tipo === 'ENTRADA' ? 'Venda na feira, peça avulsa…' : 'Embalagens, frete, taxa…'} />
          <Campo rotulo="Valor (R$)" nome="valor" tipo="number" min="0.01" step="0.01" defaultValue={form.valor} required />
          <Campo rotulo="Data" nome="data" tipo="date" defaultValue={form.data || hojeIso()} required />
          <Campo rotulo="Categoria" nome="categoria" opcoes={CATEGORIAS_LANC} defaultValue={form.categoria || (form.tipo === 'ENTRADA' ? 'PRESENTE' : 'MATERIAL')} />
          <Campo rotulo="Forma" nome="formaPagamento" opcoes={FORMAS} defaultValue={form.formaPagamento || 'PIX'} />
          {form.tipo === 'SAIDA' && <p className="inteiro mudo" style={{ fontSize: 14 }}>Contas do mês (aluguel, energia, MEI) ficam em Contas fixas, e compras de material em Materiais › Compras: assim entram no custo das peças.</p>}
          {form.tipo === 'ENTRADA' && <p className="inteiro mudo" style={{ fontSize: 14 }}>Pagamentos de encomendas são registrados na própria encomenda, vendas de peças prontas em Vendas, e as inscrições de oficina entram sozinhas ao confirmar o Pix.</p>}
        </ModalForm>
      )}
      {excluir && <Confirmar titulo="Excluir lançamento" perigo textoBotao="Excluir" texto={`Excluir "${excluir.descricao}" (${moeda(excluir.valor)})?`}
        aoFechar={() => setExcluir(null)} aoConfirmar={async () => { await api.del(`/api/lancamentos/${excluir.id}`); toast('Lançamento excluído.'); setExcluir(null); recarregar(); }} />}
    </>
  );
}
