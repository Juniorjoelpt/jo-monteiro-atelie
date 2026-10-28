import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, sessao } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Campo, Confirmar, Estado, ModalForm, lerForm, useCarregar, useToast, Vazio } from '../componentes/ui.jsx';
import { STATUS_ENCOMENDA, hojeIso } from '../util.js';
import { Topo } from './Layout.jsx';

const MESES = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'];
const SEMANA = ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb'];

export const TIPOS_COMPROMISSO = {
  ENTREGA: 'Entrega ou retirada', REUNIAO: 'Reunião', PRODUCAO: 'Dia de produção', COMPRA: 'Compra de material', OUTRO: 'Outro',
};
const ROTULO_TIPO = { ENTREGA: 'Entrega', COMPROMISSO: 'Compromisso', ANIVERSARIO: 'Aniversário', CONTA: 'Conta' };
const COR_PONTO = { ENTREGA: 'var(--terra-vivo)', COMPROMISSO: 'var(--ocre)', ANIVERSARIO: 'var(--oliva)', CONTA: '#1F6E8C' };

const iso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;

export default function Agenda() {
  const hoje = new Date();
  const [ref, setRef] = useState({ ano: hoje.getFullYear(), mes: hoje.getMonth() });
  const [selecionado, setSelecionado] = useState(null);
  const [form, setForm] = useState(null);
  const toast = useToast();

  // Grade de 6 semanas começando no domingo
  const dias = useMemo(() => {
    const primeiro = new Date(ref.ano, ref.mes, 1);
    const inicio = new Date(primeiro);
    inicio.setDate(1 - primeiro.getDay());
    return Array.from({ length: 42 }, (_, i) => { const d = new Date(inicio); d.setDate(inicio.getDate() + i); return d; });
  }, [ref]);
  const de = iso(dias[0]);
  const ate = iso(dias[41]);
  const { dados: eventos, erro, carregando, recarregar } = useCarregar(() => api.get(`/api/agenda?de=${de}&ate=${ate}`), [de, ate]);

  const porDia = useMemo(() => {
    const m = {};
    (eventos || []).forEach((e) => { (m[e.data] ||= []).push(e); });
    return m;
  }, [eventos]);

  function mover(delta) {
    const d = new Date(ref.ano, ref.mes + delta, 1);
    setRef({ ano: d.getFullYear(), mes: d.getMonth() });
    setSelecionado(null);
  }

  const hojeTxt = hojeIso();
  const proximos = (eventos || []).filter((e) => e.data >= hojeTxt && e.data <= iso(new Date(Date.now() + 7 * 864e5)));
  const lista = selecionado ? porDia[selecionado] || [] : proximos;

  async function abrirCompromisso(id) {
    try { setForm(await api.get(`/api/compromissos/${id}`)); } catch (e) { toast(e.message, 'erro'); }
  }

  return (
    <>
      <Topo titulo="Agenda">
        <button type="button" className="btn btn-terra" onClick={() => setForm({ inicio: `${selecionado || hojeTxt}T09:00` })}>
          <Icone nome="mais" tamanho={18} largura={3} />Novo compromisso
        </button>
      </Topo>

      <div className="filtros" style={{ justifyContent: 'space-between' }}>
        <div className="chips" style={{ alignItems: 'center' }}>
          <button type="button" className="chip" onClick={() => mover(-1)} aria-label="Mês anterior"><Icone nome="seta" tamanho={18} style={{ transform: 'rotate(180deg)' }} /></button>
          <span className="forte" style={{ padding: '0 8px', minWidth: 150, textAlign: 'center' }} aria-live="polite">{MESES[ref.mes]} {ref.ano}</span>
          <button type="button" className="chip" onClick={() => mover(1)} aria-label="Próximo mês"><Icone nome="seta" tamanho={18} /></button>
        </div>
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', alignItems: 'center' }}>
          <div className="legenda-linha" style={{ margin: 0 }} aria-hidden="true">
            {Object.entries(ROTULO_TIPO).filter(([k]) => k !== 'CONTA' || !sessao.ajudante()).map(([k, r]) => <span key={k}><i style={{ background: COR_PONTO[k] }} />{r}</span>)}
          </div>
          <button type="button" className="btn btn-claro btn-p" onClick={() => { setRef({ ano: hoje.getFullYear(), mes: hoje.getMonth() }); setSelecionado(hojeTxt); }}>Hoje</button>
        </div>
      </div>

      <Estado carregando={carregando && !eventos} erro={erro}>
        <section className="cartao" style={{ padding: 14 }} aria-label={`Calendário de ${MESES[ref.mes]}`}>
          <div className="calendario">
            {SEMANA.map((s) => <div key={s} className="dia-semana" aria-hidden="true">{s}</div>)}
            {dias.map((d) => {
              const k = iso(d);
              const evs = porDia[k] || [];
              const fora = d.getMonth() !== ref.mes;
              return (
                <button key={k} type="button" onClick={() => setSelecionado(selecionado === k ? null : k)}
                  className={`dia ${fora ? 'fora' : ''} ${k === hojeTxt ? 'hoje' : ''} ${selecionado === k ? 'selecionado' : ''}`}
                  aria-pressed={selecionado === k}
                  aria-label={`${d.getDate()} de ${MESES[d.getMonth()]}${evs.length ? `, ${evs.length} ${evs.length === 1 ? 'evento' : 'eventos'}` : ''}`}>
                  <span className="num-dia">{d.getDate()}</span>
                  {evs.slice(0, 3).map((e) => (
                    <span key={`${e.tipo}${e.id}`} className={`evento ev-${e.tipo} ${e.concluido ? 'ev-concluido' : ''}`}>
                      {e.hora ? `${e.hora.slice(0, 5)} ` : ''}{e.titulo.replace(/^(Entrega|Aniversário|Vence): /, '')}
                    </span>
                  ))}
                  {evs.length > 3 && <span className="mais">+{evs.length - 3}</span>}
                  {evs.length > 0 && <span className="pontos" aria-hidden="true">{evs.slice(0, 4).map((e) => <i key={`${e.tipo}${e.id}`} style={{ background: COR_PONTO[e.tipo] }} />)}</span>}
                </button>
              );
            })}
          </div>
        </section>

        <section className="cartao" aria-labelledby="t-lista-ag">
          <div className="cartao-titulo">
            <h2 id="t-lista-ag">{selecionado ? new Date(`${selecionado}T12:00`).toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: 'long' }) : 'Próximos 7 dias'}</h2>
            {selecionado && <button type="button" className="btn btn-claro btn-p" onClick={() => setForm({ inicio: `${selecionado}T09:00` })}><Icone nome="mais" tamanho={16} largura={3} />Compromisso neste dia</button>}
          </div>
          {lista.length === 0 ? <Vazio>{selecionado ? 'Nada marcado neste dia.' : 'Nada nos próximos 7 dias.'}</Vazio> : (
            <ul className="lista-linhas lista-agenda">
              {lista.map((e) => <LinhaEvento key={`${e.tipo}${e.id}${e.data}`} e={e} mostrarData={!selecionado} aoAbrir={() => abrirCompromisso(e.id)} />)}
            </ul>
          )}
        </section>
      </Estado>

      {form && <FormCompromisso compromisso={form} aoFechar={() => setForm(null)} aoSalvar={() => { setForm(null); recarregar(); }} />}
    </>
  );
}

