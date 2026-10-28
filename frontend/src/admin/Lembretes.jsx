import { useState } from 'react';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Estado, useCarregar, useToast, Vazio } from '../componentes/ui.jsx';
import { linkWhats, telefone } from '../util.js';
import { Topo } from './Layout.jsx';

const TIPOS = {
  ARTE: ['Arte para aprovar', 'oliva', 'pincel'],
  PRONTA: ['Peça pronta', 'terra', 'encomenda'],
  ENTREGA: ['Entrega amanhã', 'terra', 'calendario'],
  SALDO: ['Saldo em aberto', 'ocre', 'dinheiro'],
  ANIVERSARIO: ['Aniversário', 'areia', 'cliente'],
  DEPOIMENTO: ['Pedir depoimento', 'terra', 'aspas'],
};

/** Mensagens prontas do dia. Ela toca em "Enviar", o WhatsApp abre com o texto, e o lembrete sai da lista. */
export default function Lembretes() {
  const { dados, erro, carregando, recarregar } = useCarregar(() => api.get('/api/lembretes'));
  const [textos, setTextos] = useState({});
  const toast = useToast();

  async function marcar(l, mensagem) {
    try {
      await api.post('/api/lembretes/marcar', { chave: l.chave });
      toast(mensagem);
      recarregar();
    } catch (e) {
      toast(e.message, 'erro');
    }
  }

  return (
    <>
      <Topo titulo="Lembretes de hoje" />
      <p className="mudo forte" style={{ marginTop: -10 }}>
        O sistema prepara as mensagens. Confira o texto, toque em "Enviar no WhatsApp" e depois em "Já enviei".
        Cobranças de saldo voltam a aparecer depois de 7 dias se continuarem em aberto.
      </p>
      <Estado carregando={carregando} erro={erro}>
        {dados?.length === 0 ? (
          <div className="cartao"><Vazio>Nenhum lembrete para hoje. Tudo em dia!</Vazio></div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {dados?.map((l) => {
              const [rotulo, cor, icone] = TIPOS[l.tipo] || [l.tipo, 'areia', 'sino'];
              const texto = textos[l.chave] ?? l.mensagem;
              return (
                <article key={l.chave} className="lembrete">
                  <div className="msg">
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, alignItems: 'center' }}>
                      <span className={`selo selo-${cor}`}><Icone nome={icone} tamanho={14} />{rotulo}</span>
                      <strong>{l.titulo}</strong>
                    </div>
                    <p className="mudo forte" style={{ fontSize: 14, marginTop: 4 }}>
                      {l.nome}{l.whatsapp ? ` · ${telefone(l.whatsapp)}` : ''}{l.detalhe ? ` · ${l.detalhe}` : ''}
                    </p>
                    <label className="sr" htmlFor={`msg-${l.chave}`}>Mensagem para {l.nome}</label>
                    <textarea id={`msg-${l.chave}`} className="texto-msg entrada" rows={Math.max(4, Math.ceil(texto.length / 42) + 1)} value={texto}
                      onChange={(e) => setTextos((t) => ({ ...t, [l.chave]: e.target.value }))} />
                  </div>
                  <div className="acoes">
                    {l.whatsapp ? (
                      <a className="btn btn-oliva" href={linkWhats(l.whatsapp, texto)} target="_blank" rel="noopener noreferrer">
                        <Icone nome="whats" tamanho={18} />Enviar no WhatsApp
                      </a>
                    ) : (
                      <span className="aviso aviso-info" style={{ fontSize: 14 }}>Sem WhatsApp cadastrado</span>
                    )}
                    <button type="button" className="btn btn-claro" onClick={() => marcar(l, 'Marcado como enviado.')}>
                      <Icone nome="check" tamanho={18} largura={3} />Já enviei
                    </button>
                    <button type="button" className="btn btn-fantasma btn-p" onClick={() => marcar(l, 'Lembrete dispensado.')}>Dispensar</button>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </Estado>
    </>
  );
}
