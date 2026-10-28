import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import Marca, { Mascote, SemImagem } from '../componentes/Marca.jsx';
import { CATEGORIAS_PORTFOLIO, moeda, telefone, whatsAtelie } from '../util.js';
import { CATEGORIAS, CartaoPeca, DetalhePeca, daCategoria } from './Catalogo.jsx';
import Orcamento from './Orcamento.jsx';
import { sacola, useSacola } from './sacola.js';

const SLIDES = [
  { lema: 'papelaria criativa & presentes únicos', titulo: ['Presentes que', 'contam a sua história'],
    texto: 'Papelaria • Canecas • Kits • Lembrancinhas', botao: 'Ver as peças', destino: '#vitrines', pose: 'acenando' },
  { lema: 'datas especiais', titulo: ['Kits para', 'quem você ama'],
    texto: 'Dia dos Professores • Dia das Mães • Namorados • Natal', botao: 'Pedir um kit', tipo: 'Kit ou cesta de presente', pose: 'bebe' },
  { lema: 'escolas e empresas', titulo: ['Lembrancinhas', 'com o nome de cada um'],
    texto: 'Pedidos em quantidade, com a sua arte aprovada antes', botao: 'Pedir orçamento', tipo: 'Lembrancinhas (festa, escola, empresa)', pose: 'notebook' },
];

const PASSOS = [
  ['orcamento', 'Peça o orçamento', 'Escolha as peças ou conte o que imagina e mande fotos de referência.'],
  ['dinheiro', 'Confirme com o sinal', 'Combinado o valor, o sinal garante a sua data na agenda do ateliê.'],
  ['pincel', 'Aprove a arte', 'Você recebe um link com a arte e aprova ou pede ajuste antes da produção.'],
  ['presente', 'Receba com carinho', 'Sua peça é produzida à mão e embalada para presentear.'],
];

const POR_QUE = [
  ['mao', 'Feito à mão', 'Cada peça produzida no ateliê', '#FDE2D3', '#C9440F'],
  ['pincel', 'Você aprova a arte', 'Nada é produzido sem o seu ok', '#EEE7F8', '#6A47A8'],
  ['equipe', 'Pedidos em quantidade', 'Escolas, empresas e festas', '#E2F2E6', '#2F7A4A'],
  ['caminhao', 'Entrega combinada', 'Em Gov. Nunes Freire e região', '#DDEFF5', '#1F6E8C'],
];

