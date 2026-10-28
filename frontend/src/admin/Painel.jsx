import { Link } from 'react-router-dom';
import { api, sessao } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import Marca from '../componentes/Marca.jsx';
import { Estado, Selo, useCarregar, Vazio } from '../componentes/ui.jsx';
import { CORES_GRAFICO, STATUS_ARTE, STATUS_ENCOMENDA, dataCurta, moeda, moedaCurta, primeiroNome, qtdUn } from '../util.js';
import { Topo } from './Layout.jsx';

export default function Painel() {
  if (sessao.ajudante()) return <PainelAjudante />;
  return <PainelAdmin />;
}

function PainelAdmin() {
  const { dados: p, erro, carregando } = useCarregar(() => api.get('/api/painel'));

  return (
    <>
      <Topo titulo="Painel">
        <Link to="/admin/orcamentos?novo=1" className="btn btn-terra"><Icone nome="mais" tamanho={18} largura={3} />Novo orçamento</Link>
      </Topo>
      <Estado carregando={carregando} erro={erro}>
        {p && <Conteudo p={p} />}
      </Estado>
    </>
  );
}

function Conteudo({ p }) {
  const frases = [];
  if (p.orcamentosNovos) frases.push(<strong key="o">{p.orcamentosNovos} {p.orcamentosNovos === 1 ? 'orçamento novo' : 'orçamentos novos'}</strong>);
  if (p.entregasSemana) frases.push(<strong key="e">{p.entregasSemana} {p.entregasSemana === 1 ? 'entrega' : 'entregas'} nos próximos 7 dias</strong>);
  if (p.ajustesPedidos) frases.push(<strong key="a">{p.ajustesPedidos} {p.ajustesPedidos === 1 ? 'ajuste de arte pedido' : 'ajustes de arte pedidos'}</strong>);
  const maxSemana = Math.max(1, ...p.recebidoPorSemana.map((b) => Number(b.valor)));
  const topo = Math.ceil(maxSemana / 500) * 500;

  let acumulado = 0;
  const fatias = p.receitaPorFrente.map((f, i) => {
    const de = acumulado;
    acumulado += f.percentual;
    return `${CORES_GRAFICO[i % CORES_GRAFICO.length]} ${de}% ${acumulado}%`;
  });

  return (
    <>
      {p.lembretes > 0 && (
        <Link to="/admin/lembretes" className="clay" style={{ background: 'var(--ocre-claro)', borderRadius: 28, padding: '16px 22px', display: 'flex', alignItems: 'center', gap: 14, textDecoration: 'none', color: 'var(--marrom)' }}>
          <div className="icone-caixa" style={{ background: 'var(--ocre)' }}><Icone nome="sino" cor="#FFFDF8" tamanho={26} /></div>
          <div style={{ flex: 1 }}>
            <p style={{ fontWeight: 900, fontSize: 18 }}>{p.lembretes} {p.lembretes === 1 ? 'lembrete de WhatsApp' : 'lembretes de WhatsApp'} para hoje</p>
            <p style={{ fontWeight: 700, color: 'var(--ocre-texto)' }}>Artes para aprovar, peças prontas, saldos e aniversários: as mensagens já estão escritas.</p>
          </div>
          <Icone nome="seta" />
        </Link>
      )}
      {p.contasAPagar.length > 0 && <ContasAPagar contas={p.contasAPagar} />}
      <section className="boas-vindas estampa" aria-label="Boas-vindas">
        <div style={{ flex: '1 1 300px', minWidth: 0 }}>
          <p style={{ fontSize: 30, fontWeight: 900, color: 'var(--terra-escuro)' }}>{p.saudacao}, {primeiroNome(sessao.nome()) || 'Jô'}!</p>
          <p style={{ marginTop: 6, fontSize: 18, color: 'var(--marrom-2)' }}>
            {frases.length === 0 ? 'Tudo em dia por aqui. Bom trabalho!'
              : <>Você tem {frases.reduce((a, f, i) => (i === 0 ? [f] : [...a, ' e ', f]), [])}.</>}
          </p>
        </div>
        <Marca variante="simbolo" decorativa style={{ width: 120, flex: '0 0 auto' }} />
      </section>

      <section className="grade-resumo" aria-label="Resumo">
        <Resumo para="/admin/orcamentos" fundo="var(--areia)" caixa="var(--cafe)" icone="orcamento" rotulo="Orçamentos abertos"
          valor={p.orcamentosAbertos} extra={`${p.orcamentosNovos} aguardando resposta`} corExtra="var(--marrom-2)" />
        <Resumo para="/admin/encomendas" fundo="var(--terra-claro)" caixa="var(--terra-vivo)" icone="encomenda" rotulo="Em produção"
          valor={p.emProducao} extra={`${p.entregasSemana} entregas em 7 dias`} corExtra="var(--terra-texto)" />
        <Resumo para="/admin/financeiro" fundo="var(--ocre-claro)" caixa="var(--ocre)" icone="dinheiro" rotulo="A receber"
          valor={moeda(p.aReceber)} extra={`saldo de ${p.contasAReceber} ${p.contasAReceber === 1 ? 'encomenda' : 'encomendas'}`} corExtra="var(--ocre-texto)" />
        <Resumo para="/admin/encomendas?arte=PENDENTES" fundo="var(--oliva-claro)" caixa="var(--oliva)" icone="pincel" rotulo="Artes a fazer"
          valor={p.artesAFazer} extra={`${p.artesComCliente} com a cliente para aprovar`} corExtra="var(--oliva-texto)" />
      </section>

      <div className="grade-2">
        <section className="cartao" aria-labelledby="t-sem">
          <div className="cartao-titulo">
            <h2 id="t-sem">Recebido por semana</h2>
            <span className="selo selo-areia">{p.mesReferencia}</span>
          </div>
          <div className="barras" role="img" aria-label={`Recebido por semana em ${p.mesReferencia}: ${p.recebidoPorSemana.map((b) => `${b.rotulo} ${moeda(b.valor)}`).join(', ')}`}>
            <div className="barras-eixo"><span>{moedaCurta(topo)}</span><span>{moedaCurta(topo / 2)}</span><span>0</span></div>
            <div className="barras-area">
              {p.recebidoPorSemana.map((b, i) => (
                <div key={b.rotulo} className="barra-col" title={moeda(b.valor)}>
                  <div className="barra" style={{ height: `${(Number(b.valor) / topo) * 150}px`, background: CORES_GRAFICO[i] }} />
                  <span>{b.rotulo}</span>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="cartao" aria-labelledby="t-frente">
          <div className="cartao-titulo"><h2 id="t-frente">Receita por frente</h2><span className="selo selo-areia">{p.mesReferencia}</span></div>
          {p.receitaPorFrente.length === 0 ? <Vazio>Nenhuma entrada registrada neste mês.</Vazio> : (
            <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 28 }}>
              <div className="rosca" role="img" aria-label={p.receitaPorFrente.map((f) => `${f.nome} ${f.percentual}%`).join(', ')}
                style={{ background: `conic-gradient(${fatias.join(', ')})` }}>
                <div className="rosca-miolo" />
              </div>
              <ul className="legenda">
                {p.receitaPorFrente.map((f, i) => (
                  <li key={f.nome}><span className="bolinha" style={{ background: CORES_GRAFICO[i % CORES_GRAFICO.length] }} />
                    <span style={{ flex: 1 }}>{f.nome}</span><span>{f.percentual}%</span></li>
                ))}
              </ul>
            </div>
          )}
        </section>

        <section className="cartao" aria-labelledby="t-ent">
          <div className="cartao-titulo"><h2 id="t-ent">Próximas entregas</h2><Link to="/admin/encomendas" className="btn btn-claro btn-p">Ver todas</Link></div>
          {p.proximasEntregas.length === 0 ? <Vazio>Nenhuma entrega com prazo marcado.</Vazio> : (
            <ul className="lista-linhas">
              {p.proximasEntregas.map((e) => (
                <li key={e.id}>
                  <div className="icone-caixa" style={{ background: 'var(--terra-claro)', width: 46, height: 46 }}><Icone nome="encomenda" cor="var(--terra)" /></div>
                  <div className="info"><p>{e.descricao}</p><p>{e.clienteNome} · {dataCurta(e.prazoEntrega)}</p></div>
                  <Selo mapa={STATUS_ENCOMENDA} valor={e.status} />
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="cartao" aria-labelledby="t-arte" style={{ background: 'var(--oliva-menu)' }}>
          <div className="cartao-titulo"><h2 id="t-arte">Artes</h2><Link to="/admin/encomendas?arte=PENDENTES" className="btn btn-claro btn-p">Abrir</Link></div>
          {p.artesAFazer + p.artesComCliente === 0 ? <Vazio>Nenhuma arte pendente. Tudo aprovado!</Vazio> : (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(0, 1fr))', gap: 10 }}>
              {[[p.artesAFazer - p.ajustesPedidos, 'A criar', 'A_CRIAR'], [p.ajustesPedidos, 'Ajustes pedidos', 'AJUSTE_PEDIDO'], [p.artesComCliente, 'Com a cliente', 'AGUARDANDO_CLIENTE']].map(([n, r, f]) => (
                <Link key={r} to={`/admin/encomendas?arte=${f}`} className="clay-sm" style={{ background: f === 'AJUSTE_PEDIDO' && n > 0 ? 'var(--terra-claro)' : 'var(--creme)', borderRadius: 18, padding: 12, textAlign: 'center', textDecoration: 'none', color: 'var(--marrom)' }}>
                  <p style={{ fontSize: 26, fontWeight: 900 }}>{n}</p><p style={{ fontSize: 13, fontWeight: 700 }}>{r}</p>
                </Link>
              ))}
            </div>
          )}
          <p className="mudo" style={{ marginTop: 14, fontSize: 14, fontWeight: 700 }}>A cliente aprova a arte pelo link e a resposta aparece aqui.</p>
        </section>
      </div>

      {p.materiaisBaixos.length > 0 && (
        <section className="clay" aria-label="Aviso de estoque" style={{ background: 'var(--oliva-medio)', borderRadius: 32, padding: '22px 30px', display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 20 }}>
          <div className="icone-caixa" style={{ background: 'var(--ocre-claro)' }}><Icone nome="alerta" cor="var(--ocre-texto)" tamanho={28} /></div>
          <div style={{ flex: '1 1 260px', minWidth: 0, color: 'var(--oliva-texto)' }}>
            <p style={{ fontSize: 20, fontWeight: 900 }}>
              {p.materiaisBaixos.length === 1 ? `${p.materiaisBaixos[0].nome} está acabando` : `${p.materiaisBaixos.length} materiais estão acabando`}
            </p>
            <p style={{ fontWeight: 700 }}>{p.materiaisBaixos.map((m) => Number(m.quantidade) < 0
              ? `${m.nome}: estoque negativo, registre a compra`
              : `${m.nome}: restam ${qtdUn(m.quantidade, m.unidade)} (mínimo ${qtdUn(m.estoqueMinimo, m.unidade)})`).join(' · ')}</p>
          </div>
          <Link to="/admin/materiais" className="btn btn-oliva">Ver materiais</Link>
        </section>
      )}
    </>
  );
}

function ContasAPagar({ contas }) {
  const vencidas = contas.filter((c) => c.vencida).length;
  const total = contas.reduce((s, c) => s + Number(c.valor), 0);
  return (
    <Link to="/admin/financeiro/contas" className="clay" style={{ background: vencidas ? 'var(--perigo-claro)' : 'var(--areia)', borderRadius: 28, padding: '16px 22px', display: 'flex', alignItems: 'center', gap: 14, textDecoration: 'none', color: 'var(--marrom)' }}>
      <div className="icone-caixa" style={{ background: vencidas ? 'var(--perigo)' : 'var(--cafe)' }}><Icone nome="recibo" cor="#FFFDF8" tamanho={26} /></div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <p style={{ fontWeight: 900, fontSize: 18 }}>
          {vencidas
            ? `${vencidas} ${vencidas === 1 ? 'conta vencida' : 'contas vencidas'}${contas.length > vencidas ? ` e ${contas.length - vencidas} vencendo` : ''}`
            : `${contas.length} ${contas.length === 1 ? 'conta vence' : 'contas vencem'} nos próximos dias`} · {moeda(total)} a pagar
        </p>
        <p style={{ fontWeight: 700, color: vencidas ? 'var(--perigo)' : 'var(--marrom-2)' }}>
          {contas.slice(0, 4).map((c) => `${c.nome} (${c.vencida ? 'venceu' : 'vence'} ${dataCurta(c.vencimento)})`).join(' · ')}{contas.length > 4 ? '…' : ''}
        </p>
      </div>
      <Icone nome="seta" />
    </Link>
  );
}

/** Painel da ajudante: o que está em produção, o que vence, a agenda da semana e o material acabando. Sem valores. */
function PainelAjudante() {
  const enc = useCarregar(() => api.get('/api/encomendas'));
  const mats = useCarregar(() => api.get('/api/materiais'));
  const hoje = new Date();
  const iso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  const ate = new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate() + 7);
  const agenda = useCarregar(() => api.get(`/api/agenda?de=${iso(hoje)}&ate=${iso(ate)}`));
  const ativas = (enc.dados || []).filter((e) => ['AGUARDANDO_SINAL', 'EM_PRODUCAO', 'PRONTA'].includes(e.status))
    .sort((a, b) => (a.prazoEntrega || '9999').localeCompare(b.prazoEntrega || '9999'));
  const baixos = (mats.dados || []).filter((m) => m.abaixoDoMinimo);
  const h = hoje.getHours();

  return (
    <>
      <Topo titulo="Painel" />
      <section className="boas-vindas estampa" aria-label="Boas-vindas">
        <div style={{ flex: '1 1 300px', minWidth: 0 }}>
          <p style={{ fontSize: 30, fontWeight: 900, color: 'var(--terra-escuro)' }}>{h < 12 ? 'Bom dia' : h < 18 ? 'Boa tarde' : 'Boa noite'}, {primeiroNome(sessao.nome())}!</p>
          <p style={{ marginTop: 6, fontSize: 18, color: 'var(--marrom-2)' }}>
            {ativas.filter((e) => e.status === 'EM_PRODUCAO').length} {ativas.filter((e) => e.status === 'EM_PRODUCAO').length === 1 ? 'encomenda' : 'encomendas'} em produção.
          </p>
        </div>
      </section>
      <div className="grade-2">
        <section className="cartao" aria-labelledby="t-prod">
          <div className="cartao-titulo"><h2 id="t-prod">Para produzir</h2><Link to="/admin/encomendas" className="btn btn-claro btn-p">Abrir</Link></div>
          <Estado carregando={enc.carregando} erro={enc.erro}>
            {ativas.length === 0 ? <Vazio>Nenhuma encomenda em produção agora.</Vazio> : (
              <ul className="lista-linhas">
                {ativas.slice(0, 8).map((e) => (
                  <li key={e.id}>
                    <div className="info"><p>{e.descricao}</p><p>{e.clienteNome}{e.prazoEntrega ? ` · entrega ${dataCurta(e.prazoEntrega)}` : ''}</p></div>
                    {e.statusArte !== 'SEM_ARTE' && e.statusArte !== 'APROVADA' ? <Selo mapa={STATUS_ARTE} valor={e.statusArte} /> : <Selo mapa={STATUS_ENCOMENDA} valor={e.status} />}
                  </li>
                ))}
              </ul>
            )}
          </Estado>
        </section>
        <section className="cartao" aria-labelledby="t-sem7">
          <div className="cartao-titulo"><h2 id="t-sem7">Próximos 7 dias</h2><Link to="/admin/agenda" className="btn btn-claro btn-p">Agenda</Link></div>
          <Estado carregando={agenda.carregando} erro={agenda.erro}>
            {agenda.dados?.length === 0 ? <Vazio>Nada marcado na semana.</Vazio> : (
              <ul className="lista-linhas">
                {agenda.dados?.filter((ev) => ev.tipo !== 'ANIVERSARIO').slice(0, 8).map((ev) => (
                  <li key={`${ev.tipo}-${ev.id}-${ev.data}`}>
                    <div className="info"><p>{ev.titulo}</p><p>{dataCurta(ev.data)}{ev.hora ? ` · ${ev.hora.slice(0, 5)}` : ''}{ev.detalhe ? ` · ${ev.detalhe}` : ''}</p></div>
                  </li>
                ))}
              </ul>
            )}
          </Estado>
        </section>
      </div>
      {baixos.length > 0 && (
        <section className="clay" aria-label="Aviso de estoque" style={{ background: 'var(--oliva-medio)', borderRadius: 32, padding: '22px 30px', display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 20 }}>
          <div className="icone-caixa" style={{ background: 'var(--ocre-claro)' }}><Icone nome="alerta" cor="var(--ocre-texto)" tamanho={28} /></div>
          <div style={{ flex: '1 1 260px', minWidth: 0, color: 'var(--oliva-texto)' }}>
            <p style={{ fontSize: 20, fontWeight: 900 }}>Avise a Jô: material acabando</p>
            <p style={{ fontWeight: 700 }}>{baixos.map((m) => `${m.nome}: ${qtdUn(m.quantidade, m.unidade)}`).join(' · ')}</p>
          </div>
        </section>
      )}
    </>
  );
}

function Resumo({ para, fundo, caixa, icone, rotulo, valor, extra, corExtra }) {
  return (
    <Link to={para} className="resumo" style={{ background: fundo }}>
      <div className="icone-caixa" style={{ background: caixa }}><Icone nome={icone} cor="#FFFDF8" tamanho={26} /></div>
      <p className="rotulo">{rotulo}</p>
      <p className="valor">{valor}</p>
      <p className="extra" style={{ color: corExtra }}>{extra}</p>
    </Link>
  );
}
