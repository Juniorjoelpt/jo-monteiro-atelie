import { useState } from 'react';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Campo, Confirmar, Estado, ModalForm, lerForm, useCarregar, useToast, Vazio } from '../componentes/ui.jsx';
import { Abas, Topo } from './Layout.jsx';
import { ABAS_SITE } from './Portfolio.jsx';

export function Estrelas({ n, tamanho = 16 }) {
  return (
    <span className="estrelas" role="img" aria-label={`${n} de 5 estrelas`}>
      {[1, 2, 3, 4, 5].map((i) => <Icone key={i} nome="estrela" tamanho={tamanho} cor="currentColor" className={i > n ? 'apagada' : ''} style={{ fill: i <= n ? 'currentColor' : 'none' }} />)}
    </span>
  );
}

/** Depoimentos do site. Dica: copie o que a cliente escreveu no WhatsApp (com a autorização dela). */
export default function Depoimentos() {
  const [form, setForm] = useState(null);
  const [excluir, setExcluir] = useState(null);
  const toast = useToast();
  const { dados, erro, carregando, recarregar } = useCarregar(() => api.get('/api/depoimentos'));

  async function salvar(fd, d) {
    const corpo = new FormData();
    corpo.append('dados', new Blob([JSON.stringify(d)], { type: 'application/json' }));
    const foto = fd.get('foto');
    if (foto && foto.size > 0) {
      if (foto.size > 8 * 1024 * 1024) throw new Error('A foto deve ter no máximo 8 MB.');
      corpo.append('foto', foto);
    }
    await api.enviar(form.id ? `/api/depoimentos/${form.id}` : '/api/depoimentos', corpo);
  }

  async function alternar(d) {
    try {
      const corpo = new FormData();
      corpo.append('dados', new Blob([JSON.stringify({ nome: d.nome, contexto: d.contexto, texto: d.texto, estrelas: d.estrelas, ordem: d.ordem, publicado: !d.publicado })], { type: 'application/json' }));
      await api.enviar(`/api/depoimentos/${d.id}`, corpo);
      toast(d.publicado ? 'Depoimento escondido do site.' : 'Depoimento publicado no site.');
      recarregar();
    } catch (e) { toast(e.message, 'erro'); }
  }

  return (
    <>
      <Topo titulo="Depoimentos">
        <button type="button" className="btn btn-terra" onClick={() => setForm({})}><Icone nome="mais" tamanho={18} largura={3} />Novo depoimento</button>
      </Topo>
      <Abas itens={ABAS_SITE} />
      <p className="mudo forte">
        Aparecem no site, na ordem definida (menor número primeiro). Peça autorização antes de publicar:
        o sistema já lembra de pedir um depoimento alguns dias depois da entrega.
      </p>
      <Estado carregando={carregando} erro={erro}>
        {dados?.length === 0 ? <div className="cartao"><Vazio>Nenhum depoimento ainda. Copie aqui os feedbacks que as clientes mandam no WhatsApp e no Instagram.</Vazio></div> : (
          <div className="depo-grade">
            {dados?.map((d) => (
              <figure key={d.id} className={`depo-card ${d.publicado ? '' : 'rascunho'}`}>
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, flexWrap: 'wrap' }}>
                  <Estrelas n={d.estrelas} />
                  <span className="mudo" style={{ fontSize: 13, fontWeight: 800 }}>ordem {d.ordem}{d.publicado ? '' : ' · escondido'}</span>
                </div>
                <blockquote>{d.texto}</blockquote>
                <figcaption className="depo-autor">
                  {d.foto ? <img className="foto" src={d.foto} alt="" /> : <span className="foto" aria-hidden="true">{d.nome.charAt(0)}</span>}
                  <div><p className="forte">{d.nome}</p>{d.contexto && <p>{d.contexto}</p>}</div>
                </figcaption>
                <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
                  <button type="button" className="btn btn-claro btn-p" onClick={() => alternar(d)}>{d.publicado ? 'Esconder' : 'Publicar'}</button>
                  <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Editar depoimento de ${d.nome}`} onClick={() => setForm(d)}><Icone nome="editar" tamanho={18} /></button>
                  <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Excluir depoimento de ${d.nome}`} onClick={() => setExcluir(d)}><Icone nome="lixo" tamanho={18} /></button>
                </div>
              </figure>
            ))}
          </div>
        )}
      </Estado>

      {form && (
        <ModalForm titulo={form.id ? 'Editar depoimento' : 'Novo depoimento'} aoFechar={() => setForm(null)} aoSalvar={async (fd) => {
          const d = lerForm(fd, ['estrelas', 'ordem']);
          delete d.foto;
          d.publicado = fd.get('publicado') === 'on';
          d.removerFoto = fd.get('removerFoto') === 'on';
          await salvar(fd, d);
          toast('Depoimento salvo.');
          setForm(null);
          recarregar();
        }}>
          <Campo rotulo="Nome" nome="nome" defaultValue={form.nome} required maxLength={120} placeholder="Camila S." />
          <Campo rotulo="Quem é" nome="contexto" defaultValue={form.contexto} maxLength={120} placeholder="Professora, mãe da Alice, cliente…" />
          <Campo rotulo="Depoimento" nome="texto" linhas={5} defaultValue={form.texto} required maxLength={600} inteiro
            dica="Até 600 caracteres. As aspas são colocadas pelo site." />
          <Campo rotulo="Estrelas" nome="estrelas" opcoes={[['5', '★★★★★ (5)'], ['4', '★★★★ (4)'], ['3', '★★★ (3)']]} defaultValue={String(form.estrelas ?? 5)} />
          <Campo rotulo="Ordem no site" nome="ordem" tipo="number" step="1" defaultValue={form.ordem ?? 0} />
          <div className="campo inteiro">
            <label htmlFor="depo-foto">{form.foto ? 'Trocar a foto' : 'Foto (opcional)'}</label>
            <input id="depo-foto" name="foto" type="file" accept="image/jpeg,image/png,image/webp" className="entrada" />
            <span className="dica">Da cliente ou da peça dela. JPG, PNG ou WEBP de até 8 MB.</span>
          </div>
          {form.foto && <label className="checkbox inteiro"><input type="checkbox" name="removerFoto" /> Tirar a foto atual</label>}
          <label className="checkbox inteiro"><input type="checkbox" name="publicado" defaultChecked={form.publicado ?? true} /> Mostrar no site (a pessoa autorizou)</label>
        </ModalForm>
      )}
      {excluir && <Confirmar titulo="Excluir depoimento" perigo textoBotao="Excluir" texto={`Excluir o depoimento de ${excluir.nome}?`}
        aoFechar={() => setExcluir(null)} aoConfirmar={async () => { await api.del(`/api/depoimentos/${excluir.id}`); toast('Depoimento excluído.'); setExcluir(null); recarregar(); }} />}
    </>
  );
}