function LinhaEvento({ e, mostrarData, aoAbrir }) {
  const dataTxt = new Date(`${e.data}T12:00`).toLocaleDateString('pt-BR', { weekday: 'short', day: '2-digit', month: '2-digit' }).replace('.', '');
  const conteudo = (
    <div className="info">
      <p className={e.concluido ? 'ev-concluido' : ''}>{e.titulo}</p>
      <p>
        {ROTULO_TIPO[e.tipo]}
        {e.tipo === 'COMPROMISSO' && TIPOS_COMPROMISSO[e.status] ? ` · ${TIPOS_COMPROMISSO[e.status]}` : ''}
        {e.tipo === 'ENTREGA' && STATUS_ENCOMENDA[e.status] ? ` · ${STATUS_ENCOMENDA[e.status][0]}` : ''}
        {e.tipo === 'CONTA' ? (e.concluido ? ' · paga' : ' · a pagar') : ''}
        {e.detalhe ? ` · ${e.detalhe}` : ''}
      </p>
    </div>
  );
  const destino = { ENTREGA: `/admin/encomendas?abrir=${e.id}`,
    ANIVERSARIO: sessao.ajudante() ? null : '/admin/lembretes', CONTA: `/admin/financeiro/contas` }[e.tipo];
  return (
    <li>
      <span className="marca-tipo" style={{ background: COR_PONTO[e.tipo] }} aria-hidden="true" />
      <span className="hora">{mostrarData ? dataTxt : e.hora ? e.hora.slice(0, 5) : 'dia todo'}{mostrarData && e.hora ? <><br /><small className="mudo">{e.hora.slice(0, 5)}</small></> : null}</span>
      {conteudo}
      {e.tipo === 'COMPROMISSO'
        ? <button type="button" className="btn btn-claro btn-p" onClick={aoAbrir}>Abrir</button>
        : destino && <Link to={destino} className="btn btn-claro btn-p">Ver</Link>}
    </li>
  );
}

