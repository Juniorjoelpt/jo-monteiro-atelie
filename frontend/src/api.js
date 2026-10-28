// Acesso à API do Spring Boot. O token de login fica no navegador de quem entrou.
const CHAVE_TOKEN = 'atelie.token';
const CHAVE_NOME = 'atelie.nome';
const CHAVE_PERFIL = 'atelie.perfil';

export const sessao = {
  token: () => localStorage.getItem(CHAVE_TOKEN),
  nome: () => localStorage.getItem(CHAVE_NOME) || '',
  perfil: () => localStorage.getItem(CHAVE_PERFIL) || 'ADMIN',
  /** Ajudante: vê a produção, sem valores, financeiro nem configurações. */
  ajudante: () => localStorage.getItem(CHAVE_PERFIL) === 'AJUDANTE',
  entrar(token, nome, perfil) {
    localStorage.setItem(CHAVE_TOKEN, token);
    localStorage.setItem(CHAVE_NOME, nome);
    localStorage.setItem(CHAVE_PERFIL, perfil || 'ADMIN');
  },
  atualizar(nome, perfil) {
    localStorage.setItem(CHAVE_NOME, nome);
    localStorage.setItem(CHAVE_PERFIL, perfil || 'ADMIN');
  },
  sair() {
    localStorage.removeItem(CHAVE_TOKEN);
    localStorage.removeItem(CHAVE_NOME);
    localStorage.removeItem(CHAVE_PERFIL);
  },
};

export class ErroApi extends Error {
  constructor(mensagem, status) {
    super(mensagem);
    this.status = status;
  }
}

async function chamar(metodo, url, corpo, { formData = false } = {}) {
  const headers = {};
  const token = sessao.token();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (corpo !== undefined && !formData) headers['Content-Type'] = 'application/json';

  let resp;
  try {
    resp = await fetch(url, {
      method: metodo,
      headers,
      body: corpo === undefined ? undefined : formData ? corpo : JSON.stringify(corpo),
    });
  } catch {
    throw new ErroApi('Sem conexão com o servidor. Verifique a internet e tente de novo.', 0);
  }

  if (resp.status === 401 && url.startsWith('/api/') && !url.startsWith('/api/auth/login')) {
    sessao.sair();
    if (window.location.pathname.startsWith('/admin')) {
      window.location.href = '/admin/entrar?expirou=1';
    }
    throw new ErroApi('Sua sessão expirou. Entre novamente.', 401);
  }
  if (resp.status === 204) return null;

  const texto = await resp.text();
  const dados = texto ? JSON.parse(texto) : null;
  if (!resp.ok) {
    throw new ErroApi(dados?.erro || 'Não foi possível concluir. Tente novamente.', resp.status);
  }
  return dados;
}

/** Baixa um arquivo protegido (PDF, planilha) e salva com o nome que o servidor mandou. */
async function baixar(url, nomePadrao = 'arquivo') {
  const token = sessao.token();
  let resp;
  try {
    resp = await fetch(url, { headers: token ? { Authorization: `Bearer ${token}` } : {} });
  } catch {
    throw new ErroApi('Sem conexão com o servidor. Verifique a internet e tente de novo.', 0);
  }
  if (!resp.ok) {
    let msg = 'Não foi possível gerar o arquivo.';
    try { msg = (await resp.json()).erro || msg; } catch { /* resposta sem JSON */ }
    if (resp.status === 401) { sessao.sair(); window.location.href = '/admin/entrar?expirou=1'; }
    throw new ErroApi(msg, resp.status);
  }
  const cd = resp.headers.get('Content-Disposition') || '';
  const m = /filename\*=UTF-8''([^;]+)/i.exec(cd) || /filename="?([^";]+)"?/i.exec(cd);
  const nome = m ? decodeURIComponent(m[1]) : nomePadrao;
  const blob = await resp.blob();
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = nome;
  document.body.appendChild(link);
  link.click();
  link.remove();
  setTimeout(() => URL.revokeObjectURL(link.href), 4000);
  return nome;
}

/** Busca uma imagem que exige login e devolve um endereço local (blob:) para o <img>. */
async function imagem(url) {
  const resp = await fetch(url, { headers: { Authorization: `Bearer ${sessao.token()}` } });
  if (!resp.ok) throw new ErroApi('Não foi possível abrir a foto.', resp.status);
  return URL.createObjectURL(await resp.blob());
}

export const api = {
  baixar,
  imagem,
  get: (url) => chamar('GET', url),
  post: (url, corpo) => chamar('POST', url, corpo ?? {}),
  put: (url, corpo) => chamar('PUT', url, corpo),
  patch: (url, corpo) => chamar('PATCH', url, corpo),
  del: (url) => chamar('DELETE', url),
  enviar: (url, formData) => chamar('POST', url, formData, { formData: true }),
};
