import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, sessao } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Campo, Confirmar, Estado, Modal, ModalForm, lerForm, useCarregar, useToast, Vazio } from '../componentes/ui.jsx';
import { FRENTES, calcularFicha, moeda, moedaUnit, qtd, qtdUn } from '../util.js';
import { Topo } from './Layout.jsx';

const FILTROS = [['TODAS', 'Todas'], ['PRONTA', 'Pronta-entrega'], ['CATALOGO', 'No catálogo do site'], ['INATIVAS', 'Inativas']];

export default function Produtos() {
  const [filtro, setFiltro] = useState('TODAS');
  const [ficha, setFicha] = useState(null);
  const [produzir, setProduzir] = useState(null);
  const [excluir, setExcluir] = useState(null);
  const [catalogo, setCatalogo] = useState(null);
  const toast = useToast();
  const ajudante = sessao.ajudante();
  const produtos = useCarregar(() => api.get('/api/produtos'));
  const prec = useCarregar(() => (ajudante ? Promise.resolve({ configurada: true }) : api.get('/api/configuracoes/precificacao')));

  const lista = (produtos.dados || []).filter((p) =>
    filtro === 'INATIVAS' ? !p.ativo : p.ativo && (filtro === 'TODAS' || (filtro === 'CATALOGO' ? p.catalogo.noCatalogo : p.prontaEntrega)));

  return (
    <>
      <Topo titulo="Produtos">
        <Link to="/admin/vendas?nova=1" className="btn btn-claro"><Icone nome="sacola" tamanho={18} />Nova venda</Link>
        {!ajudante && <button type="button" className="btn btn-terra" onClick={() => setFicha({})}><Icone nome="mais" tamanho={18} largura={3} />Nova peça</button>}
      </Topo>
      <p className="mudo forte" style={{ marginTop: -10 }}>
        {ajudante
          ? 'Terminou peças de pronta-entrega? Toque em "Produzi": elas entram no estoque e os materiais saem sozinhos.'
          : <>Cada peça tem a sua ficha técnica: materiais, horas e embalagem. O sistema calcula o custo e sugere o preço.
            Peças de pronta-entrega também têm estoque para vender no ateliê e nas feiras.</>}
      </p>

      {prec.dados && !prec.dados.configurada && (
        <div className="aviso aviso-info" style={{ display: 'flex', flexWrap: 'wrap', gap: 12, alignItems: 'center' }}>
          <span style={{ flex: '1 1 300px' }}>Configure quanto vale a sua hora para o preço sugerido incluir o seu trabalho.</span>
          <Link to="/admin/configuracoes" className="btn btn-claro btn-p">Configurar agora</Link>
        </div>
      )}

      <div className="chips" role="group" aria-label="Filtrar peças" style={{ alignSelf: 'flex-start' }}>
        {FILTROS.map(([v, r]) => <button key={v} type="button" className="chip" aria-pressed={filtro === v} onClick={() => setFiltro(v)}>{r}</button>)}
      </div>

      <Estado carregando={produtos.carregando || prec.carregando} erro={produtos.erro || prec.erro}>
        {lista.length === 0 ? (
          <div className="cartao"><Vazio>
            {filtro === 'INATIVAS' ? 'Nenhuma peça inativa.' : 'Nenhuma peça cadastrada ainda. Comece pelas que você mais faz: a caderneta, a caneca, o kit professor…'}
          </Vazio></div>
        ) : (
          <div className="grade-fichas">
            {lista.map((p) => (ajudante
              ? <CartaoProducao key={p.id} p={p} aoProduzir={() => setProduzir(p)} />
              : <CartaoProduto key={p.id} p={p} aoEditar={() => setFicha(p)} aoProduzir={() => setProduzir(p)} aoExcluir={() => setExcluir(p)} aoCatalogo={() => setCatalogo(p)} />))}
          </div>
        )}
      </Estado>

      {ficha && prec.dados && (
        <FichaForm produto={ficha} prec={prec.dados} aoFechar={() => setFicha(null)}
          aoSalvar={() => { setFicha(null); toast('Ficha salva.'); produtos.recarregar(); }} />
      )}

      {produzir && (
        <ModalForm titulo={`Produzi: ${produzir.nome}`} textoSalvar="Somar ao estoque" aoFechar={() => setProduzir(null)} aoSalvar={async (fd) => {
          const d = lerForm(fd, ['quantidade']);
          await api.post(`/api/produtos/${produzir.id}/producao`, d);
          toast(`${d.quantidade} ${d.quantidade === 1 ? 'peça somada' : 'peças somadas'} ao estoque.`);
          setProduzir(null);
          produtos.recarregar();
        }}>
          <Campo rotulo="Quantas peças ficaram prontas?" nome="quantidade" tipo="number" min="1" step="1" defaultValue={1} required inteiro />
          <p className="inteiro mudo forte" style={{ fontSize: 14 }}>
            {produzir.materiais.length
              ? `Sai do estoque, por peça: ${produzir.materiais.map((m) => `${m.materialNome} (${qtdUn(m.quantidade, m.unidade)})`).join(', ')}.`
              : 'Esta ficha não tem materiais: só o estoque de peças prontas muda.'}
          </p>
        </ModalForm>
      )}

      {catalogo && <CatalogoPeca p={catalogo} aoFechar={() => { setCatalogo(null); produtos.recarregar(); }} />}

      {excluir && <Confirmar titulo="Excluir peça" perigo textoBotao="Excluir" texto={`Excluir a ficha "${excluir.nome}"?`}
        aoFechar={() => setExcluir(null)}
        aoConfirmar={async () => { await api.del(`/api/produtos/${excluir.id}`); toast('Peça excluída.'); setExcluir(null); produtos.recarregar(); }} />}
    </>
  );
}

