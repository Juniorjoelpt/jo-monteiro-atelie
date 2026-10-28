import { useState } from 'react';
import { api, sessao } from '../api.js';
import { ABAS_MATERIAIS, HistoricoPrecos, NovaCompra } from './Compras.jsx';
import Icone from '../componentes/Icone.jsx';
import { Campo, Confirmar, Estado, ModalForm, lerForm, useCarregar, useToast, Vazio } from '../componentes/ui.jsx';
import { moeda, moedaUnit, qtdUn } from '../util.js';
import { Abas, Topo } from './Layout.jsx';

const qtd = (n) => Number(n).toLocaleString('pt-BR', { maximumFractionDigits: 2 });

export default function Materiais() {
  const [form, setForm] = useState(null);
  const [ajuste, setAjuste] = useState(null);
  const [excluir, setExcluir] = useState(null);
  const [precos, setPrecos] = useState(null);
  const [compraVarios, setCompraVarios] = useState(false);
  const ajudante = sessao.ajudante();
  const toast = useToast();
  const { dados, erro, carregando, recarregar } = useCarregar(() => api.get('/api/materiais'));
  const fornecedores = useCarregar(() => (ajudante ? Promise.resolve([]) : api.get('/api/fornecedores')));
  const ativos = (fornecedores.dados || []).filter((f) => f.ativo);
  const valorEstoque = (dados || []).reduce((s, m) => s + Number(m.quantidade) * Number(m.custoUnitario), 0);

  return (
    <>
      <Topo titulo="Materiais">
        {!ajudante && <>
          <button type="button" className="btn btn-claro" onClick={() => setCompraVarios(true)}><Icone nome="carrinho" tamanho={18} />Nova compra</button>
          <button type="button" className="btn btn-terra" onClick={() => setForm({})}><Icone nome="mais" tamanho={18} largura={3} />Novo material</button>
        </>}
      </Topo>
      {!ajudante && <Abas itens={ABAS_MATERIAIS} />}
      {ajudante && <p className="mudo forte" style={{ marginTop: -10 }}>Registre em "Usei" o material que saiu do estoque. Compras são lançadas pela Jô.</p>}
      <section className="cartao">
        <Estado carregando={carregando} erro={erro}>
          {dados?.length === 0 ? <Vazio>Cadastre papéis, canecas lisas, tintas e embalagens para receber aviso quando estiverem acabando.</Vazio> : (
            <>
              {!ajudante && <p className="mudo forte" style={{ marginBottom: 12 }}>Valor em estoque: {moeda(valorEstoque)}</p>}
              <div className="tabela-caixa">
                <table className="tabela">
                  <thead><tr><th>Material</th><th className="num">Em estoque</th><th className="num">Mínimo</th>{!ajudante && <th className="num">Custo</th>}<th><span className="sr">Ações</span></th></tr></thead>
                  <tbody>
                    {dados?.map((m) => (
                      <tr key={m.id}>
                        <td><strong>{m.nome}</strong>{m.abaixoDoMinimo && <span className="selo selo-perigo" style={{ marginLeft: 8 }}>{Number(m.quantidade) < 0 ? 'registre a compra' : 'acabando'}</span>}</td>
                        <td className={`num ${Number(m.quantidade) < 0 ? 'estoque-negativo' : ''}`} style={{ fontWeight: 800 }}>{qtdUn(m.quantidade, m.unidade)}</td>
                        <td className="num">{qtd(m.estoqueMinimo)}</td>
                        {!ajudante && <td className="num">{moedaUnit(m.custoUnitario)}/{m.unidade}</td>}
                        <td style={{ whiteSpace: 'nowrap', textAlign: 'right' }}>
                          <button type="button" className="btn btn-claro btn-p" onClick={() => setAjuste({ m, tipo: 'SAIDA' })}>Usei</button>{' '}
                          {!ajudante && <>
                            <button type="button" className="btn btn-oliva btn-p" onClick={() => setAjuste({ m, tipo: 'ENTRADA' })}>Comprei</button>{' '}
                            <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Preços pagos em ${m.nome}`} title="Preços pagos" onClick={() => setPrecos(m)}><Icone nome="historico" tamanho={18} /></button>
                            <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Editar ${m.nome}`} onClick={() => setForm(m)}><Icone nome="editar" tamanho={18} /></button>
                            <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Excluir ${m.nome}`} onClick={() => setExcluir(m)}><Icone nome="lixo" tamanho={18} /></button>
                          </>}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </Estado>
      </section>

      {form && (
        <ModalForm titulo={form.id ? 'Editar material' : 'Novo material'} aoFechar={() => setForm(null)} aoSalvar={async (fd) => {
          const d = lerForm(fd, ['quantidade', 'estoqueMinimo', 'custoUnitario']);
          if (form.id) await api.put(`/api/materiais/${form.id}`, d);
          else await api.post('/api/materiais', d);
          toast('Material salvo.');
          setForm(null);
          recarregar();
        }}>
          <Campo rotulo="Nome" nome="nome" defaultValue={form.nome} required maxLength={140} inteiro placeholder="Caneca branca de porcelana 325 ml" />
          <Campo rotulo="Unidade" nome="unidade" defaultValue={form.unidade || 'metro'} required maxLength={20} dica="Use a mesma unidade das fichas: metro, grama, unidade…" />
          <Campo rotulo="Quantidade em estoque" nome="quantidade" tipo="number" step="0.001" defaultValue={form.quantidade ?? 0} required />
          <Campo rotulo="Avisar quando chegar a" nome="estoqueMinimo" tipo="number" min="0" step="0.001" defaultValue={form.estoqueMinimo ?? 0} required />
          <Campo rotulo="Custo por unidade (R$)" nome="custoUnitario" tipo="number" min="0" step="0.0001" defaultValue={form.custoUnitario ?? 0} required
            dica="Ex.: rolo de 100 m por R$ 50 = R$ 0,50 por metro. Atualiza sozinho a cada compra." />
        </ModalForm>
      )}

      {ajuste && (
        <ModalForm titulo={ajuste.tipo === 'ENTRADA' ? `Comprei ${ajuste.m.nome}` : `Usei ${ajuste.m.nome}`} textoSalvar="Confirmar"
          aoFechar={() => setAjuste(null)} aoSalvar={async (fd) => {
            const d = lerForm(fd, ['quantidade', 'valorCompra', 'fornecedorId']);
            d.tipo = ajuste.tipo;
            d.registrarDespesa = fd.get('registrarDespesa') === 'on';
            await api.post(`/api/materiais/${ajuste.m.id}/ajuste`, d);
            toast(ajuste.tipo === 'ENTRADA' ? 'Estoque atualizado.' : 'Uso registrado.');
            setAjuste(null);
            recarregar();
          }}>
          <Campo rotulo={`Quantidade (${ajuste.m.unidade})`} nome="quantidade" tipo="number" min="0.01" step="0.01" required inteiro={ajuste.tipo === 'SAIDA'} />
          {ajuste.tipo === 'ENTRADA' && <>
            <Campo rotulo="Valor pago (R$)" nome="valorCompra" tipo="number" min="0" step="0.01" dica="Atualiza o custo do material e o preço das fichas. Em branco, usa o custo cadastrado." />
            {ativos.length > 0 && <Campo rotulo="Onde comprou" nome="fornecedorId" opcoes={[['', 'Não informar'], ...ativos.map((f) => [String(f.id), f.nome])]} inteiro
              dica="Com valor e fornecedor, a compra entra no histórico de preços." />}
            <p className="inteiro mudo" style={{ fontSize: 14 }}>Comprou vários materiais de uma vez ou pagou frete? Use <button type="button" className="link-botao" onClick={() => { setAjuste(null); setCompraVarios(true); }}>Nova compra</button>.</p>
            <label className="checkbox inteiro"><input type="checkbox" name="registrarDespesa" defaultChecked /> Lançar a compra como despesa no financeiro</label>
          </>}
        </ModalForm>
      )}

      {precos && <HistoricoPrecos material={precos} aoFechar={() => setPrecos(null)} />}
      {compraVarios && <NovaCompra fornecedores={ativos} aoFechar={() => setCompraVarios(false)}
        aoSalvar={(c) => { setCompraVarios(false); toast(`Compra de ${moeda(c.total)} registrada.`); recarregar(); fornecedores.recarregar(); }} />}

      {excluir && <Confirmar titulo="Excluir material" perigo textoBotao="Excluir" texto={`Excluir ${excluir.nome}?`}
        aoFechar={() => setExcluir(null)} aoConfirmar={async () => { await api.del(`/api/materiais/${excluir.id}`); toast('Material excluído.'); setExcluir(null); recarregar(); }} />}
    </>
  );
}
