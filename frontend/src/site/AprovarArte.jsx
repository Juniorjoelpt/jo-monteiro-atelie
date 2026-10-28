import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import Marca, { Mascote } from '../componentes/Marca.jsx';
import { dataHora, whatsAtelie } from '../util.js';

/**
 * Página que a cliente abre pelo link (sem senha) para ver a arte da encomenda e responder:
 * aprovar ou pedir ajuste. Versões antigas e respostas ficam no histórico, embaixo.
 */
export default function AprovarArte() {
  const { codigo } = useParams();
  const [arte, setArte] = useState(null);
  const [erro, setErro] = useState(null);
  const [modo, setModo] = useState(null); // 'aprovar' | 'ajuste'
  const [comentario, setComentario] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [erroEnvio, setErroEnvio] = useState(null);
  const [resposta, setResposta] = useState(null); // 'aprovada' | 'ajuste' (acabou de responder)

  useEffect(() => {
    document.title = 'Aprovação da arte · Ateliê Jô Monteiro';
    api.get(`/api/publico/artes/${codigo}`).then(setArte).catch((e) => setErro(e.status === 404
      ? 'Não encontramos esta arte. Confira se o link está completo ou fale com a Jô pelo WhatsApp.'
      : e.message));
  }, [codigo]);

  const atual = arte?.versoes?.find((v) => v.atual);
  const anteriores = arte?.versoes?.filter((v) => !v.atual) || [];
  const podeResponder = arte?.status === 'AGUARDANDO_CLIENTE' && atual && !atual.resposta;

  async function responder(aprovar) {
    setEnviando(true);
    setErroEnvio(null);
    try {
      const r = await api.post(`/api/publico/artes/${codigo}/resposta`, {
        versaoId: atual.id, aprovar, comentario: comentario.trim() || null, site: null,
      });
      setArte(r);
      setResposta(aprovar ? 'aprovada' : 'ajuste');
      setModo(null);
      setComentario('');
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } catch (e) {
      setErroEnvio(e.message);
    } finally {
      setEnviando(false);
    }
  }

  const textoWhats = arte ? `Olá, Jô! Sobre a arte da minha encomenda (${arte.descricao}):` : '';

  return (
    <div className="site arte-pagina">
      <header className="envolve">
        <div className="cabecalho" style={{ justifyContent: 'center' }}>
          <a href="/" className="marca"><Marca variante="horizontal" /></a>
        </div>
      </header>

      <main className="envolve" style={{ maxWidth: 880, paddingTop: 24, paddingBottom: 48, display: 'flex', flexDirection: 'column', gap: 22 }}>
        {erro && <div className="aviso aviso-erro" role="alert">{erro}</div>}
        {!erro && !arte && <p className="carregando">Carregando a sua arte…</p>}

        {arte && (
          <>
            <div>
              <p className="sobretitulo">aprovação da arte</p>
              <h1 className="titulo-secao">Oi, {arte.cliente}!</h1>
              <p style={{ marginTop: 6, fontWeight: 600, color: 'var(--marrom-2)', fontSize: 18 }}>
                {arte.status === 'A_CRIAR' || !atual
                  ? 'A arte da sua encomenda ainda está sendo criada. Assim que ficar pronta, ela aparece aqui, neste mesmo link.'
                  : podeResponder
                    ? 'A arte da sua encomenda está pronta. Confira com calma e me diga se posso produzir.'
                    : 'Aqui fica a arte da sua encomenda e o histórico das versões.'}
              </p>
            </div>

            {resposta === 'aprovada' && (
              <div className="aviso aviso-ok arte-aviso" role="status">
                <Icone nome="check" tamanho={26} largura={3} />
                <span>Arte aprovada! Obrigada pela confiança. Agora sua peça vai para a produção.</span>
              </div>
            )}
            {resposta === 'ajuste' && (
              <div className="aviso aviso-info arte-aviso" role="status">
                <Icone nome="pincel" tamanho={26} />
                <span>Recebi o seu pedido de ajuste. Quando a nova versão ficar pronta, ela aparece neste mesmo link.</span>
              </div>
            )}

            <section className="cartao" aria-labelledby="pedido-titulo">
              <h2 id="pedido-titulo" style={{ fontSize: 20 }}>{arte.descricao}</h2>
              {arte.personalizacao && (
                <p style={{ marginTop: 8, fontWeight: 600, color: 'var(--marrom-2)', whiteSpace: 'pre-line' }}>
                  <strong style={{ color: 'var(--marrom)' }}>O que combinamos: </strong>{arte.personalizacao}
                </p>
              )}
            </section>

            {atual && (
              <section className="cartao arte-atual" aria-labelledby="versao-titulo">
                <div className="cartao-titulo">
                  <h2 id="versao-titulo">Versão {atual.numero}</h2>
                  <SeloResposta v={atual} status={arte.status} />
                </div>
                <a href={atual.url} target="_blank" rel="noopener noreferrer" className="arte-imagem" aria-label="Abrir a arte em tamanho grande">
                  <img src={atual.url} alt={`Arte da encomenda, versão ${atual.numero}`} />
                  <span className="selo selo-areia"><Icone nome="olho" tamanho={16} />Ver em tamanho grande</span>
                </a>
                {atual.observacao && (
                  <p className="arte-recado"><span className="cursiva" style={{ fontSize: 28, lineHeight: 1 }}>Jô:</span> {atual.observacao}</p>
                )}
                {atual.comentarioCliente && (
                  <p className="arte-recado cliente"><strong>Você: </strong>{atual.comentarioCliente}</p>
                )}

                {podeResponder && !modo && (
                  <div className="arte-conferir">
                    <p className="forte">Antes de aprovar, confira com atenção:</p>
                    <ul className="lista-check">
                      <li><Icone nome="check" tamanho={16} largura={3} />Nomes, frases e datas escritos certinho</li>
                      <li><Icone nome="check" tamanho={16} largura={3} />Cores e tema do jeito que você imaginou</li>
                      <li><Icone nome="check" tamanho={16} largura={3} />Fotos na posição certa</li>
                    </ul>
                    <p className="mudo" style={{ fontSize: 14, fontWeight: 600 }}>
                      As cores na tela podem variar um pouquinho em relação à peça impressa.
                    </p>
                    <div className="arte-botoes">
                      <button type="button" className="btn btn-oliva" onClick={() => setModo('aprovar')}><Icone nome="check" tamanho={20} largura={3} />Aprovar a arte</button>
                      <button type="button" className="btn btn-claro" onClick={() => setModo('ajuste')}><Icone nome="pincel" tamanho={20} />Pedir ajuste</button>
                    </div>
                  </div>
                )}

                {podeResponder && modo === 'aprovar' && (
                  <div className="arte-conferir" role="group" aria-labelledby="conf-aprovar">
                    <p id="conf-aprovar" className="forte" style={{ fontSize: 18 }}>Posso produzir com esta arte?</p>
                    <p style={{ fontWeight: 600, color: 'var(--marrom-2)' }}>Depois da aprovação, a peça vai para a produção do jeitinho que está aqui.</p>
                    <div className="campo">
                      <label htmlFor="a-coment">Quer deixar um recado? <span className="mudo" style={{ fontWeight: 600 }}>(opcional)</span></label>
                      <textarea id="a-coment" className="entrada" rows={2} maxLength={1000} value={comentario} onChange={(e) => setComentario(e.target.value)} />
                    </div>
                    {erroEnvio && <div className="aviso aviso-erro" role="alert">{erroEnvio}</div>}
                    <div className="arte-botoes">
                      <button type="button" className="btn btn-oliva" disabled={enviando} onClick={() => responder(true)}>
                        <Icone nome="check" tamanho={20} largura={3} />{enviando ? 'Enviando…' : 'Sim, pode produzir'}
                      </button>
                      <button type="button" className="btn btn-fantasma" onClick={() => { setModo(null); setErroEnvio(null); }}>Voltar</button>
                    </div>
                  </div>
                )}

                {podeResponder && modo === 'ajuste' && (
                  <div className="arte-conferir">
                    <div className="campo">
                      <label htmlFor="a-ajuste" style={{ fontSize: 18 }}>O que você gostaria de mudar?</label>
                      <textarea id="a-ajuste" className="entrada" rows={4} maxLength={1000} required value={comentario}
                        placeholder="ex.: trocar o nome para “Ana Luísa”, deixar o fundo mais clarinho…" onChange={(e) => setComentario(e.target.value)} />
                    </div>
                    {erroEnvio && <div className="aviso aviso-erro" role="alert">{erroEnvio}</div>}
                    <div className="arte-botoes">
                      <button type="button" className="btn btn-terra" disabled={enviando || comentario.trim().length < 3} onClick={() => responder(false)}>
                        <Icone nome="pincel" tamanho={20} />{enviando ? 'Enviando…' : 'Enviar pedido de ajuste'}
                      </button>
                      <button type="button" className="btn btn-fantasma" onClick={() => { setModo(null); setErroEnvio(null); }}>Voltar</button>
                    </div>
                  </div>
                )}
              </section>
            )}

            {anteriores.length > 0 && (
              <section className="cartao" aria-labelledby="hist-titulo">
                <h2 id="hist-titulo" style={{ fontSize: 20, marginBottom: 12 }}>Versões anteriores</h2>
                <ul className="lista-linhas">
                  {anteriores.map((v) => (
                    <li key={v.id} style={{ alignItems: 'flex-start' }}>
                      <a href={v.url} target="_blank" rel="noopener noreferrer" className="arte-mini" aria-label={`Abrir a versão ${v.numero}`}>
                        <img src={v.url} alt="" loading="lazy" />
                      </a>
                      <div className="info">
                        <p>Versão {v.numero} · <span className="mudo" style={{ fontWeight: 700 }}>{dataHora(v.criadoEm)}</span></p>
                        {v.comentarioCliente && <p>Você pediu: “{v.comentarioCliente}”</p>}
                        {!v.resposta && <p>Substituída por uma versão mais nova</p>}
                      </div>
                    </li>
                  ))}
                </ul>
              </section>
            )}

            <div className="bloco" style={{ background: 'var(--terra-claro)', display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 18, padding: '22px 28px' }}>
              <Mascote pose="notebook" style={{ width: 84 }} />
              <p style={{ flex: '1 1 260px', fontWeight: 800 }}>Ficou com alguma dúvida? Fale direto comigo.</p>
              <a className="btn btn-terra" href={whatsAtelie(arte.whatsappAtelie, textoWhats)} target="_blank" rel="noopener noreferrer">
                <Icone nome="whats" tamanho={18} />Falar no WhatsApp
              </a>
            </div>
          </>
        )}
      </main>
    </div>
  );
}

function SeloResposta({ v, status }) {
  if (v.resposta === 'APROVADA') return <span className="selo selo-oliva"><Icone nome="check" tamanho={14} largura={3} />Aprovada {v.respondidoEm ? `em ${dataHora(v.respondidoEm)}` : ''}</span>;
  if (v.resposta === 'AJUSTE') return <span className="selo selo-terra"><Icone nome="pincel" tamanho={14} />Ajuste pedido</span>;
  if (status === 'AGUARDANDO_CLIENTE') return <span className="selo selo-ocre">Esperando a sua resposta</span>;
  return null;
}
