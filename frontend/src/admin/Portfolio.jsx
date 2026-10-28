import { useState } from 'react';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Campo, Confirmar, Estado, ModalForm, lerForm, useCarregar, useToast, Vazio } from '../componentes/ui.jsx';
import { CATEGORIAS_PORTFOLIO } from '../util.js';
import { Abas, Topo } from './Layout.jsx';

export const ABAS_SITE = [['/admin/portfolio', 'Fotos', 'portfolio'], ['/admin/portfolio/depoimentos', 'Depoimentos', 'aspas']];

export default function Portfolio() {
  const [form, setForm] = useState(null);
  const [excluir, setExcluir] = useState(null);
  const toast = useToast();
  const { dados, erro, carregando, recarregar } = useCarregar(() => api.get('/api/portfolio'));

  async function alternar(f) {
    try {
      await api.put(`/api/portfolio/${f.id}`, { titulo: f.titulo, categoria: f.categoria, ordem: f.ordem, publicado: !f.publicado });
      toast(f.publicado ? 'Foto escondida do site.' : 'Foto publicada no site.');
      recarregar();
    } catch (e) { toast(e.message, 'erro'); }
  }

  return (
    <>
      <Topo titulo="Portfólio">
        <button type="button" className="btn btn-terra" onClick={() => setForm({})}><Icone nome="foto" tamanho={18} />Enviar foto</button>
      </Topo>
      <Abas itens={ABAS_SITE} />
      <p className="mudo forte" style={{ marginTop: -10 }}>As fotos publicadas aparecem no site, na ordem definida (menor número primeiro). As três primeiras também vão para o topo da página.</p>
      <section className="cartao">
        <Estado carregando={carregando} erro={erro}>
          {dados?.length === 0 ? <Vazio>Nenhuma foto ainda. Envie suas melhores peças: JPG, PNG ou WEBP de até 8 MB.</Vazio> : (
            <div className="galeria-adm">
              {dados?.map((f) => (
                <figure key={f.id} className={`foto-adm ${f.publicado ? '' : 'oculta'}`} style={{ margin: 0 }}>
                  <img src={f.url} alt={f.titulo} loading="lazy" />
                  <figcaption style={{ padding: '0 4px' }}>
                    <p className="forte">{f.titulo}</p>
                    <p className="mudo" style={{ fontSize: 13 }}>{CATEGORIAS_PORTFOLIO[f.categoria]} · ordem {f.ordem}{f.publicado ? '' : ' · escondida'}</p>
                  </figcaption>
                  <div style={{ display: 'flex', gap: 4, flexWrap: 'wrap' }}>
                    <button type="button" className="btn btn-claro btn-p" onClick={() => alternar(f)}>{f.publicado ? 'Esconder' : 'Publicar'}</button>
                    <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Editar ${f.titulo}`} onClick={() => setForm(f)}><Icone nome="editar" tamanho={18} /></button>
                    <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Excluir ${f.titulo}`} onClick={() => setExcluir(f)}><Icone nome="lixo" tamanho={18} /></button>
                  </div>
                </figure>
              ))}
            </div>
          )}
        </Estado>
      </section>

      {form && (
        <ModalForm titulo={form.id ? 'Editar foto' : 'Enviar foto'} textoSalvar={form.id ? 'Salvar' : 'Enviar'} aoFechar={() => setForm(null)} aoSalvar={async (fd) => {
          const d = lerForm(fd, ['ordem']);
          const dados = { titulo: d.titulo, categoria: d.categoria, ordem: d.ordem ?? 0, publicado: fd.get('publicado') === 'on' };
          if (form.id) {
            await api.put(`/api/portfolio/${form.id}`, dados);
          } else {
            const arquivo = fd.get('foto');
            if (!arquivo || !arquivo.size) throw new Error('Escolha uma foto.');
            if (arquivo.size > 8 * 1024 * 1024) throw new Error('A foto deve ter no máximo 8 MB.');
            const envio = new FormData();
            envio.append('dados', new Blob([JSON.stringify(dados)], { type: 'application/json' }));
            envio.append('foto', arquivo);
            await api.enviar('/api/portfolio', envio);
          }
          toast('Foto salva.');
          setForm(null);
          recarregar();
        }}>
          {!form.id && <Campo rotulo="Foto" nome="foto" tipo="file" accept="image/jpeg,image/png,image/webp" required inteiro />}
          <Campo rotulo="Título" nome="titulo" defaultValue={form.titulo} required maxLength={140} inteiro placeholder="Caderneta tema fundo do mar" />
          <Campo rotulo="Categoria" nome="categoria" opcoes={CATEGORIAS_PORTFOLIO} defaultValue={form.categoria || 'PAPELARIA'} />
          <Campo rotulo="Ordem" nome="ordem" tipo="number" defaultValue={form.ordem ?? 0} />
          <label className="checkbox inteiro"><input type="checkbox" name="publicado" defaultChecked={form.publicado ?? true} /> Mostrar no site</label>
        </ModalForm>
      )}

      {excluir && <Confirmar titulo="Excluir foto" perigo textoBotao="Excluir" texto={`Excluir "${excluir.titulo}" do portfólio?`}
        aoFechar={() => setExcluir(null)} aoConfirmar={async () => { await api.del(`/api/portfolio/${excluir.id}`); toast('Foto excluída.'); setExcluir(null); recarregar(); }} />}
    </>
  );
}
