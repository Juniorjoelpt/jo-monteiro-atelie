import { useEffect, useState } from 'react';
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { api, sessao } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { primeiroNome } from '../util.js';
import Marca from '../componentes/Marca.jsx';

// [caminho, ícone, rótulo, fim exato, ajudante vê]
const MENU = [
  ['/admin', 'painel', 'Painel', true, true],
  ['/admin/orcamentos', 'orcamento', 'Orçamentos'],
  ['/admin/encomendas', 'encomenda', 'Encomendas', false, true],
  ['/admin/produtos', 'etiqueta', 'Produtos', false, true],
  ['/admin/vendas', 'sacola', 'Vendas', false, true],
  ['/admin/agenda', 'calendario', 'Agenda', false, true],
  ['/admin/clientes', 'cliente', 'Clientes'],
  ['/admin/financeiro', 'financeiro', 'Financeiro'],
  ['/admin/relatorios', 'grafico', 'Relatórios'],
  ['/admin/materiais', 'material', 'Materiais', false, true],
  ['/admin/portfolio', 'portfolio', 'Portfólio'],
  ['/admin/configuracoes', 'config', 'Configurações'],
];

export default function Layout() {
  const [aberto, setAberto] = useState(false);
  const [artes, setArtes] = useState({ aFazer: 0, ajustesPedidos: 0, comCliente: 0 });
  const [lembretes, setLembretes] = useState(0);
  const local = useLocation();
  const ir = useNavigate();
  const [nome, setNome] = useState(sessao.nome());
  const [ajudante, setAjudante] = useState(sessao.ajudante());

  // Confere o perfil no servidor (pode ter mudado desde o login)
  useEffect(() => {
    api.get('/api/auth/eu').then((eu) => {
      if (!eu) return;
      const mudou = (eu.perfil === 'AJUDANTE') !== sessao.ajudante();
      sessao.atualizar(eu.nome, eu.perfil);
      setNome(eu.nome);
      setAjudante(eu.perfil === 'AJUDANTE');
      if (mudou) window.location.reload();
    }).catch(() => {});
  }, []);

  useEffect(() => {
    setAberto(false);
    api.get('/api/artes/resumo').then(setArtes).catch(() => {});
    if (ajudante) return;
    api.get('/api/lembretes').then((l) => setLembretes(l.length)).catch(() => {});
  }, [local.pathname, ajudante]);

  function sair() {
    sessao.sair();
    ir('/admin/entrar');
  }

  return (
    <div className="app">
      {aberto && <div className="fundo-menu" onClick={() => setAberto(false)} />}
      <aside className={`lateral ${aberto ? 'aberta' : ''}`} aria-label="Menu do sistema">
        <div className="lateral-perfil">
          <Link to="/admin" aria-label="Painel do ateliê"><Marca variante="empilhada" decorativa style={{ width: 176 }} /></Link>
          <p style={{ fontSize: 18, fontWeight: 800 }}>Olá, {primeiroNome(nome) || 'Jô'}!</p>
          {ajudante && <span className="selo selo-areia" style={{ alignSelf: 'center' }}>Acesso de ajudante</span>}
        </div>
        <nav>
          {MENU.filter((m) => !ajudante || m[4]).map(([para, icone, rotulo, fim]) => (
            <NavLink key={para} to={para} end={fim}>
              <Icone nome={icone} />{rotulo}
              {para === '/admin/encomendas' && artes.aFazer > 0 && <span className="contador" aria-label={`${artes.aFazer} ${artes.aFazer === 1 ? 'arte' : 'artes'} para fazer`}>{artes.aFazer}</span>}
            </NavLink>
          ))}
        </nav>
        {lembretes > 0 && (
          <Link to="/admin/lembretes" className="clay" style={{ background: 'var(--ocre-claro)', borderRadius: 26, padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 12, textDecoration: 'none', color: 'var(--marrom)' }}>
            <Icone nome="sino" tamanho={26} cor="var(--ocre-texto)" />
            <span style={{ flex: 1, fontWeight: 800, lineHeight: 1.25 }}>{lembretes} {lembretes === 1 ? 'lembrete' : 'lembretes'} para enviar hoje</span>
            <Icone nome="seta" tamanho={18} />
          </Link>
        )}
        {artes.ajustesPedidos > 0 && (
          <div className="clay" style={{ background: 'var(--terra-claro)', borderRadius: 30, padding: 20, textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6 }}>
            <Icone nome="pincel" tamanho={34} cor="var(--terra)" />
            <p style={{ fontWeight: 900, fontSize: 18 }}>{artes.ajustesPedidos === 1 ? 'Ajuste pedido' : 'Ajustes pedidos'}</p>
            <p style={{ fontSize: 14, color: 'var(--marrom-2)' }}>{artes.ajustesPedidos === 1 ? 'Uma cliente pediu mudança na arte' : `${artes.ajustesPedidos} clientes pediram mudança na arte`}</p>
            <Link to="/admin/encomendas?arte=AJUSTE_PEDIDO" className="btn btn-terra btn-p" style={{ marginTop: 6 }}>Ver o que mudar</Link>
          </div>
        )}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 4, marginTop: 'auto' }}>
          {ajudante && <Link to="/admin/senha" className="btn btn-fantasma" style={{ justifyContent: 'flex-start' }}><Icone nome="config" />Minha senha</Link>}
          <a href="/" target="_blank" rel="noopener noreferrer" className="btn btn-fantasma" style={{ justifyContent: 'flex-start' }}><Icone nome="site" />Ver o site</a>
          <button type="button" className="btn btn-fantasma" style={{ justifyContent: 'flex-start' }} onClick={sair}><Icone nome="sair" />Sair</button>
        </div>
      </aside>
      <main className="conteudo">
        <button type="button" className="btn btn-claro menu-movel" style={{ alignSelf: 'flex-start' }} onClick={() => setAberto(true)} aria-label="Abrir menu">
          <Icone nome="menu" />Menu
        </button>
        <Outlet />
      </main>
    </div>
  );
}

/** Abas no topo de uma área (ex.: Financeiro › Lançamentos | Contas fixas). itens = [[caminho, rótulo, ícone]] */
export function Abas({ itens }) {
  return (
    <nav className="abas" aria-label="Seções">
      {itens.map(([para, rotulo, icone]) => (
        <NavLink key={para} to={para} end className={({ isActive }) => `aba ${isActive ? 'ativa' : ''}`}>
          {icone && <Icone nome={icone} tamanho={18} />}{rotulo}
        </NavLink>
      ))}
    </nav>
  );
}

/** Título da página com ações à direita. */
export function Topo({ titulo, children }) {
  useEffect(() => { document.title = `${titulo} · Ateliê Jô Monteiro`; }, [titulo]);
  return (
    <div className="topo">
      <h1>{titulo}</h1>
      {children}
    </div>
  );
}
