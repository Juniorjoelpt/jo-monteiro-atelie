import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api, sessao } from '../api.js';
import { FormCompromisso } from './Agenda.jsx';
import Icone from '../componentes/Icone.jsx';
import { Campo, Confirmar, Estado, Modal, ModalForm, Selo, lerForm, useCarregar, useToast, Vazio } from '../componentes/ui.jsx';
import { FORMAS, FRENTES, STATUS_ARTE, STATUS_ENCOMENDA, data, dataHora, hojeIso, linkWhats, moeda, moedaUnit, primeiroNome, qtd, qtdUn, telefone } from '../util.js';
import Quadro from './Quadro.jsx';
import ArteEncomenda from './ArteEncomenda.jsx';
import CamposFicha from '../componentes/CamposFicha.jsx';
import { Topo } from './Layout.jsx';
import { Info } from './Orcamentos.jsx';

const FILTROS = [['', 'Todas'], ['AGUARDANDO_SINAL', 'Aguardando sinal'], ['EM_PRODUCAO', 'Em produção'], ['PRONTA', 'Prontas'], ['ENTREGUE', 'Entregues'], ['CANCELADA', 'Canceladas']];
/** Filtro pela arte (vem do painel e do menu): PENDENTES = a criar ou com ajuste pedido. */
const FILTROS_ARTE = [['', 'Todas'], ['PENDENTES', 'Arte a fazer'], ['AJUSTE_PEDIDO', 'Ajuste pedido'], ['AGUARDANDO_CLIENTE', 'Com a cliente'], ['APROVADA', 'Aprovada']];
const ARTE_ATIVA = ['AGUARDANDO_SINAL', 'EM_PRODUCAO', 'PRONTA'];
const PROXIMO = { AGUARDANDO_SINAL: 'EM_PRODUCAO', EM_PRODUCAO: 'PRONTA', PRONTA: 'ENTREGUE' };
/** A ajudante só avança a produção: pronta e entregue. */
const PROXIMO_AJUDANTE = { EM_PRODUCAO: 'PRONTA', PRONTA: 'ENTREGUE' };

