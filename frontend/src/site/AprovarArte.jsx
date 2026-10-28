import { useEffect, useRef, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import Marca, { Mascote } from '../componentes/Marca.jsx';
import { dataExtensa, dataHora, diasAte, etapasPedido, moeda, whatsAtelie } from '../util.js';

/**
 * Página do pedido, que a cliente abre pelo link (sem senha): acompanha o andamento, vê a data combinada
 * e quanto falta pagar, e aprova a arte (ou pede ajuste). Os links antigos /arte/<código> abrem a mesma página.
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
  const avisado = useRef(null);

  useEffect(() => {
    document.title = 'Meu pedido · Ateliê Jô Monteiro';
    api.get(`/api/publico/artes/${codigo}`).then(setArte).catch((e) => setErro(e.status === 404
      ? 'Não encontramos este pedido. Confira se o link está completo ou fale com a Jô pelo WhatsApp.'
      : e.message));
  }, [codigo]);

  const atual = arte?.versoes?.find((v) => v.atual);
  const anteriores = arte?.versoes?.filter((v) => !v.atual) || [];
  const podeResponder = arte?.status === 'AGUARDANDO_CLIENTE' && atual && !atual.resposta;

  // Avisa o ateliê que o pedido (e a versão atual da arte) foi aberto. Uma vez por versão.
  useEffect(() => {
    if (!arte) return;
    const chave = atual?.id ?? 0;
    if (avisado.current === chave) return;
    avisado.current = chave;
    api.post(`/api/publico/artes/${codigo}/visto`, { versaoId: atual?.id ?? null }).catch(() => {});
  }, [arte, atual?.id, codigo]);

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
      document.getElementById('arte')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } catch (e) {
      setErroEnvio(e.message);
    } finally {
      setEnviando(false);
    }
  }

  const textoWhats = arte ? `Olá, Jô! Sobre a minha encomenda (${arte.descricao}):` : '';
  const and = arte?.andamento;

  return (
    <div className="site arte-pagina">
      <header className="envolve">
        <div className="cabecalho" style={{ justifyContent: 'center' }}>
          <a href="/" className="marca"><Marca variante="horizontal" /></a>
        </div>
      </header>

      <main className="envolve" style={{ maxWidth: 880, paddingTop: 24, paddingBottom: 48, display: 'flex', flexDirection: 'column', gap: 22 }}>
        {erro && <div className="aviso aviso-erro" role="alert">{erro}</div>}
        {!erro && !arte && <p className="carregando">Carregando o seu pedido…</p>}

        {arte && (
          <>
            <div>
              <p className="sobretitulo">meu pedido</p>
              <h1 className="titulo-secao">Oi, {arte.cliente}!</h1>
              <p style={{ marginTop: 6, fontWeight: 600, color: 'var(--marrom-2)', fontSize: 18 }}>
                {podeResponder
                  ? 'A arte da sua encomenda está pronta. Confira com calma e me diga se posso produzir.'
                  : 'Aqui você acompanha cada etapa da sua encomenda. É só voltar neste link quando quiser.'}
              </p>
              {podeResponder && (
                <a href="#arte" className="btn btn-terra" style={{ marginTop: 14 }}><Icone nome="pincel" tamanho={18} />Ver a arte e responder</a>
              )}
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

            {and && <Andamento arte={arte} and={and} />}

            {(atual || arte.status === 'A_CRIAR' || arte.status === 'AJUSTE_PEDIDO') && arte.status !== 'SEM_ARTE' && (
              <section id="arte" className="cartao arte-atual" aria-labelledby="versao-titulo" style={{ scrollMarginTop: 16 }}>
                <div className="cartao-titulo">
                  <h2 id="versao-titulo">{atual ? `Arte · versão ${atual.numero}` : 'Arte'}</h2>
                  {atual && <SeloResposta v={atual} status={arte.status} />}
                </div>
                {!atual && <p className="mudo forte">A arte da sua peça ainda está sendo criada. Assim que ficar pronta, ela aparece aqui, neste mesmo link.</p>}
                {atual && (
                  <>
                    <a href={atual.url} target="_blank" rel="noopener noreferrer" className="arte-imagem" aria-label="Abrir a arte em tamanho grande">
                      <img src={atual.url} alt={`Arte da encomenda, versão ${atual.numero}`} />
                      <span className="selo selo-areia"><Icone nome="olho" tamanho={16} />Ver em tamanho grande</span>
                    </a>
                    {arte.personalizacao && (
                      <p style={{ fontWeight: 600, color: 'var(--marrom-2)', whiteSpace: 'pre-line' }}>
                        <strong style={{ color: 'var(--marrom)' }}>O que combinamos: </strong>{arte.personalizacao}
                      </p>
                    )}
                    {atual.observacao && (
                      <p className="arte-recado"><span className="cursiva" style={{ fontSize: 28, lineHeight: 1 }}>Jô:</span> {atual.observacao}</p>
                    )}
                    {atual.comentarioCliente && (
                      <p className="arte-recado cliente"><strong>Você: </strong>{atual.comentarioCliente}</p>
                    )}
                  </>
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
                <h2 id="hist-titulo" style={{ fontSize: 20, marginBottom: 12 }}>Versões anteriores da arte</h2>
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

            {and && <Pagamento and={and} />}

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

/** Linha do tempo: confirmado → arte → produção → pronta → entregue, com a data combinada. */
function Andamento({ arte, and }) {
  const etapas = etapasPedido({ status: and.status, statusArte: arte.status, valorSinal: and.valorSinal });
  const dias = diasAte(and.prazoEntrega);
  return (
    <section className="cartao pedido-andamento" aria-labelledby="and-titulo">
      <div className="cartao-titulo" style={{ alignItems: 'flex-start' }}>
        <div style={{ minWidth: 0 }}>
          <h2 id="and-titulo">{arte.descricao}</h2>
          {and.quantidade > 1 && <p className="mudo forte" style={{ fontSize: 14 }}>{and.quantidade} unidades</p>}
        </div>
        {and.status === 'ENTREGUE' && and.entregueEm ? (
          <div className="pedido-data"><span>Entregue em</span><strong>{dataExtensa(and.entregueEm)}</strong></div>
        ) : and.prazoEntrega ? (
          <div className="pedido-data">
            <span>Entrega combinada</span>
            <strong>{dataExtensa(and.prazoEntrega)}</strong>
            {dias != null && dias >= 0 && dias <= 30 && <small>{dias === 0 ? 'é hoje!' : dias === 1 ? 'é amanhã' : `faltam ${dias} dias`}</small>}
          </div>
        ) : null}
      </div>
      <ol className="etapas">
        {etapas.map((e) => (
          <li key={e.chave} className={e.feito ? 'feita' : e.atual ? 'atual' : ''} aria-current={e.atual ? 'step' : undefined}>
            <span className="etapa-marca" aria-hidden="true">{e.feito ? <Icone nome="check" tamanho={16} largura={3} /> : null}</span>
            <div>
              <p className="etapa-rotulo">{e.rotulo}{e.atual && <span className="sr"> (etapa atual)</span>}</p>
              {e.texto && (e.feito || e.atual) && <p className="etapa-texto">{e.texto}</p>}
            </div>
          </li>
        ))}
      </ol>
    </section>
  );
}

/** Valores: total, quanto já foi pago e o saldo, com a chave Pix para pagar. */
function Pagamento({ and }) {
  const [copiado, setCopiado] = useState(false);
  const saldo = Number(and.saldo);
  if (Number(and.valorTotal) <= 0) return null;
  async function copiar() {
    try { await navigator.clipboard.writeText(and.chavePix); setCopiado(true); setTimeout(() => setCopiado(false), 2500); } catch { /* sem área de transferência */ }
  }
  return (
    <section className="cartao" aria-labelledby="pag-titulo">
      <h2 id="pag-titulo" style={{ fontSize: 20, marginBottom: 12 }}>Pagamento</h2>
      <div className="pedido-valores">
        <div><span>Total</span><strong>{moeda(and.valorTotal)}</strong></div>
        <div><span>Já pago</span><strong>{moeda(and.valorPago)}</strong></div>
        <div className={saldo > 0 ? 'falta' : 'ok'}><span>{saldo > 0 ? 'Falta pagar' : 'Situação'}</span><strong>{saldo > 0 ? moeda(saldo) : 'Tudo pago'}</strong></div>
      </div>
      {saldo > 0 && and.chavePix && (
        <div className="pedido-pix">
          <Icone nome="pix" tamanho={26} cor="var(--oliva)" />
          <div style={{ flex: '1 1 200px', minWidth: 0 }}>
            <p className="forte" style={{ fontSize: 14 }}>Chave Pix{and.titularPix ? ` · ${and.titularPix}` : ''}</p>
            <p className="pix-chave">{and.chavePix}</p>
          </div>
          <button type="button" className="btn btn-claro btn-p" onClick={copiar}><Icone nome={copiado ? 'check' : 'copiar'} tamanho={16} />{copiado ? 'Copiada!' : 'Copiar chave'}</button>
        </div>
      )}
      {saldo > 0 && <p className="mudo forte" style={{ fontSize: 14, marginTop: 10 }}>Depois de pagar, mande o comprovante no WhatsApp para eu dar baixa.</p>}
    </section>
  );
}

function SeloResposta({ v, status }) {
  if (v.resposta === 'APROVADA') return <span className="selo selo-oliva"><Icone nome="check" tamanho={14} largura={3} />Aprovada {v.respondidoEm ? `em ${dataHora(v.respondidoEm)}` : ''}</span>;
  if (v.resposta === 'AJUSTE') return <span className="selo selo-terra"><Icone nome="pincel" tamanho={14} />Ajuste pedido</span>;
  if (status === 'AGUARDANDO_CLIENTE') return <span className="selo selo-ocre">Esperando a sua resposta</span>;
  return null;
}
