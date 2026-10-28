import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Campo, Confirmar, Estado, ModalForm, lerForm, useCarregar, useToast, Vazio } from '../componentes/ui.jsx';
import { FORMAS, data, hojeIso, moeda } from '../util.js';
import { Abas, Topo } from './Layout.jsx';

const MESES = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'];

const SITUACAO = {
  PAGA: ['Paga', 'oliva', 'var(--oliva-claro)', 'var(--oliva-texto)'],
  VENCIDA: ['Vencida', 'perigo', 'var(--perigo-claro)', 'var(--perigo)'],
  HOJE: ['Vence hoje', 'terra', 'var(--terra-claro)', 'var(--terra-texto)'],
  PROXIMA: ['Vence logo', 'ocre', 'var(--ocre-claro)', 'var(--ocre-texto)'],
  A_VENCER: ['A vencer', 'areia', 'var(--areia)', 'var(--marrom-2)'],
};

export const ABAS_FINANCEIRO = [['/admin/financeiro', 'Lançamentos', 'financeiro'], ['/admin/financeiro/contas', 'Contas fixas', 'recibo']];

/** Contas que se repetem todo mês. Pagar lança a despesa no financeiro, ligada ao mês. */
export default function ContasFixas() {
  const hoje = new Date();
  const [ref, setRef] = useState({ ano: hoje.getFullYear(), mes: hoje.getMonth() + 1 });
  const [form, setForm] = useState(null);
  const [pagar, setPagar] = useState(null);
  const [desfazer, setDesfazer] = useState(null);
  const [excluir, setExcluir] = useState(null);
  const toast = useToast();
  const { dados: r, erro, carregando, recarregar } = useCarregar(
    () => api.get(`/api/contas-fixas?ano=${ref.ano}&mes=${ref.mes}`), [ref.ano, ref.mes]);

  function mover(delta) {
    const d = new Date(ref.ano, ref.mes - 1 + delta, 1);
    setRef({ ano: d.getFullYear(), mes: d.getMonth() + 1 });
  }

  return (
    <>
      <Topo titulo="Contas fixas">
        <div className="chips" style={{ alignItems: 'center' }}>
          <button type="button" className="chip" onClick={() => mover(-1)} aria-label="Mês anterior"><Icone nome="seta" tamanho={18} style={{ transform: 'rotate(180deg)' }} /></button>
          <span className="forte" style={{ padding: '0 8px', minWidth: 150, textAlign: 'center' }} aria-live="polite">{MESES[ref.mes - 1]} {ref.ano}</span>
          <button type="button" className="chip" onClick={() => mover(1)} aria-label="Próximo mês"><Icone nome="seta" tamanho={18} /></button>
        </div>
        <button type="button" className="btn btn-terra" onClick={() => setForm({})}><Icone nome="mais" tamanho={18} largura={3} />Nova conta</button>
      </Topo>
      <Abas itens={ABAS_FINANCEIRO} />

      <Estado carregando={carregando} erro={erro}>
        {r && (
          <>
            <section className="grade-resumo" aria-label="Resumo do mês">
              {[['Previsto no mês', r.previsto, 'var(--areia)', 'var(--marrom)'],
                ['Já pago', r.pago, 'var(--oliva-claro)', 'var(--oliva-texto)'],
                ['Falta pagar', r.falta, r.vencidas > 0 ? 'var(--perigo-claro)' : 'var(--ocre-claro)', r.vencidas > 0 ? 'var(--perigo)' : 'var(--ocre-texto)',
                  r.vencidas > 0 ? `${r.vencidas} ${r.vencidas === 1 ? 'conta vencida' : 'contas vencidas'}` : null]].map(([rot, v, f, c, extra]) => (
                <div key={rot} className="resumo" style={{ background: f }}>
                  <p className="rotulo" style={{ marginTop: 0 }}>{rot}</p>
                  <p className="valor" style={{ color: c, fontSize: 28 }}>{moeda(v)}</p>
                  {extra && <p className="extra" style={{ color: c }}>{extra}</p>}
                </div>
              ))}
            </section>

            <section className="cartao" aria-labelledby="t-contas">
              <div className="cartao-titulo"><h2 id="t-contas">Contas de {MESES[ref.mes - 1].toLowerCase()}</h2></div>
              {r.contas.length === 0 ? (
                <Vazio>Cadastre aluguel, energia, internet, MEI… O sistema avisa no painel quando estiverem para vencer.</Vazio>
              ) : (
                <div>
                  {r.contas.map((l) => {
                    const [rotulo, selo, fundo, cor] = SITUACAO[l.situacao];
                    const dia = Number(l.vencimento.slice(8, 10));
                    return (
                      <div key={l.conta.id} className="conta-linha">
                        <div className="dia" style={{ background: fundo, color: cor }} aria-hidden="true">{dia}<small>{MESES[ref.mes - 1].slice(0, 3)}</small></div>
                        <div className="info">
                          <p>{l.conta.nome}{!l.conta.entraNaPrecificacao && <span className="selo selo-areia" style={{ marginLeft: 8, fontSize: 12 }}>fora da precificação</span>}</p>
                          <p>
                            {l.situacao === 'PAGA'
                              ? `Paga em ${data(l.pagoEm)} · ${FORMAS[l.formaPagamento]}${Number(l.valorPago) !== Number(l.conta.valor) ? ` · previsto ${moeda(l.conta.valor)}` : ''}`
                              : `Vence em ${data(l.vencimento)}`}
                            {l.conta.observacoes ? ` · ${l.conta.observacoes}` : ''}
                          </p>
                        </div>
                        <span className={`selo selo-${selo}`}>{rotulo}</span>
                        <span className="valor">{moeda(l.valorPago ?? l.conta.valor)}</span>
                        <div className="acoes">
                          {l.situacao === 'PAGA'
                            ? <button type="button" className="btn btn-fantasma btn-p" onClick={() => setDesfazer(l)}>Desfazer</button>
                            : <button type="button" className="btn btn-oliva btn-p" onClick={() => setPagar(l)}><Icone nome="check" tamanho={16} largura={3} />Paguei</button>}
                          <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Editar ${l.conta.nome}`} onClick={() => setForm(l.conta)}><Icone nome="editar" tamanho={18} /></button>
                          <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Excluir ${l.conta.nome}`} onClick={() => setExcluir(l.conta)}><Icone nome="lixo" tamanho={18} /></button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </section>

            <section className="cartao" aria-labelledby="t-prec" style={{ background: 'var(--oliva-menu)' }}>
              <div className="cartao-titulo"><h2 id="t-prec">Na precificação</h2><Link to="/admin/configuracoes#precificacao" className="btn btn-claro btn-p">Abrir precificação</Link></div>
              <p style={{ fontWeight: 700 }}>
                As contas marcadas somam <strong>{moeda(r.totalPrecificacao)}</strong> por mês.{' '}
                {r.custosFixosAutomatico
                  ? 'A precificação já usa esse total, e se ajusta sozinha quando uma conta muda.'
                  : 'Na precificação, ligue "Usar as contas fixas" para o custo fixo por hora acompanhar estas contas.'}
              </p>
            </section>

            {r.inativas.length > 0 && (
              <section className="cartao" aria-labelledby="t-inat">
                <div className="cartao-titulo"><h2 id="t-inat">Desativadas</h2></div>
                <ul className="lista-linhas">
                  {r.inativas.map((c) => (
                    <li key={c.id}>
                      <div className="info"><p>{c.nome}</p><p>{moeda(c.valor)} · dia {c.diaVencimento}</p></div>
                      <button type="button" className="btn btn-claro btn-p" onClick={() => setForm(c)}>Editar</button>
                    </li>
                  ))}
                </ul>
              </section>
            )}
          </>
        )}
      </Estado>

      {form && (
        <ModalForm titulo={form.id ? 'Editar conta' : 'Nova conta fixa'} aoFechar={() => setForm(null)} aoSalvar={async (fd) => {
          const d = lerForm(fd, ['valor', 'diaVencimento']);
          d.entraNaPrecificacao = fd.get('entraNaPrecificacao') === 'on';
          d.ativa = fd.get('ativa') === 'on';
          if (form.id) await api.put(`/api/contas-fixas/${form.id}`, d);
          else await api.post('/api/contas-fixas', d);
          toast('Conta salva.');
          setForm(null);
          recarregar();
        }}>
          <Campo rotulo="Nome" nome="nome" defaultValue={form.nome} required maxLength={120} inteiro placeholder="Aluguel, energia, internet, MEI…" />
          <Campo rotulo="Valor por mês (R$)" nome="valor" tipo="number" min="0.01" step="0.01" defaultValue={form.valor} required
            dica="Se variar, use a média. Na hora de pagar você informa o valor do mês." />
          <Campo rotulo="Dia do vencimento" nome="diaVencimento" tipo="number" min="1" max="31" step="1" defaultValue={form.diaVencimento ?? 10} required
            dica="Dia 31 vence no último dia de meses mais curtos." />
          <Campo rotulo="Observação" nome="observacoes" defaultValue={form.observacoes} maxLength={255} inteiro placeholder="Débito automático, boleto pelo app…" />
          <label className="checkbox inteiro"><input type="checkbox" name="entraNaPrecificacao" defaultChecked={form.entraNaPrecificacao ?? true} /> Entra no custo fixo das peças (precificação)</label>
          <label className="checkbox inteiro"><input type="checkbox" name="ativa" defaultChecked={form.ativa ?? true} /> Conta ativa</label>
        </ModalForm>
      )}

      {pagar && (
        <ModalForm titulo={`Paguei: ${pagar.conta.nome}`} textoSalvar="Lançar pagamento" aoFechar={() => setPagar(null)} aoSalvar={async (fd) => {
          const d = lerForm(fd, ['valor']);
          await api.post(`/api/contas-fixas/${pagar.conta.id}/pagar`, { ...d, ano: ref.ano, mes: ref.mes });
          toast('Pagamento lançado no financeiro.');
          setPagar(null);
          recarregar();
        }}>
          <Campo rotulo="Valor pago (R$)" nome="valor" tipo="number" min="0.01" step="0.01" defaultValue={pagar.conta.valor} required />
          <Campo rotulo="Data do pagamento" nome="data" tipo="date" defaultValue={hojeIso()} required />
          <Campo rotulo="Forma" nome="formaPagamento" opcoes={FORMAS} defaultValue="PIX" />
          <p className="inteiro mudo" style={{ fontSize: 14 }}>Entra como despesa em "Contas fixas", referente a {MESES[ref.mes - 1].toLowerCase()} de {ref.ano}.</p>
        </ModalForm>
      )}

      {desfazer && <Confirmar titulo="Desfazer pagamento" textoBotao="Desfazer"
        texto={`Tirar o pagamento de ${desfazer.conta.nome} (${moeda(desfazer.valorPago)}) do financeiro? A conta volta a aparecer como não paga.`}
        aoFechar={() => setDesfazer(null)} aoConfirmar={async () => {
          await api.del(`/api/contas-fixas/${desfazer.conta.id}/pagamento?ano=${ref.ano}&mes=${ref.mes}`);
          toast('Pagamento desfeito.'); setDesfazer(null); recarregar();
        }} />}

      {excluir && <Confirmar titulo="Excluir conta" perigo textoBotao="Excluir" texto={`Excluir ${excluir.nome}? Se ela já tem pagamentos, desative em vez de excluir.`}
        aoFechar={() => setExcluir(null)} aoConfirmar={async () => { await api.del(`/api/contas-fixas/${excluir.id}`); toast('Conta excluída.'); setExcluir(null); recarregar(); }} />}
    </>
  );
}
