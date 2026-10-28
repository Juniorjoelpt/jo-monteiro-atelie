import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import Marca from '../componentes/Marca.jsx';
import { reduzirImagem } from '../componentes/imagens.js';
import { CATEGORIAS_PORTFOLIO, TIPOS_PECA, hojeIso, telefone, whatsAtelie } from '../util.js';
import { CartaoPeca, DetalhePeca } from './Catalogo.jsx';

const SERVICOS = [
  { icone: 'caderno', fundo: 'var(--terra-claro)', caixa: 'var(--terra-vivo)', titulo: 'Papelaria personalizada',
    desc: 'Cadernetas, agendas, planners e capas de Bíblia com nome, frase e tema do jeitinho de quem vai usar.' },
  { icone: 'caneca', fundo: 'var(--oliva-claro)', caixa: 'var(--oliva)', titulo: 'Canecas e garrafas',
    desc: 'Porcelana e squeezes com foto, frase ou arte exclusiva, para presentear ou para o seu dia a dia.' },
  { icone: 'presente', fundo: 'var(--ocre-claro)', caixa: 'var(--ocre)', titulo: 'Kits e cestas',
    desc: 'Presentes montados para Dia dos Professores, das Mães, dos Namorados, aniversários e Natal.' },
  { icone: 'equipe', fundo: 'var(--areia)', caixa: 'var(--cafe)', titulo: 'Escolas e empresas',
    desc: 'Lembrancinhas em quantidade, com o nome de cada aluno ou a marca da sua empresa.' },
];

const PASSOS = [
  ['orcamento', 'Peça o orçamento', 'Conte o que você imagina e mande fotos de referência aqui pelo site.'],
  ['dinheiro', 'Confirme com o sinal', 'Combinado o valor, o sinal garante a sua data na agenda do ateliê.'],
  ['pincel', 'Aprove a arte', 'Você recebe um link com a arte e aprova ou pede ajuste antes da produção.'],
  ['presente', 'Receba com carinho', 'Sua peça é produzida à mão e embalada para presentear.'],
];