/** Cartão da ajudante: o que vai em cada peça e quantas estão prontas, sem custos. */
function CartaoProducao({ p, aoProduzir }) {
  return (
    <article className="cartao" style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
        <span className="selo selo-areia">{FRENTES[p.frente]}</span>
        {p.prontaEntrega && <span className={`selo ${p.estoque > 0 ? 'selo-oliva' : 'selo-perigo'}`}>{p.estoque > 0 ? `${p.estoque} ${p.estoque === 1 ? 'pronta' : 'prontas'}` : 'sem peças prontas'}</span>}
      </div>
      <h2 style={{ fontSize: 21 }}>{p.nome}</h2>
      <p className="mudo forte" style={{ fontSize: 14 }}>
        {p.materiais.length ? `Por peça: ${p.materiais.map((m) => `${m.materialNome} (${qtdUn(m.quantidade, m.unidade)})`).join(', ')}` : 'Sem materiais na ficha.'}
        {Number(p.horasMaoObra) > 0 ? ` · cerca de ${qtd(p.horasMaoObra)} h` : ''}
      </p>
      {p.prontaEntrega && p.ativo && <button type="button" className="btn btn-oliva btn-p" style={{ alignSelf: 'flex-start', marginTop: 'auto' }} onClick={aoProduzir}><Icone nome="mais" tamanho={16} largura={3} />Produzi</button>}
    </article>
  );
}

