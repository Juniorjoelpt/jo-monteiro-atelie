import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api.js';
import Icone from '../componentes/Icone.jsx';
import { Campo, Confirmar, Estado, ModalForm, lerForm, useCarregar, useToast, Vazio } from '../componentes/ui.jsx';
import { data, moeda, telefone } from '../util.js';
import { Topo } from './Layout.jsx';

export default function Configuracoes() {
  const { dados: c, erro, carregando } = useCarregar(() => api.get('/api/configuracoes'));
  return (
    <>
      <Topo titulo="Configurações" />
      <Precificacao />
      <Estado carregando={carregando} erro={erro}>
        {c && <DadosAtelie c={c} />}
      </Estado>
      <Documentos />
      <Equipe />
      <TrocarSenha />
    </>
  );
}

function DadosAtelie({ c }) {
  const [erro, setErro] = useState(null);
  const [salvando, setSalvando] = useState(false);
  const toast = useToast();

  async function salvar(e) {
    e.preventDefault();
    setErro(null);
    setSalvando(true);
    try {
      await api.put('/api/configuracoes', lerForm(new FormData(e.currentTarget)));
      toast('Dados do ateliê salvos. O site já foi atualizado.');
    } catch (err) {
      setErro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <form className="cartao" onSubmit={salvar} noValidate>
      <div className="cartao-titulo"><h2>Dados do ateliê no site</h2></div>
      {erro && <div className="aviso aviso-erro" role="alert" style={{ marginBottom: 16 }}>{erro}</div>}
      <div className="form-grade">
        <Campo rotulo="Nome do ateliê" nome="nomeAtelie" defaultValue={c.nomeAtelie} required />
        <Campo rotulo="WhatsApp do ateliê" nome="whatsapp" defaultValue={c.whatsapp ? telefone(c.whatsapp) : ''} required inputMode="tel" dica="Recebe os orçamentos do site e as dúvidas sobre a arte." />
        <Campo rotulo="Chave Pix" nome="chavePix" defaultValue={c.chavePix} dica="Aparece na proposta e nas mensagens de cobrança." />
        <Campo rotulo="Nome do titular do Pix" nome="titularPix" defaultValue={c.titularPix} />
        <Campo rotulo="Instagram" nome="instagram" defaultValue={c.instagram} placeholder="jomonteiroatelie" />
        <Campo rotulo="Cidade" nome="cidade" defaultValue={c.cidade} placeholder="Governador Nunes Freire — MA" />
        <Campo rotulo="Endereço do ateliê" nome="endereco" defaultValue={c.endereco} inteiro />
        <Campo rotulo="Horário de funcionamento" nome="horario" defaultValue={c.horario} inteiro placeholder="Seg. a sex., 9h às 18h" />
        <Campo rotulo='Texto do "Quem sou"' nome="sobreTexto" linhas={5} defaultValue={c.sobreTexto} inteiro />
      </div>
      <div className="modal-rodape"><button type="submit" className="btn btn-terra" disabled={salvando}>{salvando ? 'Salvando…' : 'Salvar'}</button></div>
    </form>
  );
}

export function TrocarSenha() {
  const [erro, setErro] = useState(null);
  const toast = useToast();

  async function salvar(e) {
    e.preventDefault();
    const form = e.currentTarget;
    const d = lerForm(new FormData(form));
    setErro(null);
    if (d.novaSenha !== d.confirmar) { setErro('A confirmação não confere com a nova senha.'); return; }
    try {
      await api.post('/api/auth/trocar-senha', { senhaAtual: d.senhaAtual, novaSenha: d.novaSenha });
      toast('Senha alterada.');
      form.reset();
    } catch (err) {
      setErro(err.message);
    }
  }

  return (
    <form className="cartao" onSubmit={salvar} noValidate>
      <div className="cartao-titulo"><h2>Trocar senha</h2></div>
      {erro && <div className="aviso aviso-erro" role="alert" style={{ marginBottom: 16 }}>{erro}</div>}
      <div className="form-grade">
        <Campo rotulo="Senha atual" nome="senhaAtual" tipo="password" autoComplete="current-password" required inteiro />
        <Campo rotulo="Nova senha" nome="novaSenha" tipo="password" autoComplete="new-password" minLength={8} required dica="Mínimo de 8 caracteres." />
        <Campo rotulo="Confirmar nova senha" nome="confirmar" tipo="password" autoComplete="new-password" required />
      </div>
      <div className="modal-rodape"><button type="submit" className="btn btn-terra">Trocar senha</button></div>
    </form>
  );
}

const PERFIS = [['AJUDANTE', 'Ajudante (produção, sem valores)'], ['ADMIN', 'Administradora (acesso completo)']];

/** Acessos ao sistema: a ajudante vê encomendas, artes, materiais, agenda e o caixa, sem valores nem financeiro. */
function Equipe() {
  const lista = useCarregar(() => api.get('/api/equipe'));
  const [form, setForm] = useState(null);
  const [excluir, setExcluir] = useState(null);
  const toast = useToast();
  return (
    <section className="cartao" aria-labelledby="t-equipe">
      <div className="cartao-titulo">
        <h2 id="t-equipe">Equipe</h2>
        <button type="button" className="btn btn-claro btn-p" onClick={() => setForm({ perfil: 'AJUDANTE' })}><Icone nome="mais" tamanho={16} largura={3} />Novo acesso</button>
      </div>
      <p className="mudo forte" style={{ marginBottom: 12, fontSize: 15 }}>
        A ajudante acompanha e produz as encomendas, envia as artes para aprovação, registra o material usado, vê a agenda e vende no caixa.
        Ela não vê valores cobrados, custos, financeiro, relatórios nem configurações.
      </p>
      <Estado carregando={lista.carregando} erro={lista.erro}>
        {lista.dados?.length === 0 ? <Vazio>Nenhum acesso.</Vazio> : (
          <ul className="lista-linhas">
            {lista.dados?.map((u) => (
              <li key={u.id} style={{ opacity: u.ativo ? 1 : 0.55 }}>
                <div className="avatar" aria-hidden="true" style={{ width: 44, height: 44, fontSize: 18 }}>{u.nome.charAt(0)}</div>
                <div className="info"><p>{u.nome}{u.euMesma ? ' (você)' : ''}</p><p>{u.email} · desde {data(u.criadoEm)}</p></div>
                <span className={`selo ${u.perfil === 'ADMIN' ? 'selo-terra' : 'selo-oliva'}`}>{u.perfil === 'ADMIN' ? 'Administradora' : 'Ajudante'}</span>
                {!u.ativo && <span className="selo selo-areia">desativado</span>}
                <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Editar ${u.nome}`} onClick={() => setForm(u)}><Icone nome="editar" tamanho={18} /></button>
                {!u.euMesma && <button type="button" className="btn btn-fantasma btn-icone btn-p" aria-label={`Excluir ${u.nome}`} onClick={() => setExcluir(u)}><Icone nome="lixo" tamanho={18} /></button>}
              </li>
            ))}
          </ul>
        )}
      </Estado>

      {form && (
        <ModalForm titulo={form.id ? `Editar acesso de ${form.nome}` : 'Novo acesso'} aoFechar={() => setForm(null)} aoSalvar={async (fd) => {
          const d = lerForm(fd);
          d.ativo = form.euMesma ? true : fd.get('ativo') === 'on';
          if (form.euMesma) d.perfil = 'ADMIN';
          if (form.id) await api.put(`/api/equipe/${form.id}`, d);
          else await api.post('/api/equipe', d);
          toast(form.id ? 'Acesso atualizado.' : 'Acesso criado. Passe o e-mail e a senha para ela entrar em /admin.');
          setForm(null);
          lista.recarregar();
        }}>
          <Campo rotulo="Nome" nome="nome" defaultValue={form.nome} required maxLength={120} />
          <Campo rotulo="E-mail (para entrar)" nome="email" tipo="email" defaultValue={form.email} required maxLength={160} autoComplete="off" />
          {!form.euMesma && <Campo rotulo="Perfil" nome="perfil" opcoes={PERFIS} defaultValue={form.perfil || 'AJUDANTE'} inteiro />}
          <Campo rotulo={form.id ? 'Nova senha (opcional)' : 'Senha provisória'} nome="senha" tipo="password" minLength={8} required={!form.id}
            autoComplete="new-password" dica={form.id ? 'Em branco, mantém a senha atual.' : 'Mínimo de 8 caracteres. Ela pode trocar depois.'} inteiro />
          {form.id && !form.euMesma && <label className="checkbox inteiro"><input type="checkbox" name="ativo" defaultChecked={form.ativo} /> Acesso ativo (desmarque para bloquear na hora)</label>}
        </ModalForm>
      )}
      {excluir && <Confirmar titulo="Excluir acesso" perigo textoBotao="Excluir" texto={`Excluir o acesso de ${excluir.nome}? Ela não consegue mais entrar.`}
        aoFechar={() => setExcluir(null)} aoConfirmar={async () => { await api.del(`/api/equipe/${excluir.id}`); toast('Acesso excluído.'); setExcluir(null); lista.recarregar(); }} />}
    </section>
  );
}

function Precificacao() {
  const { dados: p, erro: erroCarga, carregando } = useCarregar(() => api.get('/api/configuracoes/precificacao'));
  return (
    <Estado carregando={carregando} erro={erroCarga}>
      {p && <FormPrecificacao p={p} />}
    </Estado>
  );
}

function FormPrecificacao({ p }) {
  const [f, setF] = useState({
    salarioDesejado: String(p.salarioDesejado), horasMes: String(p.horasMes),
    custosFixosMensais: String(p.custosFixosMensais), margemPadrao: String(Number(p.margemPadrao)),
  });
  const [automatico, setAutomatico] = useState(p.custosFixosAutomatico);
  const [erro, setErro] = useState(null);
  const [salvando, setSalvando] = useState(false);
  const toast = useToast();
  const n = (v) => Number(String(v).replace(',', '.')) || 0;
  const horas = Math.max(1, n(f.horasMes));
  const valorHora = n(f.salarioDesejado) / horas;
  const fixos = automatico ? Number(p.totalContasFixas) : n(f.custosFixosMensais);
  const fixoHora = fixos / horas;
  const set = (k) => (e) => setF((x) => ({ ...x, [k]: e.target.value }));

  async function salvar(e) {
    e.preventDefault();
    setErro(null);
    setSalvando(true);
    try {
      await api.put('/api/configuracoes/precificacao', {
        salarioDesejado: n(f.salarioDesejado), horasMes: Math.round(n(f.horasMes)),
        custosFixosMensais: n(f.custosFixosMensais), margemPadrao: n(f.margemPadrao), custosFixosAutomatico: automatico,
      });
      toast('Precificação salva. As fichas já usam os novos valores.');
    } catch (err) {
      setErro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <form className="cartao" id="precificacao" onSubmit={salvar} noValidate>
      <div className="cartao-titulo"><h2>Precificação</h2><span className="selo selo-areia">só aparece aqui no sistema</span></div>
      <p className="mudo forte" style={{ marginBottom: 16, fontSize: 15 }}>
        Com esses valores o sistema calcula quanto vale sua hora e reparte os custos fixos em cada peça.
      </p>
      {erro && <div className="aviso aviso-erro" role="alert" style={{ marginBottom: 16 }}>{erro}</div>}
      <div className="form-grade">
        <div className="campo">
          <label htmlFor="pr-sal">Quanto quer ganhar por mês (R$)</label>
          <input id="pr-sal" className="entrada" type="number" min="0" step="50" value={f.salarioDesejado} onChange={set('salarioDesejado')} />
          <span className="dica">O seu "salário" com o ateliê.</span>
        </div>
        <div className="campo">
          <label htmlFor="pr-horas">Horas de produção por mês</label>
          <input id="pr-horas" className="entrada" type="number" min="1" max="400" step="1" value={f.horasMes} onChange={set('horasMes')} />
          <span className="dica">Ex.: 5 dias × 6 h × 4 semanas = 120 h.</span>
        </div>
        <div className="campo">
          <label htmlFor="pr-fixos">Custos fixos do mês (R$)</label>
          <input id="pr-fixos" className="entrada" type="number" min="0" step="10" value={automatico ? String(p.totalContasFixas) : f.custosFixosMensais}
            onChange={set('custosFixosMensais')} disabled={automatico} />
          <label className="checkbox" style={{ marginTop: 6 }}>
            <input type="checkbox" checked={automatico} onChange={(e) => setAutomatico(e.target.checked)} /> Usar as contas fixas ({moeda(p.totalContasFixas)})
          </label>
          <span className="dica">{automatico
            ? <>Acompanha sozinho as <Link to="/admin/financeiro/contas">contas fixas</Link> marcadas para a precificação.</>
            : 'Aluguel, energia, internet, MEI, aplicativos…'}</span>
        </div>
        <div className="campo">
          <label htmlFor="pr-margem">Margem de lucro padrão (%)</label>
          <input id="pr-margem" className="entrada" type="number" min="0" max="500" step="1" value={f.margemPadrao} onChange={set('margemPadrao')} />
          <span className="dica">Aplicada sobre o custo total. Cada ficha pode ter a sua.</span>
        </div>
      </div>
      <div className="custos-grade" style={{ marginTop: 18 }}>
        <div style={{ background: 'var(--oliva-claro)' }}><p>Sua hora vale</p><p>{moeda(valorHora)}</p></div>
        <div style={{ background: 'var(--ocre-claro)' }}><p>Custo fixo por hora</p><p>{moeda(fixoHora)}</p></div>
        <div style={{ background: 'var(--terra-claro)' }}><p>Uma peça de 3 h custa de trabalho</p><p>{moeda((valorHora + fixoHora) * 3)}</p></div>
      </div>
      <div className="modal-rodape"><button type="submit" className="btn btn-terra" disabled={salvando}>{salvando ? 'Salvando…' : 'Salvar precificação'}</button></div>
    </form>
  );
}

function Documentos() {
  const { dados: d, erro: erroCarga, carregando } = useCarregar(() => api.get('/api/configuracoes/documentos'));
  const [erro, setErro] = useState(null);
  const [salvando, setSalvando] = useState(false);
  const toast = useToast();

  async function salvar(e) {
    e.preventDefault();
    setErro(null);
    setSalvando(true);
    try {
      await api.put('/api/configuracoes/documentos', lerForm(new FormData(e.currentTarget), ['sinalPercentual', 'validadePropostaDias']));
      toast('Dados da proposta e do contrato salvos.');
    } catch (err) {
      setErro(err.message);
    } finally {
      setSalvando(false);
    }
  }

  return (
    <Estado carregando={carregando} erro={erroCarga}>
      {d && (
        <form className="cartao" onSubmit={salvar} noValidate>
          <div className="cartao-titulo"><h2>Proposta e contrato</h2><span className="selo selo-areia">só aparece nos PDFs</span></div>
          <p className="mudo forte" style={{ marginBottom: 16, fontSize: 15 }}>
            Esses dados entram na proposta (gerada no orçamento) e no contrato (gerado na encomenda).
            O contrato é um modelo: vale pedir para alguém de confiança revisar o texto.
          </p>
          {erro && <div className="aviso aviso-erro" role="alert" style={{ marginBottom: 16 }}>{erro}</div>}
          <div className="form-grade">
            <Campo rotulo="Seu nome completo" nome="responsavel" defaultValue={d.responsavel} maxLength={120} dica="Assina a proposta e o contrato." />
            <Campo rotulo="CPF ou CNPJ do ateliê" nome="documento" defaultValue={d.documento} maxLength={30} dica="Se ficar vazio, o contrato sai com espaço para preencher à mão." />
            <Campo rotulo="Sinal padrão (%)" nome="sinalPercentual" tipo="number" min="0" max="100" step="5" defaultValue={Number(d.sinalPercentual)} required />
            <Campo rotulo="Validade da proposta (dias)" nome="validadePropostaDias" tipo="number" min="1" max="365" step="1" defaultValue={d.validadePropostaDias} required />
            <Campo rotulo="Condições da proposta" nome="condicoesProposta" linhas={4} defaultValue={d.condicoesProposta} inteiro maxLength={4000} />
            <Campo rotulo="Regras de cancelamento (vão para o contrato)" nome="politicaCancelamento" linhas={5} defaultValue={d.politicaCancelamento} inteiro maxLength={4000} />
          </div>
          <div className="modal-rodape"><button type="submit" className="btn btn-terra" disabled={salvando}>{salvando ? 'Salvando…' : 'Salvar'}</button></div>
        </form>
      )}
    </Estado>
  );
}