export default function Site() {
  const [site, setSite] = useState(null);
  const [erro, setErro] = useState(null);
  const [pecaAberta, setPecaAberta] = useState(null);
  const [pecaOrcamento, setPecaOrcamento] = useState(null);
  const [params, setParams] = useSearchParams();

  useEffect(() => {
    document.title = 'Ateliê Jô Monteiro — Papelaria criativa e presentes únicos';
    api.get('/api/publico/site').then(setSite).catch((e) => setErro(e.message));
  }, []);

  const atelie = site?.atelie || { nomeAtelie: 'Ateliê Jô Monteiro', instagram: 'jomonteiroatelie' };
  const fotos = site?.portfolio || [];
  const catalogo = site?.catalogo || [];

  // Vindo do catálogo ("Pedir orçamento desta peça"): já deixa a peça escolhida no formulário
  useEffect(() => {
    const id = Number(params.get('peca'));
    if (!id || !site) return;
    const p = catalogo.find((i) => i.id === id);
    if (p) {
      setPecaOrcamento(p);
      setTimeout(() => document.getElementById('orcamento')?.scrollIntoView({ behavior: 'smooth' }), 50);
    }
    params.delete('peca');
    setParams(params, { replace: true });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [site]);

  function orcarPeca(p) {
    setPecaAberta(null);
    setPecaOrcamento(p);
    document.getElementById('orcamento')?.scrollIntoView({ behavior: 'smooth' });
  }

  return (
    <div className="site">
      <header className="envolve">
        <div className="cabecalho">
          <a href="#inicio" className="marca"><Marca variante="horizontal" /></a>
          <nav aria-label="Principal">
            {catalogo.length > 0 ? <Link to="/catalogo">Catálogo</Link> : <a href="#servicos">O que faço</a>}
            <a href="#como-funciona">Como funciona</a>
            <a href="#portfolio">Portfólio</a>
            <a href="#sobre">Quem sou</a>
            <a href="#orcamento" className="btn btn-terra">Pedir orçamento</a>
          </nav>
        </div>
      </header>

      <main>
        <section id="inicio" className="envolve" style={{ paddingTop: 28 }}>
          <div className="heroi heroi-marca estampa">
            <div style={{ flex: '1 1 440px', minWidth: 0 }}>
              <p className="lema">papelaria criativa &amp; presentes únicos</p>
              <h1>Presentes que contam a sua história</h1>
              <p className="assinatura" style={{ color: '#fff', marginTop: 10 }}>Amor em papel e porcelana.</p>
              <p className="heroi-texto">
                Cadernetas, canecas, garrafas, Bíblias e kits personalizados, feitos à mão em {atelie.cidade || 'Governador Nunes Freire — MA'}.
                Você aprova a arte antes de eu produzir.
              </p>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 14, marginTop: 30 }}>
                <a href="#orcamento" className="btn btn-branco"><Icone nome="orcamento" tamanho={18} />Pedir orçamento</a>
                {catalogo.length > 0
                  ? <Link to="/catalogo" className="btn btn-contorno"><Icone nome="etiqueta" tamanho={18} />Ver o catálogo</Link>
                  : <a href="#portfolio" className="btn btn-contorno"><Icone nome="portfolio" tamanho={18} />Ver trabalhos</a>}
              </div>
            </div>
            {fotos.length >= 3 ? (
              <div className="heroi-fotos">
                {fotos.slice(0, 3).map((f, i) => <div key={f.id}><img src={f.url} alt={f.titulo} loading={i ? 'lazy' : 'eager'} /></div>)}
              </div>
            ) : (
              <div className="heroi-mascote" aria-hidden="true">
                <span className="circulo" />
                <Marca variante="mascote" decorativa />
              </div>
            )}
          </div>
        </section>

        <section id="servicos" className="envolve secao">
          <p className="sobretitulo">o que eu faço</p>
          <h2 className="titulo-secao">Cada detalhe pensado para quem vai receber</h2>
          <div className="servicos">
            {SERVICOS.map((s) => (
              <article key={s.titulo} className="servico" style={{ background: s.fundo }}>
                <div className="icone-caixa" style={{ background: s.caixa }}><Icone nome={s.icone} cor="#fff" tamanho={28} /></div>
                <h3>{s.titulo}</h3>
                <p style={{ color: 'var(--marrom-2)' }}>{s.desc}</p>
              </article>
            ))}
          </div>
        </section>

        <section id="como-funciona" className="envolve secao">
          <div className="bloco bloco-escuro" style={{ background: 'var(--marca-preto)', color: '#fff' }}>
            <p className="sobretitulo" style={{ color: 'var(--ocre)' }}>como funciona</p>
            <h2 className="titulo-secao">Do pedido ao presente, sem surpresa</h2>
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
          </div>
        </section>

        {catalogo.length > 0 && (
          <section id="catalogo" className="envolve secao" aria-labelledby="cat-titulo">
            <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'flex-end', gap: 16, marginBottom: 24 }}>
              <div>
                <p className="sobretitulo">catálogo</p>
                <h2 id="cat-titulo" className="titulo-secao">Queridinhos do ateliê</h2>
              </div>
              <Link to="/catalogo" className="btn btn-claro">Ver o catálogo completo<Icone nome="seta" tamanho={16} /></Link>
            </div>
            <div className="pecas-grade">
              {catalogo.slice(0, 8).map((i) => <CartaoPeca key={i.id} i={i} aoAbrir={() => setPecaAberta(i)} />)}
            </div>
          </section>
        )}

        <Portfolio fotos={fotos} instagram={atelie.instagram} />

        {site?.depoimentos?.length > 0 && <Depoimentos lista={site.depoimentos} />}

        <Orcamento whatsapp={atelie.whatsapp} catalogo={catalogo} peca={pecaOrcamento} aoTrocarPeca={setPecaOrcamento} />

        <section id="sobre" className="envolve secao">
          <div className="bloco" style={{ background: 'var(--areia)', display: 'flex', flexWrap: 'wrap', gap: 40, alignItems: 'center' }}>
            <div className="clay-sm" style={{ flex: '0 1 300px', minWidth: 200, aspectRatio: '1', borderRadius: '50%', background: 'var(--cru)', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden' }}>
              <Marca variante="circular" decorativa style={{ width: '82%' }} />
            </div>
            <div style={{ flex: '1 1 440px', minWidth: 0 }}>
              <p className="sobretitulo">quem sou</p>
              <h2 className="titulo-secao" style={{ marginBottom: 14 }}>Prazer, Jô Monteiro!</h2>
              <p style={{ color: 'var(--marrom-2)', fontWeight: 600, whiteSpace: 'pre-line' }}>{atelie.sobreTexto}</p>
              {atelie.instagram && (
                <a className="btn btn-claro" style={{ marginTop: 18, whiteSpace: 'normal', textAlign: 'center', maxWidth: '100%' }} href={`https://www.instagram.com/${atelie.instagram}/`} target="_blank" rel="noopener noreferrer">
                  Acompanhe no Instagram @{atelie.instagram}
                </a>
              )}
            </div>
          </div>
        </section>
      </main>

      <footer className="envolve rodape-site">
        <div className="bloco" style={{ background: 'var(--cru)', padding: '36px 40px 24px' }}>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 36, justifyContent: 'space-between' }}>
            <div style={{ flex: '1 1 280px' }}>
              <Marca variante="empilhada" style={{ width: 280 }} />
              <p style={{ marginTop: 10, fontWeight: 600, color: 'var(--marrom-2)' }}>Papelaria criativa &amp; presentes únicos · {atelie.cidade || 'Governador Nunes Freire — MA'}</p>
            </div>
            <div style={{ flex: '1 1 220px', display: 'flex', flexDirection: 'column', gap: 8 }}>
              <p className="forte">Contato</p>
              <a href={whatsAtelie(atelie.whatsapp)} target="_blank" rel="noopener noreferrer" className="forte">
                {atelie.whatsapp ? `WhatsApp ${telefone(atelie.whatsapp)}` : 'Falar no WhatsApp'}
              </a>
              {atelie.instagram && <a href={`https://www.instagram.com/${atelie.instagram}/`} target="_blank" rel="noopener noreferrer" className="forte">@{atelie.instagram}</a>}
            </div>
            {(atelie.endereco || atelie.horario) && (
              <div style={{ flex: '1 1 220px', display: 'flex', flexDirection: 'column', gap: 8, fontWeight: 600, color: 'var(--marrom-2)' }}>
                <p className="forte" style={{ color: 'var(--marrom)' }}>Ateliê</p>
                {atelie.endereco && <p>{atelie.endereco}</p>}
                {atelie.horario && <p style={{ whiteSpace: 'pre-line' }}>{atelie.horario}</p>}
              </div>
            )}
          </div>
          <p style={{ marginTop: 28, paddingTop: 18, borderTop: '2px dashed var(--areia)', fontSize: 13, color: 'var(--marrom-3)', fontWeight: 700 }}>
            © {new Date().getFullYear()} {atelie.nomeAtelie || 'Ateliê Jô Monteiro'} · Site por MS Soluções Inteligentes
          </p>
        </div>
      </footer>

      {erro && <div className="toast toast-erro" role="alert">Não foi possível carregar o site agora.</div>}
      {pecaAberta && <DetalhePeca i={pecaAberta} whatsapp={atelie.whatsapp} aoFechar={() => setPecaAberta(null)} aoOrcar={() => orcarPeca(pecaAberta)} />}
    </div>
  );
}