/** Como a peça aparece no catálogo do site: liga/desliga, texto, preço, prazo e fotos. */
function CatalogoPeca({ p, aoFechar }) {
  const [info, setInfo] = useState(p.catalogo);
  const [erro, setErro] = useState(null);
  const [enviando, setEnviando] = useState(false);
  const toast = useToast();

  async function fotos(ev) {
    const lista = [...ev.target.files];
    ev.target.value = '';
    setErro(null);
    setEnviando(true);
    try {
      let atual = info;
      for (const f of lista) {
        if (f.size > 8 * 1024 * 1024) throw new Error(`${f.name} tem mais de 8 MB.`);
        const fd = new FormData();
        fd.append('foto', f);
        atual = await api.enviar(`/api/produtos/${p.id}/fotos`, fd);
        setInfo(atual);
      }
      toast(lista.length === 1 ? 'Foto enviada.' : 'Fotos enviadas.');
    } catch (e) { setErro(e.message); } finally { setEnviando(false); }
  }

  async function acaoFoto(fn) {
    setErro(null);
    try { setInfo(await fn()); } catch (e) { setErro(e.message); }
  }

  return (
    <ModalForm titulo={`Catálogo: ${p.nome}`} largo textoSalvar="Salvar catálogo" aoFechar={aoFechar} aoSalvar={async (fd) => {
      const d = lerForm(fd, ['ordemCatalogo']);
      d.noCatalogo = fd.get('noCatalogo') === 'on';
      d.mostrarPreco = fd.get('mostrarPreco') === 'on';
      await api.put(`/api/produtos/${p.id}/catalogo`, d);
      toast(d.noCatalogo ? 'Peça no catálogo do site.' : 'Peça fora do catálogo.');
      aoFechar();
    }}>
      <label className="checkbox inteiro"><input type="checkbox" name="noCatalogo" defaultChecked={info.noCatalogo} /> Mostrar esta peça no catálogo do site</label>
      <Campo rotulo="Texto para a cliente" nome="textoCatalogo" linhas={4} defaultValue={info.textoCatalogo} maxLength={2000} inteiro
        placeholder="Capa dura personalizada com nome e tema, 80 folhas pautadas, wire-o…" dica="A descrição da ficha técnica continua só no sistema." />
      <Campo rotulo="Prazo de produção" nome="prazoProducao" defaultValue={info.prazoProducao} maxLength={80} placeholder="Até 15 dias" />
      <Campo rotulo="Ordem no catálogo" nome="ordemCatalogo" tipo="number" step="1" defaultValue={info.ordemCatalogo} />
      <label className="checkbox inteiro"><input type="checkbox" name="mostrarPreco" defaultChecked={info.mostrarPreco} />
        Mostrar o preço ({moeda(p.custo.precoAtual)}{p.prontaEntrega ? '' : ', como "a partir de"'}). Desmarcado, aparece "sob consulta".</label>
      <div className="inteiro" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
          <p className="forte">Fotos ({info.fotos.length} de 8) · a primeira é a capa</p>
          <label className="btn btn-claro btn-p botao-upload"><Icone nome="foto" tamanho={16} />{enviando ? 'Enviando…' : 'Adicionar fotos'}
            <input type="file" accept="image/jpeg,image/png,image/webp" multiple onChange={fotos} disabled={enviando || info.fotos.length >= 8} aria-label="Fotos da peça" /></label>
        </div>
        {erro && <div className="aviso aviso-erro" role="alert">{erro}</div>}
        {info.fotos.length === 0 ? <p className="mudo forte" style={{ fontSize: 14 }}>Sem fotos, a peça aparece com um fundo neutro. Uma boa foto vende muito mais!</p> : (
          <div className="fotos-produto">
            {info.fotos.map((f, i) => (
              <figure key={f.id}>
                <img src={f.url} alt={`Foto ${i + 1} de ${p.nome}`} />
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 4 }}>
                  {i === 0 ? <span className="selo selo-oliva" style={{ fontSize: 12 }}>capa</span>
                    : <button type="button" className="btn btn-fantasma btn-p" style={{ padding: '0 8px', minHeight: 32 }} onClick={() => acaoFoto(() => api.post(`/api/fotos-produto/${f.id}/capa`))}>Capa</button>}
                  <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Excluir foto ${i + 1}`} onClick={() => acaoFoto(() => api.del(`/api/fotos-produto/${f.id}`))}><Icone nome="lixo" tamanho={16} /></button>
                </div>
              </figure>
            ))}
          </div>
        )}
      </div>
    </ModalForm>
  );
}

function CartaoProduto({ p, aoEditar, aoProduzir, aoExcluir, aoCatalogo }) {
  const c = p.custo;
  const prejuizo = Number(c.lucroUnitario) < 0;
  return (
    <article className="cartao" style={{ display: 'flex', flexDirection: 'column', gap: 12, opacity: p.ativo ? 1 : 0.7 }}>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
        <span className="selo selo-areia">{FRENTES[p.frente]}</span>
        {p.prontaEntrega && (
          <span className={`selo ${p.estoque > 0 ? 'selo-oliva' : 'selo-perigo'}`}>
            {p.estoque > 0 ? `${p.estoque} ${p.estoque === 1 ? 'pronta' : 'prontas'}` : 'sem peças prontas'}
          </span>
        )}
        {!p.ativo && <span className="selo selo-areia">inativa</span>}
        {p.catalogo.noCatalogo && <span className="selo selo-terra"><Icone nome="site" tamanho={13} />no site</span>}
      </div>
      <h2 style={{ fontSize: 21 }}>{p.nome}</h2>
      <div className="preco-linha">
        <div><span>Custo</span><strong>{moeda(c.custoTotal)}</strong></div>
        <div><span>Sugerido</span><strong>{moeda(c.precoSugerido)}</strong></div>
        <div className="destaque"><span>Você cobra</span><strong>{moeda(c.precoAtual)}</strong></div>
      </div>
      <p className="forte" style={{ fontSize: 14, color: prejuizo ? 'var(--perigo)' : 'var(--oliva-texto)' }}>
        {prejuizo ? `Prejuízo de ${moeda(-c.lucroUnitario)} por peça` : `Lucro de ${moeda(c.lucroUnitario)} por peça`}
        {c.ganhoPorHora != null && ` · você ganha ${moeda(c.ganhoPorHora)}/h`}
      </p>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 'auto' }}>
        <button type="button" className="btn btn-claro btn-p" onClick={aoEditar}><Icone nome="editar" tamanho={16} />Ficha</button>
        <button type="button" className="btn btn-claro btn-p" onClick={aoCatalogo}><Icone nome="site" tamanho={16} />Catálogo</button>
        {p.prontaEntrega && p.ativo && <button type="button" className="btn btn-oliva btn-p" onClick={aoProduzir}><Icone nome="mais" tamanho={16} largura={3} />Produzi</button>}
        <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Excluir ${p.nome}`} onClick={aoExcluir}><Icone nome="lixo" tamanho={18} /></button>
      </div>
    </article>
  );
}

function FichaForm({ produto: p, prec, aoFechar, aoSalvar }) {
  const materiais = useCarregar(() => api.get('/api/materiais'));
  const [f, setF] = useState({
    nome: p.nome || '', descricao: p.descricao || '', frente: p.frente || 'ENCOMENDA',
    horas: p.horasMaoObra ?? '', outros: p.outrosCustos ?? '', margem: p.margem ?? '', precoVenda: p.precoVenda ?? '',
    prontaEntrega: p.prontaEntrega ?? false, estoque: p.estoque ?? 0, ativo: p.ativo ?? true,
    itens: (p.materiais || []).map((m) => ({ materialId: String(m.materialId), quantidade: String(m.quantidade) })),
  });
  const [erro, setErro] = useState(null);
  const [salvando, setSalvando] = useState(false);
  const set = (k) => (e) => setF((x) => ({ ...x, [k]: e.target.type === 'checkbox' ? e.target.checked : e.target.value }));

  const porId = useMemo(() => Object.fromEntries((materiais.dados || []).map((m) => [String(m.id), m])), [materiais.dados]);
  const c = calcularFicha(f, porId, prec);

  function mudarItem(i, k, v) {
    setF((x) => ({ ...x, itens: x.itens.map((it, j) => (j === i ? { ...it, [k]: v } : it)) }));
  }

  async function salvar(e) {
    e.preventDefault();
    setErro(null);
    const itens = f.itens.filter((i) => i.materialId);
    if (itens.some((i) => !(Number(i.quantidade) > 0))) { setErro('Informe a quantidade de cada material da receita.'); return; }
    setSalvando(true);
    const num = (v) => (v === '' || v == null ? null : Number(String(v).replace(',', '.')));
    const corpo = {
      nome: f.nome, descricao: f.descricao || null, frente: f.frente, horasMaoObra: num(f.horas) ?? 0,
      outrosCustos: num(f.outros) ?? 0, margem: num(f.margem), precoVenda: num(f.precoVenda),
      prontaEntrega: f.prontaEntrega, estoque: f.prontaEntrega ? num(f.estoque) ?? 0 : 0, ativo: f.ativo,
      materiais: itens.map((i) => ({ materialId: Number(i.materialId), quantidade: num(i.quantidade) })),
    };
    try {
      if (p.id) await api.put(`/api/produtos/${p.id}`, corpo);
      else await api.post('/api/produtos', corpo);
      aoSalvar();
    } catch (err) {
      setErro(err.message);
      setSalvando(false);
    }
  }

  const usados = new Set(f.itens.map((i) => i.materialId));

  return (
    <Modal titulo={p.id ? `Ficha: ${p.nome}` : 'Nova peça'} aoFechar={aoFechar} largo>
      <form onSubmit={salvar} noValidate className="ficha">
        <div className="ficha-campos">
          {erro && <div className="aviso aviso-erro" role="alert">{erro}</div>}
          <div className="form-grade">
            <div className="campo inteiro">
              <label htmlFor="f-nome">Nome da peça</label>
              <input id="f-nome" className="entrada" value={f.nome} onChange={set('nome')} required maxLength={140} placeholder="Caderneta A5 personalizada" />
            </div>
            <div className="campo">
              <label htmlFor="f-frente">Frente</label>
              <select id="f-frente" className="entrada" value={f.frente} onChange={set('frente')}>
                {Object.entries(FRENTES).map(([k, r]) => <option key={k} value={k}>{r}</option>)}
              </select>
            </div>
            <div className="campo">
              <label htmlFor="f-horas">Horas de trabalho por peça</label>
              <input id="f-horas" className="entrada" type="number" min="0" step="0.25" value={f.horas} onChange={set('horas')} placeholder="3" />
            </div>
          </div>

          <fieldset className="receita">
            <legend>Materiais de uma peça</legend>
            {materiais.dados?.length === 0 && (
              <p className="mudo forte">Cadastre primeiro os papéis, canecas e embalagens em <Link to="/admin/materiais">Materiais</Link>.</p>
            )}
            {f.itens.map((it, i) => {
              const m = porId[it.materialId];
              return (
                <div key={i} className="receita-linha">
                  <label className="sr" htmlFor={`f-mat-${i}`}>Material {i + 1}</label>
                  <select id={`f-mat-${i}`} className="entrada" value={it.materialId} onChange={(e) => mudarItem(i, 'materialId', e.target.value)}>
                    <option value="">Escolha…</option>
                    {(materiais.dados || []).map((mm) => (
                      <option key={mm.id} value={mm.id} disabled={usados.has(String(mm.id)) && it.materialId !== String(mm.id)}>
                        {mm.nome} ({moedaUnit(mm.custoUnitario)}/{mm.unidade})
                      </option>
                    ))}
                  </select>
                  <div className="receita-qtd">
                    <label className="sr" htmlFor={`f-q-${i}`}>Quantidade</label>
                    <input id={`f-q-${i}`} className="entrada" type="number" min="0" step="0.001" value={it.quantidade}
                      onChange={(e) => mudarItem(i, 'quantidade', e.target.value)} placeholder="0" />
                    <span className="mudo">{m?.unidade || ''}</span>
                  </div>
                  <span className="num forte" style={{ minWidth: 80 }}>{m ? moeda((Number(it.quantidade) || 0) * Number(m.custoUnitario)) : '—'}</span>
                  <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label="Tirar material"
                    onClick={() => setF((x) => ({ ...x, itens: x.itens.filter((_, j) => j !== i) }))}><Icone nome="fechar" tamanho={16} /></button>
                </div>
              );
            })}
            <button type="button" className="btn btn-claro btn-p" style={{ alignSelf: 'flex-start' }}
              onClick={() => setF((x) => ({ ...x, itens: [...x.itens, { materialId: '', quantidade: '' }] }))} disabled={!materiais.dados?.length}>
              <Icone nome="mais" tamanho={16} largura={3} />Adicionar material
            </button>
          </fieldset>

          <div className="form-grade">
            <div className="campo">
              <label htmlFor="f-outros">Embalagem e outros (R$ por peça)</label>
              <input id="f-outros" className="entrada" type="number" min="0" step="0.01" value={f.outros} onChange={set('outros')} placeholder="0,00" />
            </div>
            <div className="campo">
              <label htmlFor="f-margem">Margem de lucro (%)</label>
              <input id="f-margem" className="entrada" type="number" min="0" step="1" value={f.margem} onChange={set('margem')} placeholder={`${Number(prec.margemPadrao)} (padrão)`} />
            </div>
            <div className="campo inteiro">
              <label htmlFor="f-preco">Preço que você cobra (R$)</label>
              <input id="f-preco" className="entrada" type="number" min="0" step="0.01" value={f.precoVenda} onChange={set('precoVenda')} placeholder={`Deixe vazio para usar o sugerido (${moeda(c.precoSugerido)})`} />
            </div>
            <label className="checkbox"><input type="checkbox" checked={f.prontaEntrega} onChange={set('prontaEntrega')} /> Vendo como pronta-entrega</label>
            {f.prontaEntrega && (
              <div className="campo">
                <label htmlFor="f-estoque">Peças prontas agora</label>
                <input id="f-estoque" className="entrada" type="number" min="0" step="1" value={f.estoque} onChange={set('estoque')} />
                <span className="dica">Contagem inicial. Depois use "Produzi" para somar e baixar os materiais.</span>
              </div>
            )}
            <div className="campo inteiro">
              <label htmlFor="f-desc">Observações da ficha</label>
              <textarea id="f-desc" className="entrada" rows={2} value={f.descricao} onChange={set('descricao')} placeholder="Nós usados, acabamento, variações de cor…" />
            </div>
            <label className="checkbox inteiro"><input type="checkbox" checked={f.ativo} onChange={set('ativo')} /> Ativa (aparece nas listas)</label>
          </div>
        </div>

        <aside className="composicao" aria-label="Composição do preço" aria-live="polite">
          <p className="forte" style={{ fontSize: 18 }}>Composição do preço</p>
          <dl>
            <div><dt>Materiais</dt><dd>{moeda(c.custoMateriais)}</dd></div>
            <div><dt>Sua mão de obra <small>{qtd(f.horas || 0)} h × {moeda(prec.valorHora)}</small></dt><dd>{moeda(c.custoMaoObra)}</dd></div>
            <div><dt>Custos fixos <small>{qtd(f.horas || 0)} h × {moeda(prec.custoFixoHora)}</small></dt><dd>{moeda(c.custoFixo)}</dd></div>
            <div><dt>Embalagem e outros</dt><dd>{moeda(c.outrosCustos)}</dd></div>
            <div className="total"><dt>Custo total</dt><dd>{moeda(c.custoTotal)}</dd></div>
            <div><dt>Margem</dt><dd>{qtd(c.margem)}%</dd></div>
            <div className="sugerido"><dt>Preço sugerido</dt><dd>{moeda(c.precoSugerido)}</dd></div>
          </dl>
          <div className={`resultado ${c.lucroUnitario < 0 ? 'ruim' : ''}`}>
            <p>Cobrando {moeda(c.precoAtual)}</p>
            <strong>{c.lucroUnitario < 0 ? `Prejuízo de ${moeda(-c.lucroUnitario)}` : `Lucro de ${moeda(c.lucroUnitario)}`}</strong>
            {c.ganhoPorHora != null && <p>Você ganha {moeda(c.ganhoPorHora)} por hora (meta: {moeda(prec.valorHora)}/h)</p>}
          </div>
          {!prec.configurada && <p className="mudo" style={{ fontSize: 13 }}>Sua hora ainda está em R$ 0,00. Ajuste em Configurações.</p>}
          <div className="modal-rodape" style={{ marginTop: 'auto' }}>
            <button type="button" className="btn btn-fantasma" onClick={aoFechar}>Cancelar</button>
            <button type="submit" className="btn btn-terra" disabled={salvando}>{salvando ? 'Salvando…' : 'Salvar ficha'}</button>
          </div>
        </aside>
      </form>
    </Modal>
  );
}
