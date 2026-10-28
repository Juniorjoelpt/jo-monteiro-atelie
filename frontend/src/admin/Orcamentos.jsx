import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Campo, Confirmar, Estado, Modal, ModalForm, Selo, lerForm, useCarregar, useToast, Vazio } from '../componentes/ui.jsx';
import { FRENTES, STATUS_ORCAMENTO, TIPOS_PECA, data, linkWhats, moeda, telefone } from '../util.js';
import { Topo } from './Layout.jsx';
import CamposFicha from '../componentes/CamposFicha.jsx';
import { ImagemProtegida } from './Encomendas.jsx';

const FILTROS = [['', 'Todos'], ['NOVO', 'Novos'], ['RESPONDIDO', 'Respondidos'], ['APROVADO', 'Aprovados'], ['RECUSADO', 'Recusados']];

export default function Orcamentos() {
  const [params, setParams] = useSearchParams();
  const [filtro, setFiltro] = useState('');
  const [aberto, setAberto] = useState(null);
  const [form, setForm] = useState(params.get('novo') ? {} : null);
  const toast = useToast();
  const { dados, erro, carregando, recarregar } = useCarregar(
    () => api.get(`/api/orcamentos${filtro ? `?status=${filtro}` : ''}`), [filtro]);

  function fecharForm() {
    setForm(null);
    if (params.get('novo')) setParams({});
  }

  return (
    <>
      <Topo titulo="Orçamentos">
        <button type="button" className="btn btn-terra" onClick={() => setForm({})}><Icone nome="mais" tamanho={18} largura={3} />Novo orçamento</button>
      </Topo>
      <div className="chips" role="group" aria-label="Filtrar por status" style={{ alignSelf: 'flex-start' }}>
        {FILTROS.map(([v, r]) => <button key={v} type="button" className="chip" aria-pressed={filtro === v} onClick={() => setFiltro(v)}>{r}</button>)}
      </div>
      <section className="cartao">
        <Estado carregando={carregando} erro={erro}>
          {dados?.length === 0 ? <Vazio>Nenhum orçamento {filtro ? 'neste status' : 'ainda'}. Os pedidos do site aparecem aqui.</Vazio> : (
            <div className="tabela-caixa">
              <table className="tabela">
                <thead><tr><th>Recebido</th><th>Cliente</th><th>Peça</th><th>Para quando</th><th className="num">Valor</th><th>Status</th></tr></thead>
                <tbody>
                  {dados?.map((o) => (
                    <tr key={o.id} className="clicavel" onClick={() => setAberto(o)} tabIndex={0} onKeyDown={(e) => e.key === 'Enter' && setAberto(o)}>
                      <td>{data(o.criadoEm)}{o.origem === 'SITE' && <span className="selo selo-oliva" style={{ marginLeft: 8 }}>site</span>}</td>
                      <td><strong>{o.nome}</strong><br /><span className="mudo" style={{ fontSize: 14 }}>{telefone(o.whatsapp)}</span></td>
                      <td>{o.produtoNome || o.tipoPeca}{o.quantidade > 1 && <span className="mudo"> · {o.quantidade} un.</span>}
                        {o.fotos?.length > 0 && <span className="selo selo-areia" style={{ marginLeft: 8 }}><Icone nome="foto" tamanho={14} />{o.fotos.length}</span>}</td>
                      <td>{data(o.dataDesejada)}</td>
                      <td className="num">{o.valorProposto != null ? moeda(o.valorProposto) : '—'}</td>
                      <td><Selo mapa={STATUS_ORCAMENTO} valor={o.status} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Estado>
      </section>

      {aberto && <Detalhe orcamento={aberto} aoFechar={() => setAberto(null)}
        aoEditar={() => { setForm(aberto); setAberto(null); }}
        aoMudar={() => { setAberto(null); recarregar(); }} />}

      {form && (
        <ModalForm titulo={form.id ? 'Editar orçamento' : 'Novo orçamento'} aoFechar={fecharForm} aoSalvar={async (fd) => {
          const d = lerForm(fd, ['valorProposto', 'quantidade']);
          d.clienteId = form.clienteId ?? null;
          d.produtoId = form.produtoId ?? null;
          if (form.id) await api.put(`/api/orcamentos/${form.id}`, d);
          else await api.post('/api/orcamentos', d);
          toast('Orçamento salvo.');
          fecharForm();
          recarregar();
        }}>
          <Campo rotulo="Nome da cliente" nome="nome" defaultValue={form.nome} required maxLength={140} />
          <Campo rotulo="WhatsApp" nome="whatsapp" defaultValue={form.whatsapp ? telefone(form.whatsapp) : ''} inputMode="tel" placeholder="(98) 90000-0000" />
          <Campo rotulo="Tipo de peça" nome="tipoPeca" defaultValue={form.tipoPeca || TIPOS_PECA[0]}
            opcoes={Object.fromEntries([...new Set([...TIPOS_PECA, form.tipoPeca].filter(Boolean))].map((t) => [t, t]))} />
          <Campo rotulo="Quantidade" nome="quantidade" tipo="number" min="1" step="1" defaultValue={form.quantidade ?? 1} />
          <Campo rotulo="Cores ou tema" nome="cores" defaultValue={form.cores} maxLength={160} />
          <Campo rotulo="Tamanho (opcional)" nome="medidas" defaultValue={form.medidas} maxLength={160} />
          <Campo rotulo="Personalização" nome="personalizacao" linhas={2} defaultValue={form.personalizacao} maxLength={2000} inteiro
            dica="Nome, frase, data, tema… o que vai na peça." />
          <Campo rotulo="Para quando" nome="dataDesejada" tipo="date" defaultValue={form.dataDesejada} />
          <Campo rotulo="Valor proposto (R$)" nome="valorProposto" tipo="number" min="0" step="0.01" defaultValue={form.valorProposto} />
          <Campo rotulo="Detalhes" nome="detalhes" linhas={3} defaultValue={form.detalhes} inteiro />
        </ModalForm>
      )}
    </>
  );
}

function Detalhe({ orcamento: o, aoFechar, aoEditar, aoMudar }) {
  const [aprovar, setAprovar] = useState(false);
  const [excluir, setExcluir] = useState(false);
  const [erro, setErro] = useState(null);
  const [gerando, setGerando] = useState(false);
  const [foto, setFoto] = useState(null);
  const toast = useToast();

  async function proposta() {
    setErro(null);
    setGerando(true);
    try { toast(`${await api.baixar(`/api/orcamentos/${o.id}/proposta`)} baixada. Envie para a cliente pelo WhatsApp.`); }
    catch (err) { setErro(err.message); }
    finally { setGerando(false); }
  }

  async function status(s) {
    try {
      await api.patch(`/api/orcamentos/${o.id}/status`, { status: s });
      toast(s === 'RESPONDIDO' ? 'Marcado como respondido.' : 'Orçamento atualizado.');
      aoMudar();
    } catch (e) { setErro(e.message); }
  }

  const resposta = `Olá, ${o.nome.split(' ')[0]}! Aqui é a Jô, do ateliê. Recebi seu pedido de orçamento (${o.produtoNome || o.tipoPeca})`;
  const fechado = o.status === 'APROVADO';

  if (aprovar) {
    return (
      <ModalForm titulo="Aprovar e criar encomenda" textoSalvar="Criar encomenda" aoFechar={() => setAprovar(false)} aoSalvar={async (fd) => {
        const d = { ...lerForm(fd, ['valorTotal', 'valorSinal', 'produtoId', 'quantidade']), precisaArte: fd.has('precisaArte') };
        await api.post(`/api/orcamentos/${o.id}/aprovar`, d);
        toast('Encomenda criada!');
        aoMudar();
      }}>
        <Campo rotulo="Descrição da encomenda" nome="descricao" defaultValue={`${o.produtoNome || o.tipoPeca}${o.quantidade > 1 ? ` (${o.quantidade} un.)` : ''}`.slice(0, 200)} required inteiro />
        <Campo rotulo="Frente" nome="frente" opcoes={FRENTES} defaultValue={frentePorTipo(o.tipoPeca)} />
        <Campo rotulo="Prazo de entrega" nome="prazoEntrega" tipo="date" defaultValue={o.dataDesejada} />
        <CamposFicha produtoId={o.produtoId} quantidade={o.quantidade} valorTotal={o.valorProposto} />
        <Campo rotulo="Sinal (R$)" nome="valorSinal" tipo="number" min="0" step="0.01" dica="Deixe 0 se não houver sinal: a encomenda já vai para produção." />
        <Campo rotulo="Personalização" nome="personalizacao" linhas={2} maxLength={2000} defaultValue={o.personalizacao} inteiro />
        <label className="checkbox inteiro"><input type="checkbox" name="precisaArte" defaultChecked /> Peça personalizada: a cliente aprova a arte pelo link antes da produção</label>
        {o.fotos?.length > 0 && <p className="inteiro mudo forte" style={{ fontSize: 14 }}>As {o.fotos.length === 1 ? 'foto de referência vai' : `${o.fotos.length} fotos de referência vão`} junto para a encomenda.</p>}
      </ModalForm>
    );
  }
  if (excluir) {
    return <Confirmar titulo="Excluir orçamento" perigo textoBotao="Excluir" texto={`Excluir o pedido de ${o.nome}? Isso não pode ser desfeito.`}
      aoFechar={() => setExcluir(false)} aoConfirmar={async () => { await api.del(`/api/orcamentos/${o.id}`); toast('Orçamento excluído.'); aoMudar(); }} />;
  }

  return (
    <Modal titulo={`Orçamento de ${o.nome}`} aoFechar={aoFechar} largo rodape={<>
      {!fechado && <button type="button" className="btn btn-perigo" onClick={() => setExcluir(true)}><Icone nome="lixo" tamanho={18} />Excluir</button>}
      {!fechado && <button type="button" className="btn btn-claro" onClick={aoEditar}><Icone nome="editar" tamanho={18} />Editar</button>}
      {!fechado && <button type="button" className="btn btn-claro" onClick={proposta} disabled={gerando || o.valorProposto == null}
        title={o.valorProposto == null ? 'Informe o valor proposto (Editar) para gerar a proposta' : undefined}><Icone nome="pdf" tamanho={18} />{gerando ? 'Gerando…' : 'Proposta (PDF)'}</button>}
      {o.status === 'NOVO' && <button type="button" className="btn btn-claro" onClick={() => status('RESPONDIDO')}>Marcar como respondido</button>}
      {!fechado && o.status !== 'RECUSADO' && <button type="button" className="btn btn-claro" onClick={() => status('RECUSADO')}>Recusado</button>}
      {o.status === 'RECUSADO' && <button type="button" className="btn btn-claro" onClick={() => status('RESPONDIDO')}>Reabrir</button>}
      {!fechado && <button type="button" className="btn btn-terra" onClick={() => setAprovar(true)}><Icone nome="check" tamanho={18} />Aprovar e criar encomenda</button>}
      {fechado && <Link to="/admin/encomendas" className="btn btn-terra">Ver encomenda</Link>}
    </>}>
      {erro && <div className="aviso aviso-erro" role="alert" style={{ marginBottom: 12 }}>{erro}</div>}
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, alignItems: 'center', marginBottom: 18 }}>
        <Selo mapa={STATUS_ORCAMENTO} valor={o.status} />
        <span className="mudo forte">Recebido em {data(o.criadoEm)} {o.origem === 'SITE' ? 'pelo site' : ''}</span>
      </div>
      <div className="form-grade">
        <Info rotulo="Peça" valor={o.produtoNome ? `${o.produtoNome} (do catálogo)` : o.tipoPeca} />
        <Info rotulo="WhatsApp" valor={o.whatsapp ? <a href={linkWhats(o.whatsapp, resposta)} target="_blank" rel="noopener noreferrer">{telefone(o.whatsapp)}</a> : '—'} />
        <Info rotulo="Quantidade" valor={o.quantidade} />
        <Info rotulo="Para quando" valor={data(o.dataDesejada)} />
        <Info rotulo="Cores ou tema" valor={o.cores || '—'} />
        {o.medidas && <Info rotulo="Tamanho" valor={o.medidas} />}
        <div className="inteiro"><Info rotulo="Personalização" valor={o.personalizacao || '—'} /></div>
        <Info rotulo="Valor proposto" valor={o.valorProposto != null ? moeda(o.valorProposto) : '—'} />
        <div className="inteiro"><Info rotulo="Detalhes" valor={o.detalhes || '—'} /></div>
      </div>
      {o.itens?.length > 0 && (
        <div style={{ marginTop: 18 }}>
          <p className="forte" style={{ marginBottom: 8 }}>Peças da sacola do site</p>
          <ul className="sacola-resumo">
            {o.itens.map((i) => (
              <li key={i.produtoId}>
                <span className="nome">{i.nome}</span>
                <span className="qtd">{i.quantidade}×</span>
                <span className="valor">{i.precoReferencia != null ? moeda(Number(i.precoReferencia) * i.quantidade) : 'sob consulta'}</span>
              </li>
            ))}
          </ul>
          <p className="mudo" style={{ fontSize: 13, marginTop: 6 }}>Valores pelo preço de hoje no catálogo, só como referência.
            {' '}Total: <strong>{moeda(o.itens.reduce((s, i) => s + Number(i.precoReferencia || 0) * i.quantidade, 0))}</strong></p>
        </div>
      )}
      {o.fotos?.length > 0 && (
        <div style={{ marginTop: 18 }}>
          <p className="forte" style={{ marginBottom: 8 }}>Fotos de referência</p>
          <div className="fotos-grade">
            {o.fotos.map((f, n) => (
              <figure key={f.id} className="foto-card">
                <button type="button" className="moldura" style={{ border: 0, padding: 0, cursor: 'zoom-in' }} onClick={() => setFoto(f)} aria-label={`Ampliar foto ${n + 1}`}>
                  <ImagemProtegida url={f.url} alt={`Referência ${n + 1}`} />
                </button>
              </figure>
            ))}
          </div>
        </div>
      )}
      {foto && (
        <Modal titulo="Foto de referência" aoFechar={() => setFoto(null)} largo>
          <div style={{ borderRadius: 20, overflow: 'hidden', background: 'var(--areia)' }}>
            <ImagemProtegida url={foto.url} alt="Foto de referência" style={{ width: '100%', maxHeight: '70vh', objectFit: 'contain' }} />
          </div>
        </Modal>
      )}
      {o.valorProposto == null && !fechado && (
        <p className="mudo forte" style={{ fontSize: 14, marginTop: 14 }}>Para gerar a proposta em PDF, informe o valor proposto em "Editar".</p>
      )}
      {o.whatsapp && !fechado && (
        <a className="btn btn-oliva" style={{ marginTop: 18 }} href={linkWhats(o.whatsapp, resposta)} target="_blank" rel="noopener noreferrer">
          <Icone nome="whats" tamanho={18} />Responder no WhatsApp
        </a>
      )}
    </Modal>
  );
}

/** Frente sugerida pelo tipo de peça escolhido no site. */
function frentePorTipo(tipo = '') {
  const t = tipo.toLowerCase();
  if (t.includes('caneca') || t.includes('porcelana') || t.includes('xícara')) return 'PORCELANA';
  if (t.includes('kit') || t.includes('cesta') || t.includes('garrafa') || t.includes('lembrancinha') || t.includes('presente')) return 'PRESENTE';
  return 'PAPELARIA';
}

export function Info({ rotulo, valor }) {
  return (
    <div className="campo">
      <span className="rotulo mudo">{rotulo}</span>
      <div style={{ fontWeight: 700, whiteSpace: 'pre-line' }}>{valor}</div>
    </div>
  );
}
