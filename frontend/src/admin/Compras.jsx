import { useMemo, useState } from 'react';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Campo, Confirmar, Estado, Modal, ModalForm, lerForm, useCarregar, useToast, Vazio } from '../componentes/ui.jsx';
import { CORES_GRAFICO, FORMAS, data, hojeIso, linkWhats, moeda, moedaUnit, qtdUn, telefone } from '../util.js';
import { Abas, Topo } from './Layout.jsx';

const MESES = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'];

export const ABAS_MATERIAIS = [['/admin/materiais', 'Estoque', 'material'], ['/admin/materiais/compras', 'Compras e fornecedores', 'carrinho']];

const num = (v) => Number(String(v ?? '').replace(',', '.')) || 0;

export default function Compras() {
  const hoje = new Date();
  const [ref, setRef] = useState({ ano: hoje.getFullYear(), mes: hoje.getMonth() + 1 });
  const [nova, setNova] = useState(false);
  const [ver, setVer] = useState(null);
  const [cancelar, setCancelar] = useState(null);
  const [fornForm, setFornForm] = useState(null);
  const [fornExcluir, setFornExcluir] = useState(null);
  const toast = useToast();
  const compras = useCarregar(() => api.get(`/api/compras?ano=${ref.ano}&mes=${ref.mes}`), [ref.ano, ref.mes]);
  const fornecedores = useCarregar(() => api.get('/api/fornecedores'));
  const r = compras.dados;

  function mover(delta) {
    const d = new Date(ref.ano, ref.mes - 1 + delta, 1);
    setRef({ ano: d.getFullYear(), mes: d.getMonth() + 1 });
  }

  function recarregarTudo() {
    compras.recarregar();
    fornecedores.recarregar();
  }

  return (
    <>
      <Topo titulo="Compras">
        <div className="chips" style={{ alignItems: 'center' }}>
          <button type="button" className="chip" onClick={() => mover(-1)} aria-label="Mês anterior"><Icone nome="seta" tamanho={18} style={{ transform: 'rotate(180deg)' }} /></button>
          <span className="forte" style={{ padding: '0 8px', minWidth: 150, textAlign: 'center' }} aria-live="polite">{MESES[ref.mes - 1]} {ref.ano}</span>
          <button type="button" className="chip" onClick={() => mover(1)} aria-label="Próximo mês"><Icone nome="seta" tamanho={18} /></button>
        </div>
        <button type="button" className="btn btn-terra" onClick={() => setNova(true)}><Icone nome="mais" tamanho={18} largura={3} />Nova compra</button>
      </Topo>
      <Abas itens={ABAS_MATERIAIS} />

      <Estado carregando={compras.carregando} erro={compras.erro}>
        {r && (
          <div className="grade-2">
            <section className="cartao" aria-labelledby="t-mes">
              <div className="cartao-titulo"><h2 id="t-mes">Gasto com material</h2><span className="selo selo-areia">{MESES[ref.mes - 1]}</span></div>
              <p style={{ fontSize: 34, fontWeight: 900, color: 'var(--terra-texto)' }}>{moeda(r.total)}</p>
              <p className="mudo forte">{r.quantidade} {r.quantidade === 1 ? 'compra' : 'compras'} no mês</p>
              {r.porFornecedor.length > 0 && (
                <ul className="legenda" style={{ marginTop: 16 }}>
                  {r.porFornecedor.map((c, i) => (
                    <li key={c.nome} style={{ flexDirection: 'column', alignItems: 'stretch', gap: 6 }}>
                      <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
                        <span className="bolinha" style={{ background: CORES_GRAFICO[i % CORES_GRAFICO.length] }} />
                        <span style={{ flex: 1, minWidth: 0 }}>{c.nome}</span><span>{moeda(c.valor)}</span>
                        <span className="mudo" style={{ width: 44, textAlign: 'right' }}>{c.percentual}%</span>
                      </div>
                      <div className="progresso" style={{ height: 12 }}><div style={{ width: `${c.percentual}%`, background: CORES_GRAFICO[i % CORES_GRAFICO.length] }} /></div>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section className="cartao" aria-labelledby="t-compras">
              <div className="cartao-titulo"><h2 id="t-compras">Compras de {MESES[ref.mes - 1].toLowerCase()}</h2></div>
              {r.compras.length === 0 ? <Vazio>Nenhuma compra neste mês. Registre aqui o que comprou para o custo das peças ficar certo.</Vazio> : (
                <ul className="lista-linhas">
                  {r.compras.map((c) => (
                    <li key={c.id} style={{ opacity: c.status === 'CANCELADA' ? 0.55 : 1 }}>
                      <div className="icone-caixa" style={{ background: 'var(--ocre-claro)', width: 46, height: 46 }}><Icone nome="carrinho" cor="var(--ocre-texto)" /></div>
                      <div className="info">
                        <p>{c.fornecedorNome || 'Sem fornecedor'} {c.status === 'CANCELADA' && <span className="selo selo-perigo" style={{ marginLeft: 6 }}>cancelada</span>}</p>
                        <p>{data(c.data)} · {c.itens.map((i) => i.materialNome).join(', ')}</p>
                      </div>
                      <strong className="num">{moeda(c.total)}</strong>
                      <button type="button" className="btn btn-claro btn-p" onClick={() => setVer(c)}>Ver</button>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>
        )}
      </Estado>

      <section className="cartao" aria-labelledby="t-forn">
        <div className="cartao-titulo">
          <h2 id="t-forn">Fornecedores</h2>
          <button type="button" className="btn btn-claro btn-p" onClick={() => setFornForm({})}><Icone nome="mais" tamanho={16} largura={3} />Novo fornecedor</button>
        </div>
        <Estado carregando={fornecedores.carregando} erro={fornecedores.erro}>
          {fornecedores.dados?.length === 0 ? <Vazio>Cadastre onde você compra papéis, canecas, tinta e embalagens para comparar preços.</Vazio> : (
            <div className="tabela-caixa">
              <table className="tabela">
                <thead><tr><th>Fornecedor</th><th>Contato</th><th className="num">Compras</th><th className="num">Total comprado</th><th>Última</th><th><span className="sr">Ações</span></th></tr></thead>
                <tbody>
                  {fornecedores.dados?.map((f) => (
                    <tr key={f.id} style={{ opacity: f.ativo ? 1 : 0.55 }}>
                      <td><strong>{f.nome}</strong>{!f.ativo && <span className="selo selo-areia" style={{ marginLeft: 8 }}>inativo</span>}
                        {f.cidade && <div className="mudo" style={{ fontSize: 13 }}>{f.cidade}</div>}</td>
                      <td>
                        {f.whatsapp && <a href={linkWhats(f.whatsapp)} target="_blank" rel="noopener noreferrer" style={{ fontWeight: 800 }}>{telefone(f.whatsapp)}</a>}
                        {f.contato && <div className="mudo" style={{ fontSize: 13 }}>{f.contato}</div>}
                        {!f.whatsapp && !f.contato && '—'}
                      </td>
                      <td className="num">{f.compras}</td>
                      <td className="num">{moeda(f.totalComprado)}</td>
                      <td>{data(f.ultimaCompra)}</td>
                      <td style={{ whiteSpace: 'nowrap', textAlign: 'right' }}>
                        <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Editar ${f.nome}`} onClick={() => setFornForm(f)}><Icone nome="editar" tamanho={18} /></button>
                        <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Excluir ${f.nome}`} onClick={() => setFornExcluir(f)}><Icone nome="lixo" tamanho={18} /></button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Estado>
      </section>

      {nova && <NovaCompra fornecedores={(fornecedores.dados || []).filter((f) => f.ativo)} aoFechar={() => setNova(false)}
        aoSalvar={(c) => { setNova(false); toast(`Compra de ${moeda(c.total)} registrada. Estoque e custos atualizados.`); recarregarTudo(); }} />}

      {ver && (
        <Modal titulo={`Compra de ${data(ver.data)}`} aoFechar={() => setVer(null)} largo rodape={<>
          {ver.status === 'CONCLUIDA' && <button type="button" className="btn btn-perigo" onClick={() => { setCancelar(ver); setVer(null); }}>Cancelar compra</button>}
          <button type="button" className="btn btn-terra" onClick={() => setVer(null)}>Fechar</button>
        </>}>
          <p className="forte">{ver.fornecedorNome || 'Sem fornecedor'} · {FORMAS[ver.formaPagamento]}{ver.despesaLancada ? ' · lançada no financeiro' : ''}</p>
          {ver.observacoes && <p className="mudo" style={{ marginTop: 4 }}>{ver.observacoes}</p>}
          <div className="tabela-caixa" style={{ marginTop: 12 }}>
            <table className="tabela">
              <thead><tr><th>Material</th><th className="num">Quantidade</th><th className="num">Pago</th><th className="num">Custo por unidade</th></tr></thead>
              <tbody>
                {ver.itens.map((i) => (
                  <tr key={i.materialId}><td>{i.materialNome}</td><td className="num">{qtdUn(i.quantidade, i.unidade)}</td>
                    <td className="num">{moeda(i.valor)}</td><td className="num">{moedaUnit(i.custoUnitario)}/{i.unidade}</td></tr>
                ))}
                {Number(ver.frete) > 0 && <tr><td>Frete</td><td /><td className="num">{moeda(ver.frete)}</td><td className="num mudo" style={{ fontSize: 13 }}>dividido nos itens</td></tr>}
                <tr><td><strong>Total</strong></td><td /><td className="num"><strong>{moeda(ver.total)}</strong></td><td /></tr>
              </tbody>
            </table>
          </div>
        </Modal>
      )}

      {cancelar && <Confirmar titulo="Cancelar compra" perigo textoBotao="Cancelar compra"
        texto={`Os materiais desta compra saem do estoque, a despesa de ${moeda(cancelar.total)} some do financeiro e o custo volta ao da compra anterior. Continuar?`}
        aoFechar={() => setCancelar(null)} aoConfirmar={async () => { await api.post(`/api/compras/${cancelar.id}/cancelar`); toast('Compra cancelada.'); setCancelar(null); recarregarTudo(); }} />}

      {fornForm && (
        <ModalForm titulo={fornForm.id ? 'Editar fornecedor' : 'Novo fornecedor'} aoFechar={() => setFornForm(null)} aoSalvar={async (fd) => {
          const d = lerForm(fd);
          d.ativo = fd.get('ativo') === 'on';
          if (fornForm.id) await api.put(`/api/fornecedores/${fornForm.id}`, d);
          else await api.post('/api/fornecedores', d);
          toast('Fornecedor salvo.');
          setFornForm(null);
          fornecedores.recarregar();
        }}>
          <Campo rotulo="Nome" nome="nome" defaultValue={fornForm.nome} required maxLength={140} inteiro placeholder="Armarinho, loja online, madeireira…" />
          <Campo rotulo="WhatsApp" nome="whatsapp" tipo="tel" defaultValue={fornForm.whatsapp ? telefone(fornForm.whatsapp) : ''} maxLength={20} placeholder="(98) 98888-7777" />
          <Campo rotulo="Cidade" nome="cidade" defaultValue={fornForm.cidade} maxLength={120} />
          <Campo rotulo="Site, Instagram ou endereço" nome="contato" defaultValue={fornForm.contato} maxLength={160} inteiro />
          <Campo rotulo="Observações" nome="observacoes" defaultValue={fornForm.observacoes} linhas={3} maxLength={4000} inteiro placeholder="Prazo de entrega, pedido mínimo, frete grátis acima de…" />
          <label className="checkbox inteiro"><input type="checkbox" name="ativo" defaultChecked={fornForm.ativo ?? true} /> Fornecedor ativo</label>
        </ModalForm>
      )}

      {fornExcluir && <Confirmar titulo="Excluir fornecedor" perigo textoBotao="Excluir" texto={`Excluir ${fornExcluir.nome}? Se já houver compras, desative em vez de excluir.`}
        aoFechar={() => setFornExcluir(null)} aoConfirmar={async () => { await api.del(`/api/fornecedores/${fornExcluir.id}`); toast('Fornecedor excluído.'); setFornExcluir(null); fornecedores.recarregar(); }} />}
    </>
  );
}

/** Compra com vários materiais. Mostra o custo por unidade (com o frete dividido) e compara com o atual. */
export function NovaCompra({ fornecedores, aoFechar, aoSalvar, materialInicial }) {
  const materiais = useCarregar(() => api.get('/api/materiais'));
  const [itens, setItens] = useState([{ materialId: materialInicial ? String(materialInicial) : '', quantidade: '', valor: '' }]);
  const [frete, setFrete] = useState('');
  const porId = useMemo(() => Object.fromEntries((materiais.dados || []).map((m) => [String(m.id), m])), [materiais.dados]);
  const subtotal = itens.reduce((s, i) => s + num(i.valor), 0);
  const total = subtotal + num(frete);

  function mudar(k, campo, valor) {
    setItens((l) => l.map((i, n) => (n === k ? { ...i, [campo]: valor } : i)));
  }

  return (
    <ModalForm titulo="Nova compra" largo textoSalvar="Registrar compra" aoFechar={aoFechar} aoSalvar={async (fd) => {
      const d = lerForm(fd, ['fornecedorId', 'frete']);
      const lista = itens.filter((i) => i.materialId || i.quantidade || i.valor);
      if (lista.length === 0) throw new Error('Adicione pelo menos um material.');
      if (lista.some((i) => !i.materialId || num(i.quantidade) <= 0 || num(i.valor) <= 0)) {
        throw new Error('Em cada linha, escolha o material e informe a quantidade e o valor pago.');
      }
      const c = await api.post('/api/compras', {
        fornecedorId: d.fornecedorId, data: d.data, frete: d.frete, formaPagamento: d.formaPagamento, observacoes: d.observacoes,
        registrarDespesa: fd.get('registrarDespesa') === 'on',
        itens: lista.map((i) => ({ materialId: Number(i.materialId), quantidade: num(i.quantidade), valor: num(i.valor) })),
      });
      aoSalvar(c);
    }}>
      <Campo rotulo="Fornecedor" nome="fornecedorId" opcoes={[['', 'Sem fornecedor'], ...fornecedores.map((f) => [String(f.id), f.nome])]} />
      <Campo rotulo="Data" nome="data" tipo="date" defaultValue={hojeIso()} required />

      <fieldset className="inteiro" style={{ border: 0, padding: 0, margin: 0, minWidth: 0 }}>
        <legend className="forte" style={{ marginBottom: 8 }}>Materiais</legend>
        {materiais.erro && <div className="aviso aviso-erro">{materiais.erro}</div>}
        <div className="itens-compra">
          {itens.map((i, k) => {
            const m = porId[i.materialId];
            const parteFrete = subtotal > 0 ? (num(frete) * num(i.valor)) / subtotal : 0;
            const custo = num(i.quantidade) > 0 && num(i.valor) > 0 ? (num(i.valor) + parteFrete) / num(i.quantidade) : null;
            const atual = m ? Number(m.custoUnitario) : 0;
            const variacao = custo != null && atual > 0 ? Math.round(((custo - atual) / atual) * 100) : null;
            return (
              <div key={k} className="item-compra">
                <Campo rotulo={`Material ${itens.length > 1 ? k + 1 : ''}`} nome={`m${k}`} value={i.materialId} onChange={(e) => mudar(k, 'materialId', e.target.value)}
                  opcoes={[['', 'Escolha…'], ...(materiais.dados || []).map((mm) => [String(mm.id), `${mm.nome} (${mm.unidade})`])]} />
                <Campo rotulo={m ? `Quantidade (${m.unidade})` : 'Quantidade'} nome={`q${k}`} tipo="number" min="0" step="0.001" inputMode="decimal"
                  value={i.quantidade} onChange={(e) => mudar(k, 'quantidade', e.target.value)} />
                <Campo rotulo="Valor pago (R$)" nome={`v${k}`} tipo="number" min="0" step="0.01" inputMode="decimal"
                  value={i.valor} onChange={(e) => mudar(k, 'valor', e.target.value)} />
                <button type="button" className="btn btn-fantasma btn-icone" aria-label={`Tirar a linha ${k + 1}`} disabled={itens.length === 1}
                  onClick={() => setItens((l) => l.filter((_, n) => n !== k))} style={{ marginBottom: 4 }}><Icone nome="lixo" tamanho={18} /></button>
                {custo != null && m && (
                  <p className="custo">
                    Sai a {moedaUnit(custo)}/{m.unidade}
                    {variacao != null && variacao !== 0 && <> · <span style={{ color: variacao > 0 ? 'var(--perigo)' : 'var(--oliva-texto)' }}>
                      {variacao > 0 ? `${variacao}% mais caro` : `${-variacao}% mais barato`}</span> que o custo atual ({moedaUnit(atual)})</>}
                  </p>
                )}
              </div>
            );
          })}
        </div>
        <button type="button" className="btn btn-claro btn-p" style={{ marginTop: 10 }}
          onClick={() => setItens((l) => [...l, { materialId: '', quantidade: '', valor: '' }])}><Icone nome="mais" tamanho={16} largura={3} />Outro material</button>
      </fieldset>

      <Campo rotulo="Frete (R$)" nome="frete" tipo="number" min="0" step="0.01" value={frete} onChange={(e) => setFrete(e.target.value)}
        dica="É dividido entre os materiais, pelo valor de cada um." />
      <Campo rotulo="Forma de pagamento" nome="formaPagamento" opcoes={FORMAS} defaultValue="PIX" />
      <Campo rotulo="Observação" nome="observacoes" maxLength={255} inteiro placeholder="Nota fiscal, pedido nº…" />
      <label className="checkbox inteiro"><input type="checkbox" name="registrarDespesa" defaultChecked /> Lançar como despesa no financeiro</label>
      <div className="inteiro clay-sm" style={{ background: 'var(--oliva-claro)', borderRadius: 18, padding: '12px 16px', display: 'flex', justifyContent: 'space-between', fontWeight: 900 }}>
        <span>Total da compra</span><span>{moeda(total)}</span>
      </div>
    </ModalForm>
  );
}

/** Quanto ela pagou pelo material em cada compra e em cada fornecedor (o mais barato vem destacado). */
export function HistoricoPrecos({ material, aoFechar }) {
  const h = useCarregar(() => api.get(`/api/materiais/${material.id}/precos`), [material.id]);
  return (
    <Modal titulo={`Preços de ${material.nome}`} aoFechar={aoFechar} largo>
      <Estado carregando={h.carregando} erro={h.erro}>
        {h.dados && (h.dados.compras.length === 0 ? <Vazio>Ainda não há compras com valor deste material.</Vazio> : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
            <p className="forte">Custo atual: {moedaUnit(h.dados.custoAtual)}/{h.dados.unidade}</p>
            {h.dados.porFornecedor.length > 1 && (
              <div>
                <h3 style={{ fontSize: 17, marginBottom: 8 }}>Último preço em cada fornecedor</h3>
                <ul className="lista-linhas">
                  {h.dados.porFornecedor.map((p) => (
                    <li key={p.fornecedorNome} className={p.maisBarato ? 'mais-barato' : ''} style={{ borderRadius: p.maisBarato ? 14 : 0, padding: '10px 12px' }}>
                      <div className="info"><p>{p.fornecedorNome}</p><p>em {data(p.data)}</p></div>
                      {p.maisBarato && <span className="selo selo-oliva">mais barato</span>}
                      <strong className="num">{moedaUnit(p.ultimoCusto)}/{h.dados.unidade}</strong>
                    </li>
                  ))}
                </ul>
              </div>
            )}
            <div>
              <h3 style={{ fontSize: 17, marginBottom: 8 }}>Todas as compras</h3>
              <div className="tabela-caixa">
                <table className="tabela">
                  <thead><tr><th>Data</th><th>Fornecedor</th><th className="num">Quantidade</th><th className="num">Custo por unidade</th></tr></thead>
                  <tbody>
                    {h.dados.compras.map((c, k) => (
                      <tr key={`${c.compraId}-${k}`}><td>{data(c.data)}</td><td>{c.fornecedorNome}</td>
                        <td className="num">{qtdUn(c.quantidade, h.dados.unidade)}</td><td className="num">{moedaUnit(c.custoUnitario)}</td></tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        ))}
      </Estado>
    </Modal>
  );
}