function Depoimentos({ lista }) {
  return (
    <section id="depoimentos" className="envolve secao" aria-labelledby="depo-titulo">
      <p className="sobretitulo">feedbacks</p>
      <h2 id="depo-titulo" className="titulo-secao" style={{ marginBottom: 24 }}>Quem já levou um presente do ateliê</h2>
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

function Portfolio({ fotos, instagram }) {
  const [categoria, setCategoria] = useState('TODOS');
  const categorias = useMemo(() => {
    const usadas = new Set(fotos.map((f) => f.categoria));
    return Object.entries(CATEGORIAS_PORTFOLIO).filter(([k]) => usadas.has(k));
  }, [fotos]);
  const lista = categoria === 'TODOS' ? fotos : fotos.filter((f) => f.categoria === categoria);

  return (
    <section id="portfolio" className="envolve secao">
      <div className="bloco" style={{ background: 'var(--cru)' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'flex-end', gap: 20, marginBottom: 28 }}>
          <div>
            <p className="sobretitulo">portfólio</p>
            <h2 className="titulo-secao">Trabalhos recentes</h2>
          </div>
          {categorias.length > 1 && (
            <div className="chips" role="group" aria-label="Filtrar portfólio">
              {[['TODOS', 'Todos'], ...categorias].map(([k, r]) => (
                <button key={k} type="button" className="chip" aria-pressed={categoria === k} onClick={() => setCategoria(k)}>{r}</button>
              ))}
            </div>
          )}
        </div>
        {fotos.length === 0 ? (
          <p className="mudo forte">
            As fotos dos trabalhos chegam aqui em breve. Enquanto isso, veja tudo no{' '}
            <a href={`https://www.instagram.com/${instagram || 'jomonteiroatelie'}/`} target="_blank" rel="noopener noreferrer">Instagram</a>.
          </p>
        ) : (
          <div className="galeria">
            {lista.map((f) => (
              <figure key={f.id}>
                <img src={f.url} alt={f.titulo} loading="lazy" />
                <figcaption>
                  <span>{f.titulo}</span>
                  <span className="selo selo-terra">{CATEGORIAS_PORTFOLIO[f.categoria]}</span>
                </figcaption>
              </figure>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}

const MAX_FOTOS = 3;

/** Formulário de orçamento: peça (do catálogo ou tipo), quantidade, personalização e até 3 fotos de referência. */
function Orcamento({ whatsapp, catalogo, peca, aoTrocarPeca }) {
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState(null);
  const [enviado, setEnviado] = useState(null);
  const [fotos, setFotos] = useState([]); // [{ arquivo, url }]
  const [preparando, setPreparando] = useState(false);
  const form = useRef();

  useEffect(() => () => fotos.forEach((f) => URL.revokeObjectURL(f.url)), [fotos]);

  async function escolherFotos(e) {
    const lista = Array.from(e.target.files || []);
    e.target.value = '';
    if (!lista.length) return;
    const vagas = MAX_FOTOS - fotos.length;
    if (lista.length > vagas) setErro(`Você pode mandar até ${MAX_FOTOS} fotos. ${vagas > 0 ? `Peguei só ${vagas}.` : ''}`);
    else setErro(null);
    setPreparando(true);
    try {
      const novas = [];
      for (const f of lista.slice(0, Math.max(0, vagas))) {
        const reduzida = await reduzirImagem(f);
        novas.push({ arquivo: reduzida, url: URL.createObjectURL(reduzida) });
      }
      setFotos((atual) => [...atual, ...novas]);
    } catch (err) {
      setErro(err.message);
    } finally {
      setPreparando(false);
    }
  }

  function tirarFoto(n) {
    setFotos((atual) => {
      URL.revokeObjectURL(atual[n].url);
      return atual.filter((_, i) => i !== n);
    });
  }

  async function enviar(e) {
    e.preventDefault();
    const d = Object.fromEntries(new FormData(e.currentTarget).entries());
    const tipoPeca = peca ? peca.nome : d.tipoPeca;
    const dados = {
      nome: d.nome, whatsapp: d.whatsapp, tipoPeca, produtoId: peca?.id ?? null,
      quantidade: Number(d.quantidade) || 1, personalizacao: d.personalizacao || null, cores: d.cores || null,
      dataDesejada: d.dataDesejada || null, detalhes: d.detalhes || null, site: d.site || null,
    };
    const fd = new FormData();
    fd.append('dados', new Blob([JSON.stringify(dados)], { type: 'application/json' }));
    fotos.forEach((f) => fd.append('fotos', f.arquivo));
    setErro(null);
    setEnviando(true);
    try {
      await api.enviar('/api/publico/orcamentos', fd);
      const linhas = [
        'Olá, Jô! Acabei de pedir um orçamento pelo site.',
        `Nome: ${d.nome}`,
        `Peça: ${tipoPeca}${dados.quantidade > 1 ? ` (${dados.quantidade} unidades)` : ''}`,
        d.dataDesejada && `Para: ${d.dataDesejada.split('-').reverse().join('/')}`,
        d.personalizacao && `Personalização: ${d.personalizacao}`,
        d.cores && `Cores/tema: ${d.cores}`,
        d.detalhes && `Detalhes: ${d.detalhes}`,
        fotos.length > 0 && `Mandei ${fotos.length} ${fotos.length === 1 ? 'foto' : 'fotos'} de referência pelo site.`,
      ].filter(Boolean);
      setEnviado(whatsAtelie(whatsapp, linhas.join('\n')));
      form.current?.reset();
      setFotos([]);
      aoTrocarPeca(null);
    } catch (err) {
      setErro(err.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <section id="orcamento" className="envolve secao" style={{ display: 'flex', flexWrap: 'wrap', gap: 28, alignItems: 'stretch' }}>
      <div className="bloco orcamento-lado" style={{ flex: '1 1 340px', minWidth: 0 }}>
        <div>
          <p className="sobretitulo" style={{ color: '#fff' }}>orçamento</p>
          <h2 className="titulo-secao" style={{ marginBottom: 14 }}>Conte o que você imagina</h2>
          <p style={{ fontWeight: 600 }}>
            Preencha os detalhes e mande fotos de referência. Eu recebo seu pedido organizado e te respondo
            pelo WhatsApp com o valor e o prazo.
          </p>
          <ul className="lista-check" style={{ marginTop: 18 }}>
            <li><Icone nome="check" tamanho={16} largura={3} />Antes de produzir, você aprova a arte por um link</li>
            <li><Icone nome="check" tamanho={16} largura={3} />Pedidos em quantidade para escolas e empresas</li>
            <li><Icone nome="check" tamanho={16} largura={3} />Embalagem pronta para presentear</li>
          </ul>
        </div>
        <Marca variante="mascote" decorativa style={{ width: 120, alignSelf: 'flex-end', marginTop: 12 }} />
      </div>

      <div className="bloco" style={{ flex: '1 1 540px', minWidth: 0, background: 'var(--cru)' }}>
        {enviado ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 16, alignItems: 'flex-start' }} role="status">
            <div className="icone-caixa" style={{ background: 'var(--oliva)' }}><Icone nome="check" cor="#fff" tamanho={28} largura={3} /></div>
            <h3 style={{ fontSize: 26 }}>Pedido recebido!</h3>
            <p style={{ fontWeight: 600, color: 'var(--marrom-2)' }}>
              Já anotei tudo por aqui, com as fotos. Para agilizar, me chama no WhatsApp que eu te respondo rapidinho.
            </p>
            <a className="btn btn-oliva" href={enviado} target="_blank" rel="noopener noreferrer"><Icone nome="whats" tamanho={20} />Continuar no WhatsApp</a>
            <button type="button" className="btn btn-fantasma" onClick={() => setEnviado(null)}>Fazer outro pedido</button>
          </div>
        ) : (
          <form ref={form} onSubmit={enviar} className="form-grade">
            {erro && <div className="aviso aviso-erro inteiro" role="alert">{erro}</div>}
            <CampoSite rotulo="Seu nome" nome="nome" required maxLength={140} autoComplete="name" />
            <CampoSite rotulo="Seu WhatsApp" nome="whatsapp" required maxLength={20} inputMode="tel" autoComplete="tel" placeholder="(98) 90000-0000" />
            {peca ? (
              <div className="campo inteiro">
                <span className="rotulo">Peça escolhida</span>
                <div className="peca-escolhida">
                  {peca.fotos[0] ? <img src={peca.fotos[0]} alt="" /> : <Marca variante="simbolo" decorativa />}
                  <strong>{peca.nome}</strong>
                  <button type="button" className="btn btn-claro btn-p" onClick={() => aoTrocarPeca(null)}>Trocar</button>
                </div>
              </div>
            ) : (
              <div className="campo">
                <label htmlFor="o-tipo">O que você quer?</label>
                <select id="o-tipo" name="tipoPeca" className="entrada" required
                  onChange={(e) => { const p = catalogo.find((i) => `peca:${i.id}` === e.target.value); if (p) aoTrocarPeca(p); }}>
                  {TIPOS_PECA.map((t) => <option key={t}>{t}</option>)}
                  {catalogo.length > 0 && (
                    <optgroup label="Peças do catálogo">
                      {catalogo.map((i) => <option key={i.id} value={`peca:${i.id}`}>{i.nome}</option>)}
                    </optgroup>
                  )}
                </select>
              </div>
            )}
            <CampoSite rotulo="Quantidade" nome="quantidade" type="number" min={1} max={5000} defaultValue={1} required />
            <CampoSite rotulo="Para quando?" nome="dataDesejada" type="date" min={hojeIso()} />
            <CampoSite rotulo="Cores ou tema" nome="cores" maxLength={160} placeholder="ex.: lilás e dourado, fundo do mar" />
            <div className="campo inteiro">
              <label htmlFor="o-pers">Personalização</label>
              <textarea id="o-pers" name="personalizacao" rows={2} maxLength={1000} className="entrada"
                placeholder="Nome, frase, data… o que vai escrito ou estampado na peça" />
            </div>
            <div className="campo inteiro">
              <label htmlFor="o-det">Detalhes</label>
              <textarea id="o-det" name="detalhes" rows={2} maxLength={2000} className="entrada" placeholder="Para quem é, ocasião, ideias…" />
            </div>
            <div className="campo inteiro">
              <span className="rotulo">Fotos de referência <span className="mudo" style={{ fontWeight: 600 }}>(opcional, até {MAX_FOTOS})</span></span>
              <div className="fotos-ref">
                {fotos.map((f, n) => (
                  <figure key={f.url}>
                    <img src={f.url} alt={`Foto de referência ${n + 1}`} />
                    <button type="button" className="btn btn-claro btn-icone btn-p" aria-label={`Tirar foto ${n + 1}`} onClick={() => tirarFoto(n)}>
                      <Icone nome="fechar" tamanho={16} />
                    </button>
                  </figure>
                ))}
                {fotos.length < MAX_FOTOS && (
                  <label className="foto-adicionar">
                    <input type="file" accept="image/*" multiple onChange={escolherFotos} disabled={preparando} />
                    <Icone nome="foto" tamanho={26} />
                    <span>{preparando ? 'Preparando…' : 'Adicionar foto'}</span>
                  </label>
                )}
              </div>
              <span className="dica">Foto da pessoa para a caneca, um modelo que você gostou, a logo da empresa…</span>
            </div>
            {/* Campo escondido contra robôs */}
            <div aria-hidden="true" style={{ position: 'absolute', left: -9999, width: 1, height: 1, overflow: 'hidden' }}>
              <label>Site <input name="site" tabIndex={-1} autoComplete="off" /></label>
            </div>
            <button type="submit" className="btn btn-terra inteiro" disabled={enviando || preparando} style={{ minHeight: 54, fontSize: 18 }}>
              <Icone nome="whats" tamanho={20} />{enviando ? 'Enviando…' : 'Enviar pedido de orçamento'}
            </button>
          </form>
        )}
      </div>
    </section>
  );
}

function CampoSite({ rotulo, nome, inteiro, ...resto }) {
  const id = `o-${nome}`;
  return (
    <div className={`campo ${inteiro ? 'inteiro' : ''}`}>
      <label htmlFor={id}>{rotulo}</label>
      <input id={id} name={nome} className="entrada" {...resto} />
    </div>
  );
}