/** Formulário de compromisso (usado também na encomenda, para agendar a produção ou a entrega). */
export function FormCompromisso({ compromisso: c, aoFechar, aoSalvar }) {
  const clientes = useCarregar(() => api.get('/api/clientes'));
  const encomendas = useCarregar(() => api.get('/api/encomendas'));
  const [excluir, setExcluir] = useState(false);
  const toast = useToast();

  if (excluir) {
    return <Confirmar titulo="Excluir compromisso" perigo textoBotao="Excluir" texto={`Excluir "${c.titulo}" da agenda?`}
      aoFechar={() => setExcluir(false)}
      aoConfirmar={async () => { await api.del(`/api/compromissos/${c.id}`); toast('Compromisso excluído.'); aoSalvar(); }} />;
  }
  if (!clientes.dados || !encomendas.dados) return null;
  const ativas = encomendas.dados.filter((e) => e.status !== 'CANCELADA' && e.status !== 'ENTREGUE' || e.id === c.encomendaId);

  return (
    <ModalForm titulo={c.id ? 'Compromisso' : 'Novo compromisso'} aoFechar={aoFechar} aoSalvar={async (fd) => {
      const d = lerForm(fd, ['clienteId', 'encomendaId']);
      d.concluido = fd.get('concluido') === 'on';
      if (c.id) await api.put(`/api/compromissos/${c.id}`, d);
      else await api.post('/api/compromissos', d);
      toast('Compromisso salvo na agenda.');
      aoSalvar();
    }}>
      <Campo rotulo="O que é" nome="titulo" defaultValue={c.titulo} required maxLength={160} inteiro placeholder="Entregar os kits na escola" />
      <Campo rotulo="Tipo" nome="tipo" opcoes={TIPOS_COMPROMISSO} defaultValue={c.tipo || 'REUNIAO'} />
      <Campo rotulo="Local" nome="local" defaultValue={c.local} maxLength={160} placeholder="Endereço ou nome do lugar" />
      <Campo rotulo="Início" nome="inicio" tipo="datetime-local" defaultValue={c.inicio?.slice(0, 16)} required />
      <Campo rotulo="Término" nome="fim" tipo="datetime-local" defaultValue={c.fim?.slice(0, 16)} />
      <Campo rotulo="Cliente" nome="clienteId" defaultValue={c.clienteId ?? ''}
        opcoes={[['', 'Nenhuma'], ...clientes.dados.map((x) => [x.id, x.nome])]} />
      <Campo rotulo="Encomenda" nome="encomendaId" defaultValue={c.encomendaId ?? ''}
        opcoes={[['', 'Nenhuma'], ...ativas.map((x) => [x.id, `${x.descricao} (${x.clienteNome})`])]} />
      <Campo rotulo="Observações" nome="observacoes" linhas={2} defaultValue={c.observacoes} inteiro />
      {c.id && <label className="checkbox"><input type="checkbox" name="concluido" defaultChecked={c.concluido} /> Já foi feito</label>}
      {c.id && <button type="button" className="btn btn-perigo btn-p" style={{ justifySelf: 'end', alignSelf: 'center' }} onClick={() => setExcluir(true)}><Icone nome="lixo" tamanho={16} />Excluir</button>}
    </ModalForm>
  );
}
