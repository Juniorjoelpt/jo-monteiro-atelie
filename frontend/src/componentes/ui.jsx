import { createContext, useCallback, useContext, useEffect, useId, useRef, useState } from 'react';
import Icone from './Icone.jsx';

// ---------- Avisos rápidos (toast) ----------

const ToastCtx = createContext(() => {});
export const useToast = () => useContext(ToastCtx);

export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null);
  const timer = useRef();
  const mostrar = useCallback((texto, tipo = 'ok') => {
    clearTimeout(timer.current);
    setToast({ texto, tipo });
    timer.current = setTimeout(() => setToast(null), 3500);
  }, []);
  return (
    <ToastCtx.Provider value={mostrar}>
      {children}
      {toast && (
        <div role="status" aria-live="polite" className={`toast ${toast.tipo === 'erro' ? 'toast-erro' : ''}`}>
          {toast.texto}
        </div>
      )}
    </ToastCtx.Provider>
  );
}

// ---------- Carregar dados da API ----------

export function useCarregar(fn, deps = []) {
  const [estado, setEstado] = useState({ dados: null, erro: null, carregando: true });
  const [versao, setVersao] = useState(0);
  useEffect(() => {
    let vivo = true;
    setEstado((e) => ({ ...e, carregando: true }));
    fn()
      .then((dados) => vivo && setEstado({ dados, erro: null, carregando: false }))
      .catch((erro) => vivo && setEstado({ dados: null, erro: erro.message, carregando: false }));
    return () => { vivo = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, versao]);
  return { ...estado, recarregar: () => setVersao((v) => v + 1) };
}

export function Estado({ carregando, erro, children }) {
  if (erro) return <div className="aviso aviso-erro" role="alert">{erro}</div>;
  if (carregando) return <div className="carregando">Carregando…</div>;
  return children;
}

// ---------- Modal ----------

export function Modal({ titulo, aoFechar, children, rodape, largo = false }) {
  const idTitulo = useId();
  const caixa = useRef();
  useEffect(() => {
    const anterior = document.activeElement;
    const primeiro = caixa.current?.querySelector('input, select, textarea, button:not(.fechar-modal)');
    primeiro?.focus();
    const tecla = (e) => e.key === 'Escape' && aoFechar();
    document.addEventListener('keydown', tecla);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', tecla);
      document.body.style.overflow = '';
      anterior?.focus?.();
    };
  }, [aoFechar]);
  return (
    <div className="modal-fundo" onMouseDown={(e) => e.target === e.currentTarget && aoFechar()}>
      <div ref={caixa} className={`modal ${largo ? 'modal-largo' : ''}`} role="dialog" aria-modal="true" aria-labelledby={idTitulo}>
        <div className="modal-topo">
          <h2 id={idTitulo}>{titulo}</h2>
          <button type="button" className="btn btn-claro btn-icone fechar-modal" onClick={aoFechar} aria-label="Fechar">
            <Icone nome="fechar" />
          </button>
        </div>
        {children}
        {rodape && <div className="modal-rodape">{rodape}</div>}
      </div>
    </div>
  );
}

/** Modal com formulário: cuida de enviar, mostrar erro e desabilitar o botão. */
export function ModalForm({ titulo, aoFechar, aoSalvar, textoSalvar = 'Salvar', children, largo }) {
  const [erro, setErro] = useState(null);
  const [enviando, setEnviando] = useState(false);
  async function enviar(e) {
    e.preventDefault();
    setErro(null);
    setEnviando(true);
    try {
      await aoSalvar(new FormData(e.currentTarget));
    } catch (err) {
      setErro(err.message);
      setEnviando(false);
    }
  }
  return (
    <Modal titulo={titulo} aoFechar={aoFechar} largo={largo}>
      <form onSubmit={enviar} noValidate>
        {erro && <div className="aviso aviso-erro" role="alert" style={{ marginBottom: 16 }}>{erro}</div>}
        <div className="form-grade">{children}</div>
        <div className="modal-rodape">
          <button type="button" className="btn btn-fantasma" onClick={aoFechar}>Cancelar</button>
          <button type="submit" className="btn btn-terra" disabled={enviando}>{enviando ? 'Salvando…' : textoSalvar}</button>
        </div>
      </form>
    </Modal>
  );
}

// ---------- Campos ----------

export function Campo({ rotulo, nome, tipo = 'text', inteiro, dica, opcoes, linhas, ...resto }) {
  const id = useId();
  let controle;
  if (opcoes) {
    controle = (
      <select id={id} name={nome} className="entrada" {...resto}>
        {(Array.isArray(opcoes) ? opcoes : Object.entries(opcoes)).map(([v, r]) => <option key={v} value={v}>{r}</option>)}
      </select>
    );
  } else if (linhas) {
    controle = <textarea id={id} name={nome} rows={linhas} className="entrada" {...resto} />;
  } else {
    controle = <input id={id} name={nome} type={tipo} className="entrada" {...resto} />;
  }
  return (
    <div className={`campo ${inteiro ? 'inteiro' : ''}`}>
      <label htmlFor={id}>{rotulo}</label>
      {controle}
      {dica && <span className="dica">{dica}</span>}
    </div>
  );
}

/** Converte o FormData em objeto, trocando "" por null e números quando pedido. */
export function lerForm(fd, numeros = []) {
  const o = {};
  for (const [k, v] of fd.entries()) {
    if (typeof v !== 'string') continue;
    const t = v.trim();
    if (numeros.includes(k)) o[k] = t === '' ? null : Number(t.replace(',', '.'));
    else o[k] = t === '' ? null : t;
  }
  return o;
}

export function Selo({ mapa, valor }) {
  const [rotulo, cor] = mapa[valor] || [valor, 'areia'];
  return <span className={`selo selo-${cor}`}>{rotulo}</span>;
}

export function Vazio({ children }) {
  return <div className="vazio">{children}</div>;
}

export function Confirmar({ titulo, texto, aoConfirmar, aoFechar, textoBotao = 'Confirmar', perigo }) {
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState(null);
  return (
    <Modal titulo={titulo} aoFechar={aoFechar} rodape={<>
      <button type="button" className="btn btn-fantasma" onClick={aoFechar}>Voltar</button>
      <button type="button" className={`btn ${perigo ? 'btn-perigo' : 'btn-terra'}`} disabled={enviando}
        onClick={async () => {
          setEnviando(true);
          try { await aoConfirmar(); } catch (e) { setErro(e.message); setEnviando(false); }
        }}>{textoBotao}</button>
    </>}>
      {erro && <div className="aviso aviso-erro" role="alert" style={{ marginBottom: 12 }}>{erro}</div>}
      <p>{texto}</p>
    </Modal>
  );
}
