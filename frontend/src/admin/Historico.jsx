import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Estado, useCarregar, Vazio } from '../componentes/ui.jsx';
import { dataExtensa, hojeIso, hora } from '../util.js';
import { Topo } from './Layout.jsx';

const AREAS = [['', 'Tudo'], ['ENCOMENDA', 'Encomendas'], ['VENDA', 'Vendas'], ['FINANCEIRO', 'Financeiro'], ['ESTOQUE', 'Estoque']];
const PERIODOS = [[7, '7 dias'], [30, '30 dias'], [90, '3 meses']];
const ICONE_AREA = { ENCOMENDA: 'encomenda', VENDA: 'sacola', FINANCEIRO: 'financeiro', ESTOQUE: 'material' };

function menosDias(n) {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** Quem fez o quê: a Jô, as ajudantes e as clientes pelo link, com filtros por período, área e pessoa. */
export default function Historico() {
  const [dias, setDias] = useState(30);
  const [area, setArea] = useState('');
  const [pessoa, setPessoa] = useState('');
  const equipe = useCarregar(() => api.get('/api/equipe'));
  const { dados, erro, carregando } = useCarregar(() => {
    const q = new URLSearchParams({ de: menosDias(dias), ate: hojeIso() });
    if (area) q.set('area', area);
    if (pessoa) q.set('usuario', pessoa);
    return api.get(`/api/historico?${q}`);
  }, [dias, area, pessoa]);

  const porDia = [];
  (dados || []).forEach((h) => {
    const dia = h.criadoEm.slice(0, 10);
    const grupo = porDia[porDia.length - 1];
    if (grupo && grupo.dia === dia) grupo.itens.push(h);
    else porDia.push({ dia, itens: [h] });
  });

  return (
    <>
      <Topo titulo="Histórico" />
      <p className="mudo forte" style={{ marginTop: -8, marginBottom: 16 }}>Tudo o que mudou no sistema, com quem fez e quando. Ajuda a esclarecer depois quem mudou um status, um valor ou o estoque.</p>
      <div className="filtros">
        <div className="chips" role="group" aria-label="Período">
          {PERIODOS.map(([n, r]) => <button key={n} type="button" className="chip" aria-pressed={dias === n} onClick={() => setDias(n)}>{r}</button>)}
        </div>
        <div className="chips chips-p" role="group" aria-label="Área">
          {AREAS.map(([v, r]) => <button key={v} type="button" className="chip" aria-pressed={area === v} onClick={() => setArea(v)}>{r}</button>)}
        </div>
        <div className="campo" style={{ minWidth: 200 }}>
          <label htmlFor="h-pessoa" className="sr">Quem fez</label>
          <select id="h-pessoa" className="entrada" value={pessoa} onChange={(e) => setPessoa(e.target.value)}>
            <option value="">Todas as pessoas</option>
            {(equipe.dados || []).map((u) => <option key={u.id} value={u.id}>{u.nome}</option>)}
            <option value="-1">Clientes (pelo link)</option>
          </select>
        </div>
      </div>
      <section className="cartao">
        <Estado carregando={carregando} erro={erro}>
          {!dados?.length ? <Vazio>Nada registrado neste período.</Vazio> : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 22 }}>
              {porDia.map((g) => (
                <div key={g.dia}>
                  <h2 style={{ fontSize: 16, marginBottom: 10, textTransform: 'capitalize' }}>{g.dia === hojeIso() ? 'Hoje' : dataExtensa(g.dia)}</h2>
                  <ol className="linha-tempo">
                    {g.itens.map((h) => (
                      <li key={h.id} className={h.cliente ? 'da-cliente' : ''}>
                        <p className="lt-cabeca">
                          <span className="mudo">{hora(h.criadoEm)}</span>
                          <strong>{h.autor}</strong>
                          <span className="selo selo-areia selo-p"><Icone nome={ICONE_AREA[h.area]} tamanho={12} />{AREAS.find((a) => a[0] === h.area)?.[1]}</span>
                        </p>
                        <p className="lt-texto">
                          {h.encomendaId && <Link to={`/admin/encomendas?abrir=${h.encomendaId}`} style={{ fontWeight: 800 }}>{h.encomenda}</Link>}
                          {h.encomendaId && ': '}{h.descricao}
                        </p>
                      </li>
                    ))}
                  </ol>
                </div>
              ))}
              {dados.length >= 500 && <p className="mudo forte">Mostrando as 500 ações mais recentes. Diminua o período para ver o resto.</p>}
            </div>
          )}
        </Estado>
      </section>
    </>
  );
}
