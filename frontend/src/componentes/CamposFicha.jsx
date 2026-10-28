import { useId, useState } from 'react';
import { api } from '../api.js';
import { moeda } from '../util.js';
import { useCarregar } from './ui.jsx';

/**
 * Ficha técnica + quantidade + valor total, com o preço sugerido pela ficha.
 * Os campos têm "name", então entram no FormData do ModalForm.
 * travado = materiais já baixados: ficha e quantidade não podem mudar.
 */
export default function CamposFicha({ produtoId, quantidade, valorTotal, travado = false }) {
  const { dados: produtos } = useCarregar(() => api.get('/api/produtos'));
  const [pid, setPid] = useState(produtoId ? String(produtoId) : '');
  const [q, setQ] = useState(String(quantidade ?? 1));
  const [valor, setValor] = useState(valorTotal ?? '');
  const ids = { ficha: useId(), qtd: useId(), valor: useId() };

  const opcoes = (produtos || []).filter((p) => p.ativo || String(p.id) === pid);
  const p = (produtos || []).find((x) => String(x.id) === pid);
  const n = Math.max(1, Number(q) || 1);
  const sugerido = p ? Number(p.custo.precoAtual) * n : null;

  return (
    <>
      <div className="campo">
        <label htmlFor={ids.ficha}>Ficha técnica</label>
        {travado && <input type="hidden" name="produtoId" value={pid} />}
        <select id={ids.ficha} name={travado ? undefined : 'produtoId'} className="entrada" value={pid} disabled={travado}
          onChange={(e) => setPid(e.target.value)}>
          <option value="">Sem ficha (peça única)</option>
          {opcoes.map((o) => <option key={o.id} value={o.id}>{o.nome}</option>)}
        </select>
        {travado && <span className="dica">Materiais já baixados: para trocar, remova antes os materiais usados.</span>}
      </div>
      <div className="campo">
        <label htmlFor={ids.qtd}>Quantidade de peças</label>
        {travado && <input type="hidden" name="quantidade" value={q} />}
        <input id={ids.qtd} name={travado ? undefined : 'quantidade'} className="entrada" type="number" min="1" step="1"
          value={q} disabled={travado} onChange={(e) => setQ(e.target.value)} />
      </div>
      <div className="campo inteiro">
        <label htmlFor={ids.valor}>Valor total (R$)</label>
        <input id={ids.valor} name="valorTotal" className="entrada" type="number" min="0" step="0.01" required
          value={valor} onChange={(e) => setValor(e.target.value)} />
        {p && (
          <span className="dica" style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 8 }}>
            Pela ficha: {moeda(p.custo.precoAtual)} × {n} = <strong>{moeda(sugerido)}</strong>
            {Number(valor) !== sugerido && (
              <button type="button" className="btn btn-claro btn-p" onClick={() => setValor(sugerido.toFixed(2))}>Usar este valor</button>
            )}
            {valor !== '' && Number(valor) < Number(p.custo.custoTotal) * n && (
              <span className="erro-texto">Abaixo do custo ({moeda(Number(p.custo.custoTotal) * n)})</span>
            )}
          </span>
        )}
      </div>
    </>
  );
}
