import { useEffect, useRef, useState } from 'react';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import Marca, { SemImagem } from '../componentes/Marca.jsx';
import { reduzirImagem } from '../componentes/imagens.js';
import { TIPOS_PECA, hojeIso, moeda, whatsAtelie } from '../util.js';
import { sacola } from './sacola.js';

const MAX_FOTOS = 3;

/**
 * Pedido de orçamento. Com peças na sacola, o pedido leva todas elas; sem sacola, a cliente escolhe o tipo de peça.
 * Junto: quantidade, data, personalização, cores e até 3 fotos de referência.
 */
export default function Orcamento({ whatsapp, itens, tipoInicial }) {
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState(null);
  const [enviado, setEnviado] = useState(null);
  const [fotos, setFotos] = useState([]); // [{ arquivo, url }]
  const [preparando, setPreparando] = useState(false);
  const [tipo, setTipo] = useState(tipoInicial || TIPOS_PECA[0]);
  const form = useRef();
  const comSacola = itens.length > 0;

  useEffect(() => { if (tipoInicial) setTipo(tipoInicial); }, [tipoInicial]);
  useEffect(() => () => fotos.forEach((f) => URL.revokeObjectURL(f.url)), [fotos]);

  async function escolherFotos(e) {
    const lista = Array.from(e.target.files || []);
    e.target.value = '';
    if (!lista.length) return;
    const vagas = MAX_FOTOS - fotos.length;
    setErro(lista.length > vagas ? `Você pode mandar até ${MAX_FOTOS} fotos.${vagas > 0 ? ` Peguei só ${vagas}.` : ''}` : null);
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
    const dados = {
      nome: d.nome, whatsapp: d.whatsapp,
      tipoPeca: comSacola ? 'Sacola do site' : tipo,
      itens: comSacola ? itens.map((i) => ({ produtoId: i.id, quantidade: i.quantidade })) : null,
      quantidade: comSacola ? 1 : Number(d.quantidade) || 1,
      personalizacao: d.personalizacao || null, cores: d.cores || null,
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
        ...(comSacola
          ? ['Peças:', ...itens.map((i) => `• ${i.quantidade}× ${i.peca.nome}`)]
          : [`Peça: ${tipo}${dados.quantidade > 1 ? ` (${dados.quantidade} unidades)` : ''}`]),
        d.dataDesejada && `Para: ${d.dataDesejada.split('-').reverse().join('/')}`,
        d.personalizacao && `Personalização: ${d.personalizacao}`,
        d.cores && `Cores/tema: ${d.cores}`,
        d.detalhes && `Detalhes: ${d.detalhes}`,
        fotos.length > 0 && `Mandei ${fotos.length} ${fotos.length === 1 ? 'foto' : 'fotos'} de referência pelo site.`,
      ].filter(Boolean);
      setEnviado(whatsAtelie(whatsapp, linhas.join('\n')));
      form.current?.reset();
      setFotos([]);
      sacola.limpar();
    } catch (err) {
      setErro(err.message);
    } finally {
      setEnviando(false);
    }
  }

  return (
    <section id="orcamento" className="loja-secao orcamento-loja" aria-labelledby="orc-titulo">
      <div className="orcamento-lado">
        <div>
          <p className="sobretitulo" style={{ color: '#fff' }}>orçamento</p>
          <h2 id="orc-titulo" className="titulo-secao" style={{ marginBottom: 12 }}>Conte o que você imagina</h2>
          <p style={{ fontWeight: 600 }}>
            {comSacola ? 'As peças da sua sacola já vão no pedido. ' : ''}
            Mande os detalhes e as fotos de referência: eu te respondo pelo WhatsApp com o valor e o prazo.
          </p>
          <ul className="lista-check" style={{ marginTop: 16 }}>
            <li><Icone nome="check" tamanho={16} largura={3} />Antes de produzir, você aprova a arte por um link</li>
            <li><Icone nome="check" tamanho={16} largura={3} />Pedidos em quantidade para escolas e empresas</li>
            <li><Icone nome="check" tamanho={16} largura={3} />Embalagem pronta para presentear</li>
          </ul>
        </div>
        <Marca variante="mascote" decorativa style={{ width: 110, alignSelf: 'flex-end' }} />
      </div>

      <div className="orcamento-form">
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
            {comSacola ? (
              <div className="campo inteiro">
                <span className="rotulo">Peças da sacola</span>
                <ul className="sacola-resumo">
                  {itens.map((i) => (
                    <li key={i.id}>
                      <span className="mini">{i.peca.fotos[0] ? <img src={i.peca.fotos[0]} alt="" /> : <SemImagem />}</span>
                      <span className="nome">{i.peca.nome}</span>
                      <span className="qtd">{i.quantidade}×</span>
                      <span className="valor">{i.peca.preco != null ? moeda(Number(i.peca.preco) * i.quantidade) : 'sob consulta'}</span>
                    </li>
                  ))}
                </ul>
                <span className="dica">Para mudar as quantidades, use a sacola.</span>
              </div>
            ) : (
              <>
                <div className="campo">
                  <label htmlFor="o-tipo">O que você quer?</label>
                  <select id="o-tipo" className="entrada" value={tipo} onChange={(e) => setTipo(e.target.value)}>
                    {[...new Set([...TIPOS_PECA, tipo])].map((t) => <option key={t}>{t}</option>)}
                  </select>
                </div>
                <CampoSite rotulo="Quantidade" nome="quantidade" type="number" min={1} max={5000} defaultValue={1} required />
              </>
            )}
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