export default function Encomendas() {
  const [params, setParams] = useSearchParams();
  const [visao, setVisao] = useState(() => {
    try { return params.get('ver') || localStorage.getItem('atelie.encomendas.visao') || 'lista'; } catch { return 'lista'; }
  });
  function mudarVisao(v) {
    setVisao(v);
    try { localStorage.setItem('atelie.encomendas.visao', v); } catch { /* sem armazenamento */ }
  }
  const [filtro, setFiltro] = useState('');
  const [filtroArte, setFiltroArte] = useState(params.get('arte') || '');
  const arteUrl = params.get('arte');
  useEffect(() => { if (arteUrl) setFiltroArte(arteUrl); }, [arteUrl]);
  const [aberta, setAberta] = useState(params.get('abrir') ? { id: Number(params.get('abrir')) } : null);
  const [form, setForm] = useState(null);
  const toast = useToast();
  const ajudante = sessao.ajudante();
  const { dados, erro, carregando, recarregar } = useCarregar(
    () => api.get(`/api/encomendas${filtro && visao === 'lista' ? `?status=${filtro}` : ''}`), [filtro, visao]);
  const lista = dados?.filter((e) => !filtroArte || (ARTE_ATIVA.includes(e.status) && (filtroArte === 'PENDENTES'
    ? ['A_CRIAR', 'AJUSTE_PEDIDO'].includes(e.statusArte) : e.statusArte === filtroArte)));

  return (
    <>
      <Topo titulo="Encomendas">
        {!ajudante && <button type="button" className="btn btn-terra" onClick={() => setForm({})}><Icone nome="mais" tamanho={18} largura={3} />Nova encomenda</button>}
      </Topo>
      <div className="filtros">
        <div className="chips" role="group" aria-label="Como ver as encomendas">
          <button type="button" className="chip" aria-pressed={visao === 'quadro'} onClick={() => mudarVisao('quadro')}><Icone nome="encomenda" tamanho={16} />Quadro</button>
          <button type="button" className="chip" aria-pressed={visao === 'lista'} onClick={() => mudarVisao('lista')}><Icone nome="menu" tamanho={16} />Lista</button>
        </div>
      </div>
      {visao === 'quadro' ? (
        <Estado carregando={carregando && !dados} erro={erro}>
          {dados && <Quadro encomendas={dados} aoAbrir={setAberta} aoMudar={recarregar} />}
        </Estado>
      ) : <>
      <div className="filtros">
        <div className="chips" role="group" aria-label="Filtrar por status">
          {FILTROS.map(([v, r]) => <button key={v} type="button" className="chip" aria-pressed={filtro === v} onClick={() => setFiltro(v)}>{r}</button>)}
        </div>
        <div className="chips chips-p" role="group" aria-label="Filtrar pela arte">
          {FILTROS_ARTE.map(([v, r]) => <button key={v} type="button" className="chip" aria-pressed={filtroArte === v}
            onClick={() => { setFiltroArte(v); if (params.get('arte')) setParams({}); }}>{v ? r : 'Arte: todas'}</button>)}
        </div>
      </div>
      <section className="cartao">
        <Estado carregando={carregando} erro={erro}>
          {lista?.length === 0 ? <Vazio>{filtroArte ? 'Nenhuma encomenda com a arte nesta situação.' : `Nenhuma encomenda ${filtro ? 'neste status' : 'ainda'}. Aprove um orçamento ou crie uma nova.`}</Vazio> : (
            <div className="tabela-caixa">
              <table className="tabela">
                <thead><tr><th>Encomenda</th><th>Cliente</th><th>Entrega</th>{!ajudante && <><th className="num">Total</th><th className="num">Falta receber</th></>}<th>Arte</th><th>Status</th></tr></thead>
                <tbody>
                  {lista?.map((e) => (
                    <tr key={e.id} className="clicavel" tabIndex={0} onClick={() => setAberta(e)} onKeyDown={(k) => k.key === 'Enter' && setAberta(e)}>
                      <td>{e.urgente && <span className="selo selo-perigo selo-p" style={{ marginRight: 6 }}><Icone nome="raio" tamanho={12} />Urgente</span>}<strong>{e.descricao}</strong><br /><span className="mudo" style={{ fontSize: 14 }}>{FRENTES[e.frente]}{e.produtoNome ? ` · ${e.quantidade}× ${e.produtoNome}` : ''}</span></td>
                      <td>{e.clienteNome}</td>
                      <td>{data(e.prazoEntrega)}</td>
                      {!ajudante && <>
                        <td className="num">{moeda(e.valorTotal)}</td>
                        <td className="num" style={{ fontWeight: 800, color: Number(e.saldo) > 0 ? 'var(--terra-texto)' : 'var(--oliva-texto)' }}>{Number(e.saldo) > 0 ? moeda(e.saldo) : 'Quitada'}</td>
                      </>}
                      <td>{e.statusArte === 'SEM_ARTE' ? <span className="mudo">—</span> : <><Selo mapa={STATUS_ARTE} valor={e.statusArte} />
                        {e.statusArte === 'AGUARDANDO_CLIENTE' && <span className="mudo forte" style={{ display: 'block', fontSize: 12.5, marginTop: 3 }}>{e.arteVistaEm ? 'vista pela cliente' : 'ainda não vista'}</span>}</>}</td>
                      <td><Selo mapa={STATUS_ENCOMENDA} valor={e.status} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Estado>
      </section>
      </>}

      {aberta && <Detalhe id={aberta.id} aoFechar={() => { setAberta(null); if (params.get('abrir')) setParams({}); recarregar(); }}
        aoEditar={(e) => { setAberta(null); setForm(e); }} />}

      {form && <FormEncomenda encomenda={form} aoFechar={() => setForm(null)} aoSalvar={() => { setForm(null); toast('Encomenda salva.'); recarregar(); }} />}
    </>
  );
}

function FormEncomenda({ encomenda: e, aoFechar, aoSalvar }) {
  const { dados: clientes } = useCarregar(() => api.get('/api/clientes'));
  if (!clientes) return null;
  if (clientes.length === 0) {
    return (
      <Modal titulo="Nova encomenda" aoFechar={aoFechar}>
        <p>Cadastre a cliente primeiro em <strong>Clientes</strong>, ou crie a encomenda aprovando um orçamento.</p>
      </Modal>
    );
  }
  return (
    <ModalForm titulo={e.id ? 'Editar encomenda' : 'Nova encomenda'} aoFechar={aoFechar} aoSalvar={async (fd) => {
      const d = { ...lerForm(fd, ['valorTotal', 'valorSinal', 'clienteId', 'produtoId', 'quantidade', 'horasTrabalhadas']), precisaArte: fd.has('precisaArte'), urgente: fd.has('urgente') };
      if (e.id) await api.put(`/api/encomendas/${e.id}`, d);
      else await api.post('/api/encomendas', d);
      aoSalvar();
    }}>
      <Campo rotulo="Cliente" nome="clienteId" defaultValue={e.clienteId} inteiro
        opcoes={Object.fromEntries(clientes.map((c) => [c.id, `${c.nome}${c.whatsapp ? ` · ${telefone(c.whatsapp)}` : ''}`]))} />
      <Campo rotulo="Descrição" nome="descricao" defaultValue={e.descricao} required maxLength={200} inteiro />
      <Campo rotulo="Frente" nome="frente" opcoes={FRENTES} defaultValue={e.frente || 'PAPELARIA'} />
      <Campo rotulo="Status" nome="status" opcoes={Object.fromEntries(Object.entries(STATUS_ENCOMENDA).map(([k, [r]]) => [k, r]))} defaultValue={e.status || 'AGUARDANDO_SINAL'} />
      <CamposFicha produtoId={e.produtoId} quantidade={e.quantidade} valorTotal={e.valorTotal} travado={e.materiaisBaixados && !!e.produtoId} />
      <Campo rotulo="Sinal (R$)" nome="valorSinal" tipo="number" min="0" step="0.01" defaultValue={e.valorSinal ?? 0} />
      <Campo rotulo="Horas trabalhadas" nome="horasTrabalhadas" tipo="number" min="0" step="0.25" defaultValue={e.horasTrabalhadas}
        dica="Deixe vazio para usar as horas da ficha." />
      <Campo rotulo="Prazo de entrega" nome="prazoEntrega" tipo="date" defaultValue={e.prazoEntrega} />
      <Campo rotulo="Personalização" nome="personalizacao" linhas={2} maxLength={2000} defaultValue={e.personalizacao} inteiro
        dica="Nome, frase, data, tema… o que vai na peça." />
      <label className="checkbox inteiro"><input type="checkbox" name="precisaArte" defaultChecked={e.id ? e.statusArte !== 'SEM_ARTE' : true} disabled={e.versoesArte > 0} />
        Peça personalizada: a cliente aprova a arte pelo link antes da produção</label>
      <label className="checkbox inteiro"><input type="checkbox" name="urgente" defaultChecked={!!e.urgente} />
        Urgente: aparece primeiro no quadro de produção</label>
      <Campo rotulo="Observações" nome="observacoes" linhas={3} defaultValue={e.observacoes} inteiro />
    </ModalForm>
  );
}

function Detalhe({ id, aoFechar, aoEditar }) {
  const ajudante = sessao.ajudante();
  const { dados: e, erro, carregando, recarregar } = useCarregar(() => api.get(`/api/encomendas/${id}`));
  const { dados: pagamentos, recarregar: recarregarPag } = useCarregar(() => (ajudante ? Promise.resolve([]) : api.get(`/api/encomendas/${id}/pagamentos`)));
  const [pagar, setPagar] = useState(false);
  const [confirmar, setConfirmar] = useState(null);
  const [falha, setFalha] = useState(null);
  const [agendar, setAgendar] = useState(false);
  const [gerando, setGerando] = useState(false);
  const toast = useToast();

  async function contrato() {
    setGerando(true);
    try { toast(`${await api.baixar(`/api/encomendas/${id}/contrato`)} baixado.`); }
    catch (err) { setFalha(err.message); }
    finally { setGerando(false); }
  }

  async function urgente(valor) {
    try {
      await api.patch(`/api/encomendas/${id}/urgente`, { urgente: valor });
      toast(valor ? 'Marcada como urgente.' : 'Tirou a urgência.');
      recarregar();
    } catch (err) { setFalha(err.message); }
  }

  async function mudar(status) {
    try {
      await api.patch(`/api/encomendas/${id}/status`, { status });
      toast(`Status: ${STATUS_ENCOMENDA[status][0]}.`);
      setConfirmar(null);
      recarregar();
    } catch (err) { setFalha(err.message); }
  }

  if (pagar && e) {
    return (
      <ModalForm titulo="Registrar pagamento" textoSalvar="Registrar" aoFechar={() => setPagar(false)} aoSalvar={async (fd) => {
        await api.post(`/api/encomendas/${id}/pagamentos`, lerForm(fd, ['valor']));
        toast('Pagamento registrado.');
        setPagar(false);
        recarregar();
        recarregarPag();
      }}>
        <div className="inteiro aviso aviso-info">Falta receber {moeda(e.saldo)} de {e.clienteNome}.</div>
        <Campo rotulo="Valor (R$)" nome="valor" tipo="number" min="0.01" step="0.01" required
          defaultValue={Number(e.valorPago) === 0 && Number(e.valorSinal) > 0 ? e.valorSinal : e.saldo} />
        <Campo rotulo="Data" nome="data" tipo="date" defaultValue={hojeIso()} />
        <Campo rotulo="Forma" nome="formaPagamento" opcoes={FORMAS} defaultValue="PIX" />
        <Campo rotulo="Descrição (opcional)" nome="descricao" maxLength={200} placeholder="Sinal, saldo final…" />
      </ModalForm>
    );
  }
  if (agendar && e) {
    return <FormCompromisso compromisso={{ tipo: 'PRODUCAO', titulo: `Produzir: ${e.descricao}`.slice(0, 160), encomendaId: e.id, clienteId: e.clienteId, inicio: `${hojeIso()}T09:00` }}
      aoFechar={() => setAgendar(false)} aoSalvar={() => setAgendar(false)} />;
  }
  if (confirmar) {
    return <Confirmar titulo={confirmar === 'EXCLUIR' ? 'Excluir encomenda' : 'Cancelar encomenda'} perigo
      textoBotao={confirmar === 'EXCLUIR' ? 'Excluir' : 'Cancelar encomenda'}
      texto={confirmar === 'EXCLUIR' ? 'Excluir esta encomenda? Isso não pode ser desfeito.' : 'A encomenda sai das entregas e do "a receber". Os pagamentos já registrados continuam no financeiro.'}
      aoFechar={() => setConfirmar(null)}
      aoConfirmar={async () => {
        if (confirmar === 'EXCLUIR') { await api.del(`/api/encomendas/${id}`); toast('Encomenda excluída.'); aoFechar(); }
        else await mudar('CANCELADA');
      }} />;
  }

  const proximo = e && (ajudante ? PROXIMO_AJUDANTE : PROXIMO)[e.status];
  return (
    <Modal titulo={e ? e.descricao : 'Encomenda'} aoFechar={aoFechar} largo rodape={e && <>
      {!ajudante && <>
        {e.status !== 'CANCELADA' && e.status !== 'ENTREGUE' && <button type="button" className="btn btn-perigo" onClick={() => setConfirmar(Number(e.valorPago) > 0 ? 'CANCELAR' : 'EXCLUIR')}>{Number(e.valorPago) > 0 ? 'Cancelar' : 'Excluir'}</button>}
        <button type="button" className="btn btn-claro" onClick={() => aoEditar(e)}><Icone nome="editar" tamanho={18} />Editar</button>
        <button type="button" className="btn btn-claro" onClick={contrato} disabled={gerando}><Icone nome="pdf" tamanho={18} />{gerando ? 'Gerando…' : 'Contrato'}</button>
      </>}
      <a className="btn btn-claro" href={`/admin/encomendas/${e.id}/ficha`} target="_blank" rel="noopener noreferrer"><Icone nome="recibo" tamanho={18} />Ficha de produção</a>
      {e.status !== 'CANCELADA' && e.status !== 'ENTREGUE' && <button type="button" className="btn btn-claro" onClick={() => setAgendar(true)}><Icone nome="calendario" tamanho={18} />Agendar</button>}
      {!ajudante && Number(e.saldo) > 0 && <button type="button" className="btn btn-claro" onClick={() => setPagar(true)}><Icone nome="dinheiro" tamanho={18} />Registrar pagamento</button>}
      {proximo && <button type="button" className="btn btn-terra" onClick={() => mudar(proximo)}>Marcar como {STATUS_ENCOMENDA[proximo][0].toLowerCase()}</button>}
    </>}>
      <Estado carregando={carregando} erro={erro}>
        {e && (
          <>
            {falha && <div className="aviso aviso-erro" role="alert" style={{ marginBottom: 12 }}>{falha}</div>}
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, alignItems: 'center', marginBottom: 18 }}>
              <Selo mapa={STATUS_ENCOMENDA} valor={e.status} /><span className="selo selo-areia">{FRENTES[e.frente]}</span>
              {e.urgente && <span className="selo selo-perigo"><Icone nome="raio" tamanho={14} />Urgente</span>}
              {!ajudante && e.status !== 'CANCELADA' && e.status !== 'ENTREGUE' && (
                <button type="button" className="btn btn-fantasma btn-p" onClick={() => urgente(!e.urgente)}>
                  <Icone nome="raio" tamanho={16} />{e.urgente ? 'Tirar urgência' : 'Marcar como urgente'}
                </button>
              )}
            </div>
            {!ajudante && <div className="grade-resumo" style={{ marginBottom: 20 }}>
              {[['Total', e.valorTotal, 'var(--areia)'], ['Recebido', e.valorPago, 'var(--oliva-claro)'], ['Falta receber', e.saldo, 'var(--terra-claro)']].map(([r, v, f]) => (
                <div key={r} className="clay-sm" style={{ background: f, borderRadius: 22, padding: 16 }}>
                  <p className="forte" style={{ fontSize: 14 }}>{r}</p><p style={{ fontSize: 24, fontWeight: 900 }}>{moeda(v)}</p>
                </div>
              ))}
            </div>}
            <div className="form-grade">
              <Info rotulo="Cliente" valor={e.clienteNome} />
              <Info rotulo="WhatsApp" valor={e.clienteWhatsapp ? <a href={linkWhats(e.clienteWhatsapp, `Olá, ${e.clienteNome.split(' ')[0]}! Aqui é a Jô, do ateliê.`)} target="_blank" rel="noopener noreferrer">{telefone(e.clienteWhatsapp)}</a> : '—'} />
              <Info rotulo="Prazo de entrega" valor={data(e.prazoEntrega)} />
              {!ajudante && <Info rotulo="Sinal combinado" valor={moeda(e.valorSinal)} />}
              {e.produtoNome && <Info rotulo="Peça da ficha" valor={`${e.quantidade}× ${e.produtoNome}`} />}
              {e.personalizacao && <div className="inteiro"><Info rotulo="Personalização" valor={e.personalizacao} /></div>}
              {e.observacoes && <div className="inteiro"><Info rotulo="Observações" valor={e.observacoes} /></div>}
            </div>
            {e.status === 'EM_PRODUCAO' && ['A_CRIAR', 'AGUARDANDO_CLIENTE', 'AJUSTE_PEDIDO'].includes(e.statusArte) && (
              <div className="aviso aviso-info" style={{ marginTop: 16 }}>A arte ainda não foi aprovada. Espere a aprovação antes de produzir.</div>
            )}
            {e.linkPedido && e.status !== 'CANCELADA' && <LinkPedido e={e} />}
            <ArteEncomenda e={e} aoMudar={recarregar} />
            {!ajudante && <Custos e={e} />}
            <MateriaisUsados e={e} aoMudar={() => { recarregar(); }} />
            <FotosEncomenda e={e} />
            {!ajudante && <>
              <h3 style={{ fontSize: 18, margin: '24px 0 8px' }}>Pagamentos</h3>
              {!pagamentos?.length ? <p className="mudo forte">Nenhum pagamento registrado.</p> : (
                <ul className="lista-linhas">
                  {pagamentos.map((p) => (
                    <li key={p.id}><div className="info"><p>{moeda(p.valor)} · {FORMAS[p.formaPagamento]}</p><p>{data(p.data)} · {p.descricao}</p></div></li>
                  ))}
                </ul>
              )}
            </>}
            <HistoricoEncomenda e={e} />
          </>
        )}
      </Estado>
    </Modal>
  );
}

/** Link que a cliente usa para acompanhar o pedido (andamento, data, saldo e arte). */
function LinkPedido({ e }) {
  const toast = useToast();
  const nome = primeiroNome(e.clienteNome);
  const mensagem = `Oi, ${nome}! Por este link você acompanha a sua encomenda (${e.descricao}): cada etapa, a data combinada`
    + ` e o pagamento. ${e.linkPedido}`;
  async function copiar() {
    try { await navigator.clipboard.writeText(e.linkPedido); toast('Link copiado.'); } catch { toast('Não deu para copiar. Selecione o link e copie à mão.'); }
  }
  return (
    <section aria-labelledby={`link-${e.id}`} style={{ marginTop: 24 }}>
      <h3 id={`link-${e.id}`} style={{ fontSize: 18, marginBottom: 8, display: 'flex', alignItems: 'center', gap: 8 }}>
        <Icone nome="link" tamanho={20} cor="var(--terra)" />Acompanhamento da cliente
      </h3>
      <div className="link-arte">
        <code>{e.linkPedido}</code>
        <button type="button" className="btn btn-claro btn-p" onClick={copiar}><Icone nome="copiar" tamanho={16} />Copiar</button>
        <a className="btn btn-claro btn-p" href={e.linkPedido} target="_blank" rel="noopener noreferrer"><Icone nome="olho" tamanho={16} />Ver como a cliente</a>
        {e.clienteWhatsapp && (
          <a className="btn btn-oliva btn-p" href={linkWhats(e.clienteWhatsapp, mensagem)} target="_blank" rel="noopener noreferrer">
            <Icone nome="whats" tamanho={16} />Mandar para {nome}
          </a>
        )}
      </div>
      <p className="mudo forte" style={{ fontSize: 13.5, marginTop: 6 }}>
        {e.acompanhadoEm ? `${nome} abriu o link pela última vez ${dataHora(e.acompanhadoEm)}.` : `${nome} ainda não abriu o link.`}
        {' '}Quando você abre o link logada, não conta como visita.
      </p>
    </section>
  );
}

const ROTULO_ACAO = { CRIOU: 'criou', EDITOU: 'editou', VALORES: 'valores', STATUS: 'status', PAGAMENTO: 'pagamento', URGENTE: 'urgente',
  ARTE: 'arte', VIU_ARTE: 'viu a arte', ABRIU: 'abriu o link', MATERIAL: 'material', FOTO: 'foto' };

/** Linha do tempo: quem fez o quê nesta encomenda. */
function HistoricoEncomenda({ e }) {
  const [tudo, setTudo] = useState(false);
  const h = useCarregar(() => api.get(`/api/encomendas/${e.id}/historico`),
    [e.id, e.status, e.statusArte, e.valorPago, e.urgente, e.versoesArte, e.materiaisBaixados]);
  const lista = h.dados || [];
  const mostrar = tudo ? lista : lista.slice(0, 6);
  return (
    <section aria-labelledby={`hist-${e.id}`} style={{ marginTop: 24 }}>
      <h3 id={`hist-${e.id}`} style={{ fontSize: 18, marginBottom: 8, display: 'flex', alignItems: 'center', gap: 8 }}>
        <Icone nome="historico" tamanho={20} cor="var(--terra)" />Histórico
      </h3>
      {!lista.length ? <p className="mudo forte">{h.carregando ? 'Carregando…' : 'Nada registrado ainda. As próximas mudanças aparecem aqui, com quem fez e quando.'}</p> : (
        <ol className="linha-tempo">
          {mostrar.map((x) => (
            <li key={x.id} className={x.cliente ? 'da-cliente' : ''}>
              <p className="lt-cabeca"><strong>{x.autor}</strong> <span className="selo selo-areia selo-p">{ROTULO_ACAO[x.acao] || x.acao.toLowerCase()}</span> <span className="mudo">{dataHora(x.criadoEm)}</span></p>
              <p className="lt-texto">{x.descricao}</p>
            </li>
          ))}
        </ol>
      )}
      {lista.length > 6 && <button type="button" className="link-botao" style={{ marginTop: 6 }} onClick={() => setTudo(!tudo)}>{tudo ? 'Mostrar menos' : `Ver as ${lista.length} ações`}</button>}
    </section>
  );
}

function Custos({ e }) {
  const c = e.custos;
  const meta = Number(c.valorHoraDesejado);
  const porHora = c.ganhoPorHora != null ? Number(c.ganhoPorHora) : null;
  const bateu = porHora != null && meta > 0 && porHora >= meta;
  return (
    <section aria-labelledby={`custos-${e.id}`} style={{ marginTop: 24 }}>
      <h3 id={`custos-${e.id}`} style={{ fontSize: 18, marginBottom: 10 }}>
        Custos e lucro {c.estimado && <span className="selo selo-ocre" style={{ marginLeft: 6 }}>materiais estimados pela ficha</span>}
      </h3>
      <div className="custos-grade">
        <div><p>Valor cobrado</p><p>{moeda(e.valorTotal)}</p></div>
        <div><p>Materiais</p><p>{moeda(c.custoMateriais)}</p></div>
        <div><p>Custos fixos ({qtd(c.horas)} h)</p><p>{moeda(c.custoFixo)}</p></div>
        {Number(c.outrosCustos) > 0 && <div><p>Embalagem e outros</p><p>{moeda(c.outrosCustos)}</p></div>}
        <div style={{ background: 'var(--oliva-claro)' }}><p>Seu ganho</p><p>{moeda(c.ganho)}</p></div>
        <div style={{ background: porHora == null ? 'var(--cru)' : bateu ? 'var(--oliva-claro)' : 'var(--terra-claro)' }}>
          <p>Por hora trabalhada</p>
          <p>{porHora == null ? '—' : moeda(porHora)}</p>
        </div>
      </div>
      <p className="mudo forte" style={{ fontSize: 14, marginTop: 8 }}>
        {porHora == null ? 'Informe as horas trabalhadas (ou use uma ficha) para ver quanto rendeu sua hora.'
          : meta > 0 ? (bateu
            ? `Acima da sua meta de ${moeda(meta)}/h: sobra um lucro de ${moeda(c.lucro)} além do seu pagamento.`
            : `Abaixo da sua meta de ${moeda(meta)}/h: faltaram ${moeda(-c.lucro)} para pagar todas as suas horas.`)
          : 'Configure sua meta de valor da hora em Configurações.'}
      </p>
    </section>
  );
}

function MateriaisUsados({ e, aoMudar }) {
  const lista = useCarregar(() => api.get(`/api/encomendas/${e.id}/consumos`), [e.id, e.materiaisBaixados]);
  const materiais = useCarregar(() => api.get('/api/materiais'));
  const [novo, setNovo] = useState(null);
  const [erro, setErro] = useState(null);
  const toast = useToast();
  const cancelada = e.status === 'CANCELADA';

  async function acao(fn, msg) {
    setErro(null);
    try { await fn(); toast(msg); lista.recarregar(); aoMudar(); } catch (err) { setErro(err.message); }
  }

  return (
    <section aria-labelledby={`mat-${e.id}`} style={{ marginTop: 24 }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
        <h3 id={`mat-${e.id}`} style={{ fontSize: 18 }}>Materiais usados</h3>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {e.produtoId && !e.materiaisBaixados && !cancelada && (
            <button type="button" className="btn btn-oliva btn-p"
              onClick={() => acao(() => api.post(`/api/encomendas/${e.id}/baixar-ficha`), 'Materiais da ficha baixados do estoque.')}>
              Baixar materiais da ficha agora
            </button>
          )}
          {!cancelada && !novo && <button type="button" className="btn btn-claro btn-p" onClick={() => setNovo({ materialId: '', quantidade: '' })}><Icone nome="mais" tamanho={16} largura={3} />Material extra</button>}
        </div>
      </div>
      {e.produtoId && !e.materiaisBaixados && (
        <p className="mudo forte" style={{ fontSize: 14 }}>Os materiais da ficha saem do estoque sozinhos quando a encomenda entra em produção.</p>
      )}
      {erro && <div className="aviso aviso-erro" role="alert" style={{ margin: '8px 0' }}>{erro}</div>}
      {novo && (
        <form style={{ display: 'flex', flexWrap: 'wrap', gap: 8, alignItems: 'flex-end', margin: '8px 0' }}
          onSubmit={(ev) => {
            ev.preventDefault();
            acao(async () => {
              await api.post(`/api/encomendas/${e.id}/consumos`, { materialId: Number(novo.materialId) || null, quantidade: Number(String(novo.quantidade).replace(',', '.')) || null });
              setNovo(null);
            }, 'Material registrado e baixado do estoque.');
          }}>
          <div className="campo" style={{ flex: '2 1 200px' }}>
            <label htmlFor={`cm-m-${e.id}`}>Material</label>
            <select id={`cm-m-${e.id}`} className="entrada" value={novo.materialId} onChange={(ev) => setNovo({ ...novo, materialId: ev.target.value })} required>
              <option value="">Escolha…</option>
              {(materiais.dados || []).map((m) => <option key={m.id} value={m.id}>{m.nome} ({qtdUn(m.quantidade, m.unidade)} em estoque)</option>)}
            </select>
          </div>
          <div className="campo" style={{ flex: '1 1 100px' }}>
            <label htmlFor={`cm-q-${e.id}`}>Quantidade</label>
            <input id={`cm-q-${e.id}`} className="entrada" type="number" min="0" step="0.001" value={novo.quantidade} onChange={(ev) => setNovo({ ...novo, quantidade: ev.target.value })} required />
          </div>
          <button type="submit" className="btn btn-oliva">Registrar</button>
          <button type="button" className="btn btn-fantasma" onClick={() => setNovo(null)}>Cancelar</button>
        </form>
      )}
      {!lista.dados?.length ? <p className="mudo forte">{lista.carregando ? 'Carregando…' : 'Nenhum material baixado ainda.'}</p> : (
        <ul className="lista-linhas">
          {lista.dados.map((c) => (
            <li key={c.id}>
              <div className="info">
                <p>{c.materialNome}: {qtdUn(c.quantidade, c.unidade)}</p>
                <p>{c.custoUnitario != null ? `${moedaUnit(c.custoUnitario)}/${c.unidade} · ` : ''}{c.origem === 'FICHA_ENCOMENDA' ? 'pela ficha' : 'extra'} · {data(c.data)}</p>
              </div>
              {c.custoTotal != null && <strong className="num">{moeda(c.custoTotal)}</strong>}
              {!cancelada && (
                <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Devolver ${c.materialNome} ao estoque`}
                  onClick={() => acao(() => api.del(`/api/consumos/${c.id}`), 'Material devolvido ao estoque.')}><Icone nome="fechar" tamanho={16} /></button>
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

const ROTULO_FOTO = { REFERENCIA: 'Referência', PRONTA: 'Peça pronta' };

function FotosEncomenda({ e }) {
  const fotos = useCarregar(() => api.get(`/api/encomendas/${e.id}/fotos`), [e.id]);
  const [tipo, setTipo] = useState('REFERENCIA');
  const [enviando, setEnviando] = useState(false);
  const [aberta, setAberta] = useState(null);
  const [erro, setErro] = useState(null);
  const toast = useToast();

  async function enviar(ev) {
    const arquivos = [...ev.target.files];
    ev.target.value = '';
    if (!arquivos.length) return;
    setErro(null);
    setEnviando(true);
    try {
      for (const f of arquivos) {
        if (f.size > 8 * 1024 * 1024) throw new Error(`${f.name} tem mais de 8 MB.`);
        const fd = new FormData();
        fd.append('foto', f);
        fd.append('tipo', tipo);
        await api.enviar(`/api/encomendas/${e.id}/fotos`, fd);
      }
      toast(arquivos.length === 1 ? 'Foto enviada.' : `${arquivos.length} fotos enviadas.`);
    } catch (err) {
      setErro(err.message);
    } finally {
      setEnviando(false);
      fotos.recarregar();
    }
  }

  async function acao(fn, msg) {
    setErro(null);
    try { await fn(); toast(msg); setAberta(null); fotos.recarregar(); } catch (err) { setErro(err.message); }
  }

  return (
    <section aria-labelledby={`fotos-${e.id}`} style={{ marginTop: 24 }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
        <h3 id={`fotos-${e.id}`} style={{ fontSize: 18 }}>Fotos</h3>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, alignItems: 'center' }}>
          <div className="chips chips-p" role="group" aria-label="Tipo da foto">
            {Object.entries(ROTULO_FOTO).map(([k, r]) => <button key={k} type="button" className="chip" aria-pressed={tipo === k} onClick={() => setTipo(k)}>{r}</button>)}
          </div>
          <label className="btn btn-claro btn-p botao-upload">
            <Icone nome="foto" tamanho={16} />{enviando ? 'Enviando…' : 'Adicionar fotos'}
            <input type="file" accept="image/jpeg,image/png,image/webp" multiple onChange={enviar} disabled={enviando} aria-label="Escolher fotos" />
          </label>
        </div>
      </div>
      <p className="mudo" style={{ fontSize: 13, marginBottom: 8 }}>As fotos ficam só no sistema. Fotos da peça pronta podem ir para o portfólio do site com um toque.</p>
      {erro && <div className="aviso aviso-erro" role="alert" style={{ marginBottom: 8 }}>{erro}</div>}
      {!fotos.dados?.length ? <p className="mudo forte">{fotos.carregando ? 'Carregando…' : 'Nenhuma foto ainda. Guarde aqui a referência que a cliente mandou e a foto da peça pronta.'}</p> : (
        <div className="fotos-grade">
          {fotos.dados.map((f) => (
            <figure key={f.id} className="foto-card">
              <button type="button" className="moldura" style={{ border: 0, padding: 0, cursor: 'zoom-in' }} onClick={() => setAberta(f)} aria-label={`Ampliar foto ${ROTULO_FOTO[f.tipo]}`}>
                <ImagemProtegida url={f.url} alt={f.legenda || ROTULO_FOTO[f.tipo]} />
              </button>
              <figcaption>
                <span className={`selo ${f.tipo === 'PRONTA' ? 'selo-oliva' : 'selo-areia'}`}>{ROTULO_FOTO[f.tipo]}</span>
                {f.portfolioItemId && <span className="selo selo-terra" style={{ marginLeft: 4 }}>no site</span>}
              </figcaption>
            </figure>
          ))}
        </div>
      )}
      {aberta && (
        <Modal titulo={ROTULO_FOTO[aberta.tipo]} aoFechar={() => setAberta(null)} largo rodape={<>
          {!sessao.ajudante() && <button type="button" className="btn btn-perigo" onClick={() => acao(() => api.del(`/api/fotos/${aberta.id}`), 'Foto excluída.')}><Icone nome="lixo" tamanho={18} />Excluir</button>}
          {sessao.ajudante() && <button type="button" className="btn btn-terra" onClick={() => setAberta(null)}>Fechar</button>}
          {!sessao.ajudante() && aberta.tipo === 'PRONTA' && !aberta.portfolioItemId && (
            <button type="button" className="btn btn-terra" onClick={() => acao(() => api.post(`/api/fotos/${aberta.id}/publicar`, {}), 'Foto publicada no portfólio do site.')}>
              <Icone nome="site" tamanho={18} />Publicar no portfólio
            </button>
          )}
        </>}>
          <div style={{ borderRadius: 20, overflow: 'hidden', background: 'var(--areia)' }}>
            <ImagemProtegida url={aberta.url} alt={aberta.legenda || ROTULO_FOTO[aberta.tipo]} style={{ width: '100%', maxHeight: '65vh', objectFit: 'contain' }} />
          </div>
          {aberta.legenda && <p className="forte" style={{ marginTop: 10 }}>{aberta.legenda}</p>}
          {aberta.portfolioItemId && <p className="mudo forte" style={{ marginTop: 10 }}>Esta foto já está no portfólio. Para tirar do site, use a tela Portfólio.</p>}
        </Modal>
      )}
    </section>
  );
}

/** <img> para fotos que exigem login: busca com o token e mostra a partir da memória. */
export function ImagemProtegida({ url, alt, style }) {
  const [src, setSrc] = useState(null);
  const [falhou, setFalhou] = useState(false);
  useEffect(() => {
    let vivo = true;
    let criado = null;
    api.imagem(url).then((u) => { criado = u; if (vivo) setSrc(u); else URL.revokeObjectURL(u); }).catch(() => vivo && setFalhou(true));
    return () => { vivo = false; if (criado) URL.revokeObjectURL(criado); };
  }, [url]);
  if (falhou) return <span className="mudo" style={{ fontSize: 12 }}>não abriu</span>;
  if (!src) return <span className="mudo" style={{ fontSize: 12 }}>…</span>;
  return <img src={src} alt={alt} style={style} loading="lazy" />;
}
