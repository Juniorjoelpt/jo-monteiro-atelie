import { useState } from 'react';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Campo, Confirmar, Estado, Modal, ModalForm, Selo, lerForm, useCarregar, useToast, Vazio } from '../componentes/ui.jsx';
import { STATUS_ENCOMENDA, STATUS_ORCAMENTO, TIPOS_CLIENTE, data, linkWhats, moeda, telefone } from '../util.js';
import { Topo } from './Layout.jsx';
import { Info } from './Orcamentos.jsx';

export default function Clientes() {
  const [busca, setBusca] = useState('');
  const [termo, setTermo] = useState('');
  const [form, setForm] = useState(null);
  const [aberta, setAberta] = useState(null);
  const toast = useToast();
  const { dados, erro, carregando, recarregar } = useCarregar(
    () => api.get(`/api/clientes${termo ? `?busca=${encodeURIComponent(termo)}` : ''}`), [termo]);

  return (
    <>
      <Topo titulo="Clientes">
        <form className="busca" role="search" onSubmit={(e) => { e.preventDefault(); setTermo(busca.trim()); }}>
          <Icone nome="busca" tamanho={20} cor="var(--marrom-3)" />
          <label htmlFor="busca-cliente" className="sr">Buscar cliente</label>
          <input id="busca-cliente" type="search" placeholder="Buscar por nome ou WhatsApp…" value={busca}
            onChange={(e) => { setBusca(e.target.value); if (!e.target.value) setTermo(''); }} />
        </form>
        <button type="button" className="btn btn-terra" onClick={() => setForm({})}><Icone nome="mais" tamanho={18} largura={3} />Nova cliente</button>
      </Topo>
      <section className="cartao">
        <Estado carregando={carregando} erro={erro}>
          {dados?.length === 0 ? <Vazio>{termo ? 'Ninguém encontrado com essa busca.' : 'Nenhuma cliente ainda. Elas entram aqui ao aprovar orçamentos e registrar encomendas.'}</Vazio> : (
            <div className="tabela-caixa">
              <table className="tabela">
                <thead><tr><th>Nome</th><th>WhatsApp</th><th>Tipo</th><th>Cidade</th><th>Aniversário</th></tr></thead>
                <tbody>
                  {dados?.map((c) => (
                    <tr key={c.id} className="clicavel" tabIndex={0} onClick={() => setAberta(c.id)} onKeyDown={(e) => e.key === 'Enter' && setAberta(c.id)}>
                      <td><strong>{c.nome}</strong></td>
                      <td>{telefone(c.whatsapp)}</td>
                      <td><span className="selo selo-areia">{TIPOS_CLIENTE[c.tipo]}</span></td>
                      <td>{c.cidade || '—'}</td>
                      <td>{c.aniversario ? data(c.aniversario).slice(0, 5) : '—'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Estado>
      </section>

      {aberta && <Detalhe id={aberta} aoFechar={() => setAberta(null)} aoEditar={(c) => { setAberta(null); setForm(c); }}
        aoExcluir={() => { setAberta(null); recarregar(); }} />}

      {form && (
        <ModalForm titulo={form.id ? 'Editar cliente' : 'Nova cliente'} aoFechar={() => setForm(null)} aoSalvar={async (fd) => {
          const d = lerForm(fd);
          if (form.id) await api.put(`/api/clientes/${form.id}`, d);
          else await api.post('/api/clientes', d);
          toast('Cliente salva.');
          setForm(null);
          recarregar();
        }}>
          <Campo rotulo="Nome" nome="nome" defaultValue={form.nome} required maxLength={140} inteiro />
          <Campo rotulo="WhatsApp" nome="whatsapp" defaultValue={form.whatsapp ? telefone(form.whatsapp) : ''} inputMode="tel" placeholder="(98) 90000-0000" />
          <Campo rotulo="Tipo" nome="tipo" opcoes={TIPOS_CLIENTE} defaultValue={form.tipo || 'CLIENTE'} />
          <Campo rotulo="E-mail" nome="email" tipo="email" defaultValue={form.email} maxLength={160} />
          <Campo rotulo="Cidade" nome="cidade" defaultValue={form.cidade} maxLength={120} />
          <Campo rotulo="Aniversário" nome="aniversario" tipo="date" defaultValue={form.aniversario} dica="Gera o lembrete de parabéns no dia." />
          <Campo rotulo="CPF ou CNPJ" nome="documento" defaultValue={form.documento} maxLength={30} dica="Usado no contrato (opcional)." />
          <Campo rotulo="Endereço" nome="endereco" defaultValue={form.endereco} maxLength={255} inteiro dica="Usado no contrato (opcional)." />
          <Campo rotulo="Observações" nome="observacoes" linhas={3} defaultValue={form.observacoes} inteiro placeholder="Preferências, escola em que trabalha, como conheceu o ateliê…" />
        </ModalForm>
      )}
    </>
  );
}

function Detalhe({ id, aoFechar, aoEditar, aoExcluir }) {
  const { dados: d, erro, carregando } = useCarregar(() => api.get(`/api/clientes/${id}`));
  const [excluir, setExcluir] = useState(false);
  const toast = useToast();
  const c = d?.cliente;

  if (excluir) {
    return <Confirmar titulo="Excluir cliente" perigo textoBotao="Excluir" texto={`Excluir ${c.nome}? Só é possível se não houver histórico.`}
      aoFechar={() => setExcluir(false)} aoConfirmar={async () => { await api.del(`/api/clientes/${id}`); toast('Cliente excluída.'); aoExcluir(); }} />;
  }

  return (
    <Modal titulo={c ? c.nome : 'Cliente'} aoFechar={aoFechar} largo rodape={c && <>
      <button type="button" className="btn btn-perigo" onClick={() => setExcluir(true)}><Icone nome="lixo" tamanho={18} />Excluir</button>
      <button type="button" className="btn btn-claro" onClick={() => aoEditar(c)}><Icone nome="editar" tamanho={18} />Editar</button>
      {c.whatsapp && <a className="btn btn-oliva" href={linkWhats(c.whatsapp, `Olá, ${c.nome.split(' ')[0]}! Aqui é a Jô, do ateliê.`)} target="_blank" rel="noopener noreferrer"><Icone nome="whats" tamanho={18} />WhatsApp</a>}
    </>}>
      <Estado carregando={carregando} erro={erro}>
        {d && (
          <>
            <div className="form-grade">
              <Info rotulo="WhatsApp" valor={telefone(c.whatsapp)} />
              <Info rotulo="Tipo" valor={TIPOS_CLIENTE[c.tipo]} />
              <Info rotulo="E-mail" valor={c.email || '—'} />
              <Info rotulo="Cidade" valor={c.cidade || '—'} />
              <Info rotulo="Aniversário" valor={c.aniversario ? data(c.aniversario) : '—'} />
              {c.documento && <Info rotulo="CPF/CNPJ" valor={c.documento} />}
              {c.endereco && <Info rotulo="Endereço" valor={c.endereco} />}
              <Info rotulo="Total já pago em encomendas" valor={moeda(d.totalPago)} />
              {c.observacoes && <div className="inteiro"><Info rotulo="Observações" valor={c.observacoes} /></div>}
            </div>
            <Historico titulo="Encomendas" itens={d.encomendas} render={(e) => (
              <><div className="info"><p>{e.descricao}</p><p>{moeda(e.valorTotal)} · entrega {data(e.prazoEntrega)}</p></div><Selo mapa={STATUS_ENCOMENDA} valor={e.status} /></>)} />
            <Historico titulo="Orçamentos" itens={d.orcamentos} render={(o) => (
              <><div className="info"><p>{o.tipoPeca}</p><p>{data(o.criadoEm)}</p></div><Selo mapa={STATUS_ORCAMENTO} valor={o.status} /></>)} />
          </>
        )}
      </Estado>
    </Modal>
  );
}

function Historico({ titulo, itens, render }) {
  if (!itens?.length) return null;
  return (
    <>
      <h3 style={{ fontSize: 18, margin: '24px 0 4px' }}>{titulo}</h3>
      <ul className="lista-linhas">{itens.map((x) => <li key={x.id}>{render(x)}</li>)}</ul>
    </>
  );
}