/** Loja do ateliê: página inicial (vitrines) ou catálogo completo, com a sacola de orçamento ao lado. */
export default function Loja({ modo = 'inicio' }) {
  const [site, setSite] = useState(null);
  const [erro, setErro] = useState(null);
  const [busca, setBusca] = useState('');
  const [pecaAberta, setPecaAberta] = useState(null);
  const [menuAberto, setMenuAberto] = useState(false);
  const [sacolaAberta, setSacolaAberta] = useState(false);
  const [aviso, setAviso] = useState(null);
  const [tipoOrcamento, setTipoOrcamento] = useState(null);
  const [params, setParams] = useSearchParams();
  const ir = useNavigate();
  const local = useLocation();
  const timerAviso = useRef();

  useEffect(() => {
    document.title = modo === 'catalogo' ? 'Catálogo · Ateliê Jô Monteiro' : 'Ateliê Jô Monteiro — Papelaria criativa e presentes únicos';
    api.get('/api/publico/site').then(setSite).catch((e) => setErro(e.message));
  }, [modo]);

  const atelie = site?.atelie || { nomeAtelie: 'Ateliê Jô Monteiro', instagram: 'jomonteiroatelie' };
  const catalogo = site?.catalogo || [];
  const minhaSacola = useSacola(site ? catalogo : null);
  const categoria = CATEGORIAS.find((c) => c.chave === params.get('cat')) || null;

  // Vindo de um link antigo "/?peca=ID" (ou do detalhe no catálogo): coloca a peça na sacola e abre o pedido
  useEffect(() => {
    const id = Number(params.get('peca'));
    if (!id || !site) return;
    if (catalogo.some((i) => i.id === id)) {
      if (!minhaSacola.tem(id)) sacola.adicionar(id);
      setTimeout(() => irPara('orcamento'), 80);
    }
    params.delete('peca');
    setParams(params, { replace: true });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [site]);

  // Âncoras (#orcamento, #como-funciona…) depois que a página carrega
  useEffect(() => {
    if (site && local.hash) setTimeout(() => document.getElementById(local.hash.slice(1))?.scrollIntoView(), 60);
  }, [site, local.hash]);

  function irPara(id) {
    setMenuAberto(false);
    setSacolaAberta(false);
    if (modo !== 'inicio') { ir(`/#${id}`); return; }
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' });
  }

  function avisar(texto) {
    clearTimeout(timerAviso.current);
    setAviso(texto);
    timerAviso.current = setTimeout(() => setAviso(null), 2600);
  }

  const aoAdicionar = (i) => avisar(`${i.nome} na sacola`);

  /** No computador a sacola fica sempre ao lado (só rola até ela); no celular, abre como gaveta. */
  function abrirSacola() {
    if (window.matchMedia('(min-width: 1201px)').matches) {
      document.getElementById('sacola-titulo')?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    } else {
      setSacolaAberta(true);
    }
  }

  function orcarPeca(i) {
    if (!minhaSacola.tem(i.id)) sacola.adicionar(i.id);
    setPecaAberta(null);
    irPara('orcamento');
  }

  function pedirTipo(tipo) {
    setTipoOrcamento(tipo);
    irPara('orcamento');
  }

  function escolherCategoria(c) {
    setMenuAberto(false);
    if (modo === 'catalogo') setParams(c ? { cat: c.chave } : {});
    else ir(c ? `/catalogo?cat=${c.chave}` : '/catalogo');
    window.scrollTo({ top: 0 });
  }

  const termo = busca.trim().toLowerCase();
  const filtrados = useMemo(() => catalogo.filter((i) => daCategoria(categoria, i)
    && (!termo || `${i.nome} ${i.texto || ''}`.toLowerCase().includes(termo))), [catalogo, categoria, termo]);

  const props = { naSacola: minhaSacola.tem, aoAbrir: setPecaAberta, aoAdicionar };
  const prontas = catalogo.filter((i) => i.prontaEntrega && i.disponiveis > 0);
  const maisPedidos = (site?.maisPedidos || []).map((id) => catalogo.find((i) => i.id === id)).filter(Boolean);
  const presentes = catalogo.filter((i) => i.frente === 'PRESENTE');
  const mostrarLista = modo === 'catalogo' || termo;

  return (
    <div className="loja">
      {menuAberto && <div className="loja-fundo" onClick={() => setMenuAberto(false)} />}
      <aside className={`loja-menu rola-discreto ${menuAberto ? 'aberto' : ''}`} aria-label="Menu da loja">
        <Link to="/" className="loja-logo" onClick={() => setMenuAberto(false)}><Marca variante="horizontal" /></Link>
        <nav>
          <Link to="/" className={modo === 'inicio' && !termo ? 'ativo' : ''} onClick={() => { setMenuAberto(false); setBusca(''); }}>
            <Icone nome="painel" tamanho={20} />Início</Link>
          <Link to="/catalogo" className={modo === 'catalogo' && !categoria ? 'ativo' : ''} onClick={() => setMenuAberto(false)}>
            <Icone nome="etiqueta" tamanho={20} />Todas as peças</Link>
          {CATEGORIAS.map((c) => (
            <button key={c.chave} type="button" className={categoria?.chave === c.chave ? 'ativo' : ''} onClick={() => escolherCategoria(c)}>
              <Icone nome={c.icone} tamanho={20} />{c.rotulo}
            </button>
          ))}
          <span className="loja-menu-divisor" />
          <button type="button" onClick={() => irPara('como-funciona')}><Icone nome="lampada" tamanho={20} />Como funciona</button>
          <button type="button" onClick={() => irPara('inspiracao')}><Icone nome="portfolio" tamanho={20} />Trabalhos</button>
          <button type="button" onClick={() => irPara('sobre')}><Icone nome="cliente" tamanho={20} />Quem sou</button>
          <button type="button" onClick={() => irPara('orcamento')}><Icone nome="orcamento" tamanho={20} />Pedir orçamento</button>
        </nav>
        <div className="loja-promo">
          <p>Pequenos<br />detalhes,</p>
          <p className="cursiva">grandes afetos</p>
          <Mascote pose="raposa" />
        </div>
        <div className="loja-menu-rodape">
          {atelie.instagram && <a href={`https://www.instagram.com/${atelie.instagram}/`} target="_blank" rel="noopener noreferrer"><Icone nome="instagram" tamanho={20} />@{atelie.instagram}</a>}
          <a href={whatsAtelie(atelie.whatsapp)} target="_blank" rel="noopener noreferrer"><Icone nome="whats" tamanho={20} />Falar no WhatsApp</a>
        </div>
        <p className="loja-rabisco cursiva">Bons presentes<br />começam aqui!</p>
      </aside>

      <div className="loja-corpo">
        <header className="loja-topo">
          <button type="button" className="btn btn-claro btn-icone loja-so-movel" aria-label="Abrir menu" onClick={() => setMenuAberto(true)}><Icone nome="menu" /></button>
          <Link to="/" className="loja-logo-movel"><Marca variante="horizontal" /></Link>
          <label className="loja-busca">
            <Icone nome="busca" tamanho={20} />
            <span className="sr">Buscar peças</span>
            <input type="search" placeholder="Buscar cadernetas, canecas, kits…" value={busca} onChange={(e) => setBusca(e.target.value)} />
          </label>
          <a className="loja-contato" href={whatsAtelie(atelie.whatsapp, 'Olá, Jô! Vim pelo site.')} target="_blank" rel="noopener noreferrer">
            <Marca variante="simbolo" decorativa />
            <span><strong>Olá! Eu sou a Jô</strong><small>Fale comigo no WhatsApp</small></span>
          </a>
          <button type="button" className="loja-botao-sacola" onClick={abrirSacola} aria-label={`Abrir sacola, ${minhaSacola.quantidade} ${minhaSacola.quantidade === 1 ? 'peça' : 'peças'}`}>
            <Icone nome="sacola" />
            {minhaSacola.quantidade > 0 && <span className="contador">{minhaSacola.quantidade}</span>}
          </button>
        </header>

        <main className="loja-centro">
          {erro && <div className="aviso aviso-erro" role="alert">Não foi possível carregar a loja agora. Tente de novo em instantes.</div>}

          {mostrarLista ? (
            <section className="loja-secao" aria-labelledby="lista-titulo">
              <p className="sobretitulo">{termo ? 'busca' : 'catálogo'}</p>
              <h1 id="lista-titulo" className="titulo-secao">{termo ? `Resultados para “${busca.trim()}”` : categoria ? categoria.rotulo : 'Todas as peças'}</h1>
              <div className="chips chips-p loja-chips" role="group" aria-label="Categorias">
                <button type="button" className="chip" aria-pressed={!categoria} onClick={() => escolherCategoria(null)}>Todas</button>
                {CATEGORIAS.map((c) => <button key={c.chave} type="button" className="chip" aria-pressed={categoria?.chave === c.chave} onClick={() => escolherCategoria(c)}>{c.rotulo}</button>)}
              </div>
              {!site && !erro && <p className="carregando">Carregando…</p>}
              {site && filtrados.length === 0 && (
                <div className="aviso aviso-info">
                  Nenhuma peça encontrada{termo ? ' com essa busca' : ' nesta categoria'}. A Jô também cria peças sob medida:{' '}
                  <button type="button" className="link-botao" onClick={() => pedirTipo('Outro')}>peça um orçamento</button>.
                </div>
              )}
              <div className="produtos-grade">
                {filtrados.map((i) => <CartaoPeca key={i.id} i={i} {...props} naSacola={minhaSacola.tem(i.id)} />)}
              </div>
            </section>
          ) : (
            <>
              <Banner aoPedir={pedirTipo} aoVer={() => irPara('vitrines')} />
              <nav className="categorias-circulos" aria-label="Categorias">
                {CATEGORIAS.map((c) => (
                  <button key={c.chave} type="button" onClick={() => escolherCategoria(c)}>
                    <span className="circulo" style={{ background: c.fundo, color: c.cor }}><Icone nome={c.icone} tamanho={34} largura={1.8} /></span>
                    <span>{c.rotulo}</span>
                  </button>
                ))}
              </nav>

              <div id="vitrines" style={{ height: 0, marginTop: -28, scrollMarginTop: 16 }} />
              {prontas.length > 0 && (
                <Vitrine destaque titulo="Pronta-entrega" icone="raio" subtitulo="Peças prontas para levar hoje"
                  verTodos={() => escolherCategoria(CATEGORIAS.find((c) => c.pronta))} itens={prontas} props={props} sacolaTem={minhaSacola.tem} />
              )}
              {catalogo.length > 0 && (
                <Vitrine titulo="Destaques do ateliê" verTodos={() => escolherCategoria(null)} itens={catalogo} props={props} sacolaTem={minhaSacola.tem} />
              )}
              {maisPedidos.length > 0 && (
                <Vitrine titulo="Mais pedidos" icone="coroa" corIcone="var(--ocre)" itens={maisPedidos} selo="Mais pedido" props={props} sacolaTem={minhaSacola.tem} />
              )}
              {presentes.length > 0 && (
                <Vitrine titulo="Para presentear" icone="coracao" corIcone="var(--terra-vivo)" subtitulo="Kits e cestas para datas especiais"
                  verTodos={() => escolherCategoria(CATEGORIAS.find((c) => c.chave === 'kits'))} itens={presentes} props={props} sacolaTem={minhaSacola.tem} />
              )}
              {site && catalogo.length === 0 && (
                <div className="aviso aviso-info">As peças do ateliê chegam aqui em breve. Enquanto isso, peça seu orçamento abaixo.</div>
              )}

              <Inspiracao fotos={site?.portfolio || []} aoPedir={pedirTipo} instagram={atelie.instagram} />
              <ComoFunciona />
              {site?.depoimentos?.length > 0 && <Depoimentos lista={site.depoimentos} />}
            </>
          )}

          <Beneficios />
          <Orcamento whatsapp={atelie.whatsapp} itens={minhaSacola.itens} tipoInicial={tipoOrcamento} />

          {modo === 'inicio' && !termo && (
            <section id="sobre" className="loja-secao sobre-loja">
              <div className="sobre-foto"><Mascote pose="garrafa-raposa" /></div>
              <div style={{ flex: '1 1 340px', minWidth: 0 }}>
                <p className="sobretitulo">quem sou</p>
                <h2 className="titulo-secao" style={{ marginBottom: 12 }}>Prazer, Jô Monteiro!</h2>
                <p style={{ color: 'var(--marrom-2)', fontWeight: 600, whiteSpace: 'pre-line' }}>{atelie.sobreTexto}</p>
              </div>
            </section>
          )}

          <section className="comunidade" aria-labelledby="com-titulo">
            <Icone nome="instagram" tamanho={44} cor="#fff" />
            <div style={{ flex: '1 1 260px', minWidth: 0 }}>
              <h2 id="com-titulo">Faça parte da nossa comunidade</h2>
              <p>Novidades, bastidores do ateliê e as datas especiais do ano, primeiro no Instagram.</p>
            </div>
            <a className="btn btn-branco" href={`https://www.instagram.com/${atelie.instagram || 'jomonteiroatelie'}/`} target="_blank" rel="noopener noreferrer">
              Seguir @{atelie.instagram || 'jomonteiroatelie'}
            </a>
            <span className="cursiva comunidade-rabisco" aria-hidden="true">Escreva, crie, presenteie</span>
          </section>

          <footer className="loja-rodape">
            <Marca variante="horizontal" style={{ width: 200 }} />
            <div>
              {atelie.cidade && <p>{atelie.cidade}</p>}
              {atelie.horario && <p>{atelie.horario}</p>}
              {atelie.whatsapp && <p>WhatsApp {telefone(atelie.whatsapp)}</p>}
            </div>
            <p className="mudo">© {new Date().getFullYear()} {atelie.nomeAtelie || 'Ateliê Jô Monteiro'} · Site por Joel Júnior · <Link to="/admin">Área da Jô</Link></p>
          </footer>
        </main>

        {sacolaAberta && <div className="loja-fundo lado" onClick={() => setSacolaAberta(false)} />}
        <aside className={`loja-lado ${sacolaAberta ? 'aberto' : ''}`} aria-label="Sacola e informações">
          <Sacola s={minhaSacola} aoFechar={() => setSacolaAberta(false)} aoPedir={() => irPara('orcamento')} />
          <CombinaCom catalogo={catalogo} sacolaTem={minhaSacola.tem} aoAbrir={setPecaAberta} aoAdicionar={aoAdicionar} />
          <section className="lado-cartao" aria-labelledby="pq-titulo">
            <h2 id="pq-titulo">Por que o Ateliê Jô?</h2>
            <ul className="por-que">
              {POR_QUE.map(([icone, titulo, texto, fundo, cor]) => (
                <li key={titulo}>
                  <span className="bolinha" style={{ background: fundo, color: cor }}><Icone nome={icone} tamanho={22} /></span>
                  <span><strong>{titulo}</strong><small>{texto}</small></span>
                </li>
              ))}
            </ul>
          </section>
          <section className="lado-cartao escolas" aria-labelledby="esc-titulo">
            <p className="escolas-titulo"><Icone nome="coroa" tamanho={28} cor="var(--ocre)" /><span id="esc-titulo">Jô <em>para escolas</em></span></p>
            <p className="forte">Kits e lembrancinhas em quantidade</p>
            <ul className="lista-check">
              <li><Icone nome="check" tamanho={16} largura={3} />Nome de cada aluno ou da empresa</li>
              <li><Icone nome="check" tamanho={16} largura={3} />Arte aprovada antes da produção</li>
              <li><Icone nome="check" tamanho={16} largura={3} />Datas comemorativas do ano todo</li>
              <li><Icone nome="check" tamanho={16} largura={3} />Entrega combinada com você</li>
            </ul>
            <button type="button" className="btn btn-amarelo" onClick={() => pedirTipo('Lembrancinhas (festa, escola, empresa)')}>Pedir orçamento <Icone nome="seta" tamanho={16} /></button>
          </section>
          <p className="loja-rabisco cursiva lado">Mais que papelaria:<br />afeto em cada detalhe</p>
        </aside>
      </div>

      {minhaSacola.quantidade > 0 && !sacolaAberta && (
        <button type="button" className="sacola-flutuante" onClick={() => setSacolaAberta(true)}>
          <Icone nome="sacola" />Sacola · {minhaSacola.quantidade}
        </button>
      )}
      {aviso && (
        <div className="toast" role="status" aria-live="polite">
          {aviso}{' '}<button type="button" className="link-botao" style={{ color: '#FFD9C6' }} onClick={() => { setAviso(null); abrirSacola(); }}>Ver sacola</button>
        </div>
      )}
      {pecaAberta && <DetalhePeca i={pecaAberta} whatsapp={atelie.whatsapp} aoFechar={() => setPecaAberta(null)}
        aoOrcar={() => orcarPeca(pecaAberta)} aoAdicionar={aoAdicionar} />}
    </div>
  );
}

function Banner({ aoPedir, aoVer }) {
  const [n, setN] = useState(0);
  const [pausa, setPausa] = useState(false);
  useEffect(() => {
    if (pausa || window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) return undefined;
    const t = setInterval(() => setN((x) => (x + 1) % SLIDES.length), 6500);
    return () => clearInterval(t);
  }, [pausa]);
  const s = SLIDES[n];
  return (
    <section className="banner estampa" aria-roledescription="carrossel" aria-label="Destaques"
      onMouseEnter={() => setPausa(true)} onMouseLeave={() => setPausa(false)}>
      <div className="banner-texto" aria-live="polite">
        <p className="lema">{s.lema}</p>
        <h1><span>{s.titulo[0]}</span> <span className="cursiva">{s.titulo[1]}</span></h1>
        <p className="banner-sub">{s.texto}</p>
        <button type="button" className="btn btn-preto" onClick={() => (s.tipo ? aoPedir(s.tipo) : aoVer())}>{s.botao}<Icone nome="seta" tamanho={16} /></button>
      </div>
      <div className="banner-arte" aria-hidden="true">
        <span className="banner-circulo" />
        <Mascote key={s.pose} pose={s.pose} />
        <span className="banner-bilhete b1 maquina">amor em<br />papel</span>
        <span className="banner-bilhete b2 cursiva">e porcelana</span>
      </div>
      <div className="banner-pontos" role="group" aria-label="Escolher destaque">
        {SLIDES.map((x, i) => <button key={x.lema} type="button" aria-label={`Destaque ${i + 1}: ${x.lema}`} aria-pressed={i === n} onClick={() => setN(i)} />)}
      </div>
    </section>
  );
}

function Vitrine({ titulo, subtitulo, icone, corIcone, verTodos, itens, props, sacolaTem, destaque, selo }) {
  return (
    <section className={`loja-secao vitrine ${destaque ? 'vitrine-destaque' : ''}`} aria-label={titulo}>
      <div className="vitrine-topo">
        <h2>{icone && <Icone nome={icone} tamanho={26} cor={corIcone || 'var(--terra-vivo)'} largura={2.2} />}{titulo}</h2>
        {subtitulo && <span className="mudo forte">{subtitulo}</span>}
        {verTodos && <button type="button" className="ver-todos" onClick={verTodos}>Ver todas <Icone nome="seta" tamanho={14} /></button>}
      </div>
      <div className="produtos-linha">
        {itens.slice(0, 4).map((i) => <CartaoPeca key={i.id} i={i} {...props} selo={selo} naSacola={sacolaTem(i.id)} />)}
      </div>
    </section>
  );
}

const INSPIRACOES = [
  { titulo: 'Kit Dia dos Professores', texto: 'Caderneta, marcador e mimos', tipo: 'Kit ou cesta de presente', icone: 'caderno', fundo: 'linear-gradient(135deg, #FDE2D3, #FBC7A8)' },
  { titulo: 'Canecas com foto', texto: 'Para a mãe, a avó, a madrinha', tipo: 'Caneca', icone: 'caneca', fundo: 'linear-gradient(135deg, #EEE7F8, #D5C6F0)' },
  { titulo: 'Lembrancinhas de festa', texto: 'Com o nome de cada convidado', tipo: 'Lembrancinhas (festa, escola, empresa)', icone: 'presente', fundo: 'linear-gradient(135deg, #FCEFC6, #F6D98A)' },
];

function Inspiracao({ fotos, aoPedir, instagram }) {
  const usarFotos = fotos.length >= 3;
  return (
    <section id="inspiracao" className="loja-secao" aria-labelledby="insp-titulo">
      <div className="vitrine-topo">
        <h2 id="insp-titulo"><Icone nome="lampada" tamanho={26} cor="var(--ocre)" largura={2.2} />Inspiração</h2>
        <a className="ver-todos" href={`https://www.instagram.com/${instagram || 'jomonteiroatelie'}/`} target="_blank" rel="noopener noreferrer">Mais no Instagram <Icone nome="seta" tamanho={14} /></a>
      </div>
      <div className="inspiracao-grade">
        {usarFotos ? fotos.slice(0, 3).map((f) => (
          <figure key={f.id} className="inspiracao-card">
            <img src={f.url} alt="" loading="lazy" />
            <figcaption><strong>{f.titulo}</strong><span>{CATEGORIAS_PORTFOLIO[f.categoria]}</span></figcaption>
          </figure>
        )) : INSPIRACOES.map((x) => (
          <button key={x.titulo} type="button" className="inspiracao-card ideia" style={{ background: x.fundo }} onClick={() => aoPedir(x.tipo)}>
            <Icone nome={x.icone} tamanho={64} largura={1.4} cor="rgba(35, 31, 32, .55)" />
            <span className="legenda"><strong>{x.titulo}</strong><span>{x.texto}</span></span>
          </button>
        ))}
      </div>
    </section>
  );
}

function ComoFunciona() {
  return (
    <section id="como-funciona" className="loja-secao como-funciona" aria-labelledby="cf-titulo">
      <p className="sobretitulo" style={{ color: 'var(--ocre)' }}>como funciona</p>
      <h2 id="cf-titulo" className="titulo-secao">Do pedido ao presente, sem surpresa</h2>
      <ol className="passos-site">
        {PASSOS.map(([icone, titulo, desc], n) => (
          <li key={titulo}>
            <span className="passo-num" aria-hidden="true">{n + 1}</span>
            <Icone nome={icone} tamanho={30} cor="var(--terra-vivo)" />
            <h3>{titulo}</h3>
            <p>{desc}</p>
          </li>
        ))}
      </ol>
    </section>
  );
}

function Depoimentos({ lista }) {
  return (
    <section id="depoimentos" className="loja-secao" aria-labelledby="depo-titulo">
      <div className="vitrine-topo"><h2 id="depo-titulo"><Icone nome="aspas" tamanho={26} cor="var(--oliva)" />Quem já levou um presente do ateliê</h2></div>
      <div className="depo-site">
        {lista.map((d) => (
          <figure key={d.id} className="depo-card">
            <span className="estrelas" role="img" aria-label={`${d.estrelas} de 5 estrelas`}>
              {[1, 2, 3, 4, 5].map((i) => <Icone key={i} nome="estrela" tamanho={18} className={i > d.estrelas ? 'apagada' : ''} style={{ fill: i <= d.estrelas ? 'currentColor' : 'none' }} />)}
            </span>
            <blockquote>{d.texto}</blockquote>
            <figcaption className="depo-autor">
              {d.foto ? <img className="foto" src={d.foto} alt="" loading="lazy" /> : <span className="foto" aria-hidden="true">{d.nome.charAt(0)}</span>}
              <div><p className="forte">{d.nome}</p>{d.contexto && <p>{d.contexto}</p>}</div>
            </figcaption>
          </figure>
        ))}
      </div>
    </section>
  );
}

function Beneficios() {
  return (
    <ul className="beneficios" aria-label="Vantagens">
      <li><Icone nome="mao" tamanho={26} /><span><strong>Feito à mão</strong><small>Peça a peça, no ateliê</small></span></li>
      <li><Icone nome="pincel" tamanho={26} /><span><strong>Arte aprovada</strong><small>Você confere antes</small></span></li>
      <li><Icone nome="pix" tamanho={26} /><span><strong>Pix ou cartão</strong><small>Sinal e saldo na entrega</small></span></li>
      <li><Icone nome="presente" tamanho={26} /><span><strong>Pronto para presentear</strong><small>Embalagem com carinho</small></span></li>
    </ul>
  );
}

function Sacola({ s, aoFechar, aoPedir }) {
  return (
    <section className="lado-cartao sacola-cartao" aria-labelledby="sacola-titulo">
      <div className="sacola-topo">
        <h2 id="sacola-titulo"><Icone nome="sacola" tamanho={22} />Sua sacola ({s.quantidade})</h2>
        <button type="button" className="btn btn-fantasma btn-icone btn-p loja-so-gaveta" aria-label="Fechar sacola" onClick={aoFechar}><Icone nome="fechar" tamanho={18} /></button>
      </div>
      {s.itens.length === 0 ? (
        <div className="sacola-vazia">
          <Mascote pose="pensando" />
          <p>Sua sacola está vazia.</p>
          <small>Toque na sacolinha das peças para juntar tudo num pedido só.</small>
        </div>
      ) : (
        <>
          <ul className="sacola-itens">
            {s.itens.map((i) => (
              <li key={i.id}>
                <span className="mini">{i.peca.fotos[0] ? <img src={i.peca.fotos[0]} alt="" /> : <SemImagem />}</span>
                <div className="info">
                  <p className="nome">{i.peca.nome}</p>
                  <p className="preco">{i.peca.preco != null ? moeda(i.peca.preco) : 'Sob consulta'}</p>
                  <div className="contador-qtd">
                    <button type="button" className="qtd-botao" aria-label={`Tirar uma unidade de ${i.peca.nome}`} onClick={() => sacola.mudar(i.id, i.quantidade - 1)}><Icone nome="menos" tamanho={14} largura={2.6} /></button>
                    <span aria-label="Quantidade">{i.quantidade}</span>
                    <button type="button" className="qtd-botao" aria-label={`Colocar mais uma unidade de ${i.peca.nome}`} onClick={() => sacola.mudar(i.id, i.quantidade + 1)}><Icone nome="mais" tamanho={14} largura={2.6} /></button>
                  </div>
                </div>
                <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Tirar ${i.peca.nome} da sacola`} onClick={() => sacola.tirar(i.id)}><Icone nome="lixo" tamanho={18} /></button>
              </li>
            ))}
          </ul>
          <div className="sacola-total">
            <span>Estimativa</span>
            <strong>{moeda(s.total)}{s.semPreco ? ' +' : ''}</strong>
          </div>
          <p className="mudo" style={{ fontSize: 12.5, fontWeight: 600 }}>O valor final vem no orçamento, com a personalização e a quantidade.</p>
          <button type="button" className="btn btn-terra" onClick={aoPedir}>Pedir orçamento <Icone nome="seta" tamanho={16} /></button>
          <button type="button" className="btn btn-claro" onClick={() => sacola.limpar()}>Esvaziar sacola</button>
        </>
      )}
    </section>
  );
}

function CombinaCom({ catalogo, sacolaTem, aoAbrir, aoAdicionar }) {
  const lista = catalogo.filter((i) => !sacolaTem(i.id)).slice(0, 4);
  if (lista.length === 0) return null;
  return (
    <section className="lado-cartao" aria-labelledby="comb-titulo">
      <h2 id="comb-titulo">Combina com você</h2>
      <ul className="combina">
        {lista.map((i) => (
          <li key={i.id}>
            <button type="button" className="mini" onClick={() => aoAbrir(i)} aria-label={`Ver ${i.nome}`}>{i.fotos[0] ? <img src={i.fotos[0]} alt="" /> : <SemImagem />}</button>
            <button type="button" className="info" onClick={() => aoAbrir(i)}>
              <span className="nome">{i.nome}</span>
              <span className="preco">{i.preco != null ? moeda(i.preco) : 'Sob consulta'}</span>
            </button>
            <button type="button" className="mais-botao" aria-label={`Colocar ${i.nome} na sacola`} onClick={() => { sacola.adicionar(i.id); aoAdicionar(i); }}>
              <Icone nome="mais" tamanho={18} largura={2.6} />
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}
