import { useState } from 'react';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { reduzirImagem } from '../componentes/imagens.js';
import { Modal, Selo, useCarregar, useToast } from '../componentes/ui.jsx';
import { STATUS_ARTE, dataHora, linkWhats, primeiroNome } from '../util.js';
import { ImagemProtegida } from './Encomendas.jsx';

/**
 * Aprovação de arte dentro da encomenda: envia versões, copia/manda o link para a cliente
 * e registra o que ela respondeu pelo WhatsApp quando não usou o link.
 */
export default function ArteEncomenda({ e, aoMudar }) {
  const artes = useCarregar(() => api.get(`/api/encomendas/${e.id}/artes`), [e.id]);
  const [nova, setNova] = useState(null); // { arquivo, url, observacao }
  const [ajuste, setAjuste] = useState(null); // texto do ajuste pedido fora do link
  const [ocupado, setOcupado] = useState(false);
  const [erro, setErro] = useState(null);
  const [ver, setVer] = useState(null);
  const toast = useToast();
  const a = artes.dados;
  const encerrada = e.status === 'CANCELADA' || e.status === 'ENTREGUE';
  const ultima = a?.versoes?.[0];
  const aberta = ultima && !ultima.resposta;

  async function acao(fn, msg) {
    setErro(null);
    setOcupado(true);
    try {
      await fn();
      if (msg) toast(msg);
      artes.recarregar();
      aoMudar();
    } catch (err) {
      setErro(err.message);
    } finally {
      setOcupado(false);
    }
  }

  async function escolher(ev) {
    const f = ev.target.files?.[0];
    ev.target.value = '';
    if (!f) return;
    setErro(null);
    try {
      // A arte precisa de nitidez: reduz pouco (só para caber no limite do envio)
      const arquivo = await reduzirImagem(f, 2600, 0.92);
      setNova({ arquivo, url: URL.createObjectURL(arquivo), observacao: '' });
    } catch (err) {
      setErro(err.message);
    }
  }

  function enviarVersao() {
    acao(async () => {
      const fd = new FormData();
      fd.append('arquivo', nova.arquivo);
      if (nova.observacao.trim()) fd.append('observacao', nova.observacao.trim());
      await api.enviar(`/api/encomendas/${e.id}/artes`, fd);
      URL.revokeObjectURL(nova.url);
      setNova(null);
    }, 'Arte enviada. Agora é só mandar o link para a cliente.');
  }

  function marcar(status, comentario) {
    return acao(() => api.patch(`/api/encomendas/${e.id}/arte`, { status, comentario: comentario || null }),
      `Arte: ${STATUS_ARTE[status][0].toLowerCase()}.`);
  }

  const mensagem = a?.link && ultima ? `Oi, ${primeiroNome(e.clienteNome)}! A arte da sua encomenda (${e.descricao}) está pronta`
    + `${ultima.numero > 1 ? ', já com os ajustes' : ''}. Dá uma olhadinha neste link e me diz se posso produzir ou se quer mudar algo: ${a.link}` : '';

  async function mandouNoWhats() {
    // O lembrete do dia com o mesmo link some da lista (a ajudante não tem lembretes: ignora o erro)
    try { await api.post('/api/lembretes/marcar', { chave: `ARTE:${ultima.id}` }); } catch { /* sem acesso */ }
  }

  async function copiar() {
    try {
      await navigator.clipboard.writeText(a.link);
      toast('Link copiado.');
    } catch {
      setErro('Não deu para copiar. Selecione o link e copie à mão.');
    }
  }

  return (
    <section aria-labelledby={`arte-${e.id}`} style={{ marginTop: 24 }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
        <h3 id={`arte-${e.id}`} style={{ fontSize: 18, display: 'flex', alignItems: 'center', gap: 8 }}>
          <Icone nome="pincel" tamanho={20} cor="var(--terra)" />Arte {a && <Selo mapa={STATUS_ARTE} valor={a.status} />}
        </h3>
        {a && a.status !== 'SEM_ARTE' && !encerrada && !nova && (
          <label className="btn btn-terra btn-p botao-upload">
            <Icone nome="mais" tamanho={16} largura={3} />{a.versoes.length ? 'Enviar nova versão' : 'Enviar arte para aprovar'}
            <input type="file" accept="image/jpeg,image/png,image/webp" onChange={escolher} aria-label="Escolher a imagem da arte" />
          </label>
        )}
      </div>

      {erro && <div className="aviso aviso-erro" role="alert" style={{ marginBottom: 8 }}>{erro}</div>}
      {artes.carregando && !a && <p className="mudo forte">Carregando…</p>}

      {a?.status === 'SEM_ARTE' && (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, alignItems: 'center' }}>
          <p className="mudo forte" style={{ flex: '1 1 260px' }}>Esta encomenda não passa por aprovação de arte.</p>
          {!encerrada && <button type="button" className="btn btn-claro btn-p" disabled={ocupado} onClick={() => marcar('A_CRIAR')}>Precisa de arte</button>}
        </div>
      )}

      {nova && (
        <div className="arte-versao" style={{ marginBottom: 12 }}>
          <div className="moldura"><img src={nova.url} alt="Nova versão da arte" /></div>
          <div className="info">
            <div className="campo">
              <label htmlFor={`rec-${e.id}`}>Recado para a cliente <span className="mudo" style={{ fontWeight: 600 }}>(opcional)</span></label>
              <textarea id={`rec-${e.id}`} className="entrada" rows={2} maxLength={500} value={nova.observacao}
                placeholder="ex.: troquei a fonte do nome como você pediu" onChange={(ev) => setNova({ ...nova, observacao: ev.target.value })} />
            </div>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              <button type="button" className="btn btn-terra btn-p" disabled={ocupado} onClick={enviarVersao}>{ocupado ? 'Enviando…' : 'Enviar para aprovação'}</button>
              <button type="button" className="btn btn-fantasma btn-p" onClick={() => { URL.revokeObjectURL(nova.url); setNova(null); }}>Cancelar</button>
            </div>
          </div>
        </div>
      )}

      {a?.status === 'A_CRIAR' && !a.versoes.length && !nova && (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, alignItems: 'center' }}>
          <p className="mudo forte" style={{ flex: '1 1 260px' }}>Envie o mockup: a cliente recebe um link para aprovar ou pedir ajuste.</p>
          <button type="button" className="btn btn-fantasma btn-p" disabled={ocupado} onClick={() => marcar('APROVADA')}>Já foi aprovada fora do sistema</button>
          <button type="button" className="btn btn-fantasma btn-p" disabled={ocupado} onClick={() => marcar('SEM_ARTE')}>Não precisa de arte</button>
        </div>
      )}

      {a?.link && ultima && !encerrada && (
        <div className="link-arte" style={{ marginBottom: 12 }}>
          <code>{a.link}</code>
          <button type="button" className="btn btn-claro btn-p" onClick={copiar}><Icone nome="copiar" tamanho={16} />Copiar</button>
          {aberta && e.clienteWhatsapp && (
            <a className="btn btn-oliva btn-p" href={linkWhats(e.clienteWhatsapp, mensagem)} target="_blank" rel="noopener noreferrer" onClick={mandouNoWhats}>
              <Icone nome="whats" tamanho={16} />Mandar para {primeiroNome(e.clienteNome)}
            </a>
          )}
        </div>
      )}

      {aberta && !encerrada && (
        ajuste == null ? (
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 12 }}>
            <span className="mudo forte" style={{ fontSize: 14, alignSelf: 'center' }}>A cliente respondeu pelo WhatsApp?</span>
            <button type="button" className="btn btn-claro btn-p" disabled={ocupado} onClick={() => marcar('APROVADA')}><Icone nome="check" tamanho={16} largura={3} />Aprovou</button>
            <button type="button" className="btn btn-claro btn-p" disabled={ocupado} onClick={() => setAjuste('')}><Icone nome="pincel" tamanho={16} />Pediu ajuste</button>
          </div>
        ) : (
          <form className="arte-versao" style={{ marginBottom: 12 }} onSubmit={(ev) => { ev.preventDefault(); marcar('AJUSTE_PEDIDO', ajuste).then(() => setAjuste(null)); }}>
            <div className="info">
              <div className="campo">
                <label htmlFor={`aj-${e.id}`}>O que a cliente pediu para mudar?</label>
                <textarea id={`aj-${e.id}`} className="entrada" rows={2} maxLength={1000} required value={ajuste} onChange={(ev) => setAjuste(ev.target.value)} />
              </div>
              <div style={{ display: 'flex', gap: 8 }}>
                <button type="submit" className="btn btn-terra btn-p" disabled={ocupado || !ajuste.trim()}>Registrar ajuste</button>
                <button type="button" className="btn btn-fantasma btn-p" onClick={() => setAjuste(null)}>Cancelar</button>
              </div>
            </div>
          </form>
        )
      )}

      {a?.versoes?.length > 0 && (
        <div className="arte-versoes">
          {a.versoes.map((v, n) => (
            <div key={v.id} className="arte-versao">
              <button type="button" className="moldura" style={{ border: 0, padding: 0, cursor: 'zoom-in' }} onClick={() => setVer(v)}
                aria-label={`Ampliar a versão ${v.numero}`}>
                <ImagemProtegida url={v.url} alt={`Arte versão ${v.numero}`} />
              </button>
              <div className="info">
                <p className="forte">Versão {v.numero} <span className="mudo" style={{ fontWeight: 700 }}>· {dataHora(v.criadoEm)}</span></p>
                {v.observacao && <p style={{ fontSize: 14 }}><strong>Seu recado:</strong> {v.observacao}</p>}
                {v.resposta === 'APROVADA' && <p><span className="selo selo-oliva"><Icone nome="check" tamanho={14} largura={3} />Aprovada {v.respondidaNoSistema ? '(marcada por você)' : 'pela cliente'} · {dataHora(v.respondidoEm)}</span></p>}
                {v.resposta === 'AJUSTE' && <p><span className="selo selo-terra">Ajuste pedido {v.respondidaNoSistema ? '(marcado por você)' : 'pela cliente'}</span></p>}
                {v.comentarioCliente && <p style={{ fontSize: 14, background: 'var(--terra-claro)', borderRadius: 12, padding: '8px 12px', fontWeight: 600 }}>“{v.comentarioCliente}”</p>}
                {!v.resposta && n === 0 && <p className="mudo forte" style={{ fontSize: 14 }}>Esperando a resposta da cliente.</p>}
                {!v.resposta && n > 0 && <p className="mudo forte" style={{ fontSize: 14 }}>Substituída pela versão {v.numero + 1}.</p>}
                {!v.resposta && n === 0 && !encerrada && (
                  <button type="button" className="btn btn-fantasma btn-p" style={{ alignSelf: 'flex-start' }} disabled={ocupado}
                    onClick={() => acao(() => api.del(`/api/artes/${v.id}`), 'Versão excluída.')}><Icone nome="lixo" tamanho={16} />Excluir esta versão</button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
      {ver && (
        <Modal titulo={`Arte · versão ${ver.numero}`} aoFechar={() => setVer(null)} largo>
          <div style={{ borderRadius: 20, overflow: 'hidden', background: 'var(--areia)' }}>
            <ImagemProtegida url={ver.url} alt={`Arte versão ${ver.numero}`} style={{ width: '100%', maxHeight: '70vh', objectFit: 'contain' }} />
          </div>
        </Modal>
      )}
    </section>
  );
}
