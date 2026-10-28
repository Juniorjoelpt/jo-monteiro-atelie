import { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import Marca from '../componentes/Marca.jsx';
import { useCarregar } from '../componentes/ui.jsx';
import { FRENTES, STATUS_ENCOMENDA, dataExtensa, dataHora, primeiroNome, qtdUn } from '../util.js';
import { ImagemProtegida } from './Encomendas.jsx';

/**
 * Ficha de produção para imprimir e deixar na bancada: prazo, personalização em letra grande,
 * a arte aprovada, as referências, os materiais da ficha e uma lista para conferir. Sem valores.
 */
export default function FichaProducao() {
  const { id } = useParams();
  const enc = useCarregar(() => api.get(`/api/encomendas/${id}`), [id]);
  const artes = useCarregar(() => api.get(`/api/encomendas/${id}/artes`), [id]);
  const fotos = useCarregar(() => api.get(`/api/encomendas/${id}/fotos`), [id]);
  const produtos = useCarregar(() => api.get('/api/produtos'), []);
  const e = enc.dados;

  useEffect(() => {
    if (e) document.title = `Ficha · ${e.descricao}`;
  }, [e]);

  if (enc.erro) return <div className="ficha-pagina"><div className="aviso aviso-erro">{enc.erro}</div></div>;
  if (!e || !artes.dados || !fotos.dados) return <div className="ficha-pagina"><p className="carregando">Montando a ficha…</p></div>;

  const versoes = artes.dados.versoes || [];
  const aprovada = versoes.find((v) => v.resposta === 'APROVADA');
  const ultima = versoes[0];
  const temArte = artes.dados.status !== 'SEM_ARTE';
  const arte = aprovada || ultima;
  const referencias = fotos.dados.filter((f) => f.tipo === 'REFERENCIA');
  const ficha = e.produtoId ? produtos.dados?.find((p) => p.id === e.produtoId) : null;

  return (
    <div className="ficha-pagina">
      <div className="ficha-barra nao-imprimir">
        <button type="button" className="btn btn-terra" onClick={() => window.print()}><Icone nome="pdf" tamanho={18} />Imprimir ou salvar em PDF</button>
        <button type="button" className="btn btn-claro" onClick={() => window.close()}>Fechar</button>
      </div>

      <article className="ficha-folha">
        <header className="ficha-topo">
          <Marca variante="horizontal" style={{ width: 170 }} />
          <div style={{ textAlign: 'right' }}>
            <p className="ficha-rotulo">Ficha de produção</p>
            <p style={{ fontSize: 22, fontWeight: 900 }}>Encomenda nº {e.id}</p>
            <p className="ficha-pequeno">Impressa {dataHora(new Date().toISOString())}</p>
          </div>
        </header>

        <section className="ficha-destaque">
          <div>
            <p className="ficha-rotulo">Entrega</p>
            <p className="ficha-prazo">{e.prazoEntrega ? dataExtensa(e.prazoEntrega) : 'Sem data combinada'}</p>
          </div>
          <div className="ficha-marcas">
            {e.urgente && <span className="ficha-urgente"><Icone nome="raio" tamanho={16} />URGENTE</span>}
            <span className="ficha-status">{STATUS_ENCOMENDA[e.status][0]}</span>
          </div>
        </section>

        <section className="ficha-grade">
          <div><p className="ficha-rotulo">Peça</p><p className="ficha-valor">{e.descricao}</p></div>
          <div><p className="ficha-rotulo">Quantidade</p><p className="ficha-valor" style={{ fontSize: 26 }}>{e.quantidade}</p></div>
          <div><p className="ficha-rotulo">Cliente</p><p className="ficha-valor">{e.clienteNome}</p></div>
          <div><p className="ficha-rotulo">Frente</p><p className="ficha-valor">{FRENTES[e.frente]}{e.produtoNome ? ` · ficha: ${e.produtoNome}` : ''}</p></div>
        </section>

        <section className="ficha-bloco">
          <p className="ficha-rotulo">Personalização (o que vai na peça)</p>
          <p className="ficha-personalizacao">{e.personalizacao || '—'}</p>
        </section>

        {e.observacoes && (
          <section className="ficha-bloco">
            <p className="ficha-rotulo">Observações</p>
            <p style={{ whiteSpace: 'pre-line', fontWeight: 600 }}>{e.observacoes}</p>
          </section>
        )}

        {temArte && (
          <section className="ficha-bloco ficha-arte">
            <p className="ficha-rotulo">{aprovada ? `Arte aprovada · versão ${aprovada.numero}` : 'Arte'}</p>
            {!aprovada && (
              <p className="ficha-alerta"><Icone nome="alerta" tamanho={18} />
                {ultima ? `A arte ainda NÃO foi aprovada (versão ${ultima.numero} com a cliente). Não produzir antes da aprovação.` : 'A arte ainda não foi feita. Não produzir antes da aprovação.'}
              </p>
            )}
            {arte && <div className="ficha-arte-img"><ImagemProtegida url={arte.url} alt={`Arte versão ${arte.numero}`} /></div>}
            {aprovada?.comentarioCliente && <p style={{ fontWeight: 600 }}>Recado da cliente: “{aprovada.comentarioCliente}”</p>}
            {aprovada && <p className="ficha-pequeno">Aprovada {aprovada.respondidaNoSistema ? '(marcado no sistema)' : `por ${primeiroNome(e.clienteNome)} pelo link`} em {dataHora(aprovada.respondidoEm)}.</p>}
          </section>
        )}

        {referencias.length > 0 && (
          <section className="ficha-bloco">
            <p className="ficha-rotulo">Fotos de referência</p>
            <div className="ficha-refs">
              {referencias.slice(0, 6).map((f) => <div key={f.id}><ImagemProtegida url={f.url} alt={f.legenda || 'Referência'} /></div>)}
            </div>
          </section>
        )}

        {ficha?.materiais?.length > 0 && (
          <section className="ficha-bloco">
            <p className="ficha-rotulo">Materiais da ficha × {e.quantidade}</p>
            <table className="ficha-tabela">
              <thead><tr><th>Material</th><th>Por peça</th><th>Total</th><th aria-label="Separado">✓</th></tr></thead>
              <tbody>
                {ficha.materiais.map((m) => (
                  <tr key={m.materialId}>
                    <td>{m.materialNome}</td>
                    <td>{qtdUn(m.quantidade, m.unidade)}</td>
                    <td><strong>{qtdUn(Number(m.quantidade) * e.quantidade, m.unidade)}</strong></td>
                    <td><span className="ficha-caixa" /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>
        )}

        <section className="ficha-bloco ficha-duas">
          <div>
            <p className="ficha-rotulo">Antes de entregar</p>
            <ul className="ficha-check">
              {['Nomes, frases e datas conferidos com a arte', 'Cores e tema iguais aos aprovados', 'Acabamento e limpeza', 'Foto da peça pronta no sistema', 'Embalagem e cartão'].map((t) => (
                <li key={t}><span className="ficha-caixa" />{t}</li>
              ))}
            </ul>
          </div>
          <div>
            <p className="ficha-rotulo">Anotações</p>
            <div className="ficha-linhas"><span /><span /><span /><span /><span /></div>
          </div>
        </section>
      </article>
    </div>
  );
}
