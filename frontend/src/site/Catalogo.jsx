import { useState } from 'react';
import Icone from '../componentes/Icone.jsx';
import { SemImagem } from '../componentes/Marca.jsx';
import { Modal } from '../componentes/ui.jsx';
import { FRENTES, moeda, whatsAtelie } from '../util.js';
import { sacola } from './sacola.js';

/**
 * Categorias da loja (os círculos do topo e o menu). Como as peças não têm categoria própria,
 * cada uma é reconhecida pelo nome (e pela frente, quando faz sentido).
 */
export const CATEGORIAS = [
  { chave: 'cadernetas', rotulo: 'Cadernetas', icone: 'caderno', fundo: '#FDE2D3', cor: '#C9440F', palavras: ['caderneta', 'caderno', 'agenda', 'planner', 'bloco'] },
  { chave: 'canecas', rotulo: 'Canecas', icone: 'caneca', fundo: '#EEE7F8', cor: '#6A47A8', palavras: ['caneca', 'xícara', 'xicara', 'porcelana'], frente: 'PORCELANA' },
  { chave: 'garrafas', rotulo: 'Garrafas', icone: 'garrafa', fundo: '#DDEFF5', cor: '#1F6E8C', palavras: ['garrafa', 'squeeze', 'copo', 'tumbler'] },
  { chave: 'biblias', rotulo: 'Bíblias', icone: 'livro', fundo: '#FCEFC6', cor: '#8A5D00', palavras: ['bíblia', 'biblia', 'terço', 'terco'] },
  { chave: 'kits', rotulo: 'Kits e cestas', icone: 'presente', fundo: '#FBDDE6', cor: '#B03A62', palavras: ['kit', 'cesta', 'box', 'caixa'], frente: 'PRESENTE' },
  { chave: 'lembrancinhas', rotulo: 'Lembrancinhas', icone: 'coracao', fundo: '#E2F2E6', cor: '#2F7A4A', palavras: ['lembranc', 'chaveiro', 'tag', 'topo', 'marcador'] },
  { chave: 'pronta', rotulo: 'Pronta-entrega', icone: 'sacola', fundo: '#FFE9C7', cor: '#A65A00', pronta: true },
];

export function daCategoria(cat, i) {
  if (!cat) return true;
  if (cat.pronta) return i.prontaEntrega && i.disponiveis > 0;
  const nome = `${i.nome} ${i.texto || ''}`.toLowerCase();
  return cat.palavras.some((p) => nome.includes(p)) || (cat.frente && i.frente === cat.frente);
}

export const precoTexto = (i) => (i.preco == null ? 'Sob consulta' : moeda(i.preco));

export function mensagemPeca(i) {
  return i.prontaEntrega && i.disponiveis > 0
    ? `Olá, Jô! Vi no site a peça "${i.nome}" e quero comprar. Ainda tem disponível?`
    : `Olá, Jô! Vi no site a peça "${i.nome}" e quero personalizar. Pode me passar os detalhes?`;
}

/** Cartão da peça nas vitrines: foto, nome, preço e o botão da sacola. */
export function CartaoPeca({ i, aoAbrir, naSacola, selo, aoAdicionar }) {
  const pronta = i.prontaEntrega && i.disponiveis > 0;
  return (
    <article className="produto">
      <button type="button" className="produto-foto" onClick={aoAbrir} aria-label={`Ver detalhes: ${i.nome}`}>
        {i.fotos[0] ? <img src={i.fotos[0]} alt="" loading="lazy" /> : <SemImagem />}
        {selo ? <span className="produto-selo">{selo}</span>
          : pronta ? <span className="produto-selo verde">{i.disponiveis} {i.disponiveis === 1 ? 'pronta' : 'prontas'}</span> : null}
      </button>
      <button type="button" className="produto-nome" onClick={aoAbrir}>{i.nome}</button>
      <div className="produto-rodape">
        <span className="produto-preco">
          {i.preco != null && !pronta && <small>a partir de</small>}
          {precoTexto(i)}
        </span>
        <button type="button" className={`produto-add ${naSacola ? 'na-sacola' : ''}`}
          aria-label={naSacola ? `${i.nome} já está na sacola. Colocar mais uma` : `Colocar ${i.nome} na sacola`}
          onClick={() => { sacola.adicionar(i.id); aoAdicionar?.(i); }}>
          <Icone nome={naSacola ? 'check' : 'sacola'} tamanho={18} largura={naSacola ? 3 : 2} />
        </button>
      </div>
    </article>
  );
}

/** Detalhe da peça com galeria: colocar na sacola, pedir orçamento só dela ou falar no WhatsApp. */
export function DetalhePeca({ i, whatsapp, aoFechar, aoOrcar, aoAdicionar }) {
  const [foto, setFoto] = useState(0);
  const disponivel = i.prontaEntrega && i.disponiveis > 0;
  return (
    <Modal titulo={i.nome} aoFechar={aoFechar} largo>
      <div className="peca-detalhe">
        <div style={{ minWidth: 0 }}>
          <div className="peca-foto grande">
            {i.fotos[foto] ? <img src={i.fotos[foto]} alt={`${i.nome}, foto ${foto + 1}`} /> : <SemImagem />}
          </div>
          {i.fotos.length > 1 && (
            <div className="miniaturas" role="group" aria-label="Fotos da peça">
              {i.fotos.map((f, n) => (
                <button key={f} type="button" aria-pressed={n === foto} aria-label={`Foto ${n + 1}`} onClick={() => setFoto(n)}><img src={f} alt="" /></button>
              ))}
            </div>
          )}
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, minWidth: 0 }}>
          <span className="selo selo-terra" style={{ alignSelf: 'flex-start' }}>{FRENTES[i.frente]}</span>
          <p style={{ fontSize: 28, fontWeight: 900 }}>
            {i.preco != null && !disponivel && <small style={{ fontSize: 15, fontWeight: 700, color: 'var(--marrom-3)', marginRight: 6 }}>a partir de</small>}
            {precoTexto(i)}
          </p>
          {i.texto && <p style={{ whiteSpace: 'pre-line', fontWeight: 600, color: 'var(--marrom-2)' }}>{i.texto}</p>}
          <ul className="lista-check">
            {disponivel
              ? <li><Icone nome="check" tamanho={16} largura={3} />Pronta-entrega: {i.disponiveis} {i.disponiveis === 1 ? 'disponível' : 'disponíveis'}</li>
              : <li><Icone nome="check" tamanho={16} largura={3} />Personalizada com o nome, a frase e o tema que você escolher</li>}
            {!disponivel && <li><Icone nome="check" tamanho={16} largura={3} />Você aprova a arte antes da produção</li>}
            {i.prazoProducao && !disponivel && <li><Icone nome="check" tamanho={16} largura={3} />Prazo de produção: {i.prazoProducao.charAt(0).toLowerCase() + i.prazoProducao.slice(1)}</li>}
            <li><Icone nome="check" tamanho={16} largura={3} />Feita à mão, com carinho</li>
          </ul>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
            <button type="button" className="btn btn-terra" style={{ minHeight: 52 }} onClick={() => { sacola.adicionar(i.id); aoAdicionar?.(i); aoFechar(); }}>
              <Icone nome="sacola" tamanho={18} />Colocar na sacola
            </button>
            <button type="button" className="btn btn-claro" style={{ minHeight: 52 }} onClick={aoOrcar}>
              <Icone nome="orcamento" tamanho={18} />Pedir orçamento desta peça
            </button>
            <a className="btn btn-fantasma" style={{ minHeight: 52 }} target="_blank" rel="noopener noreferrer" href={whatsAtelie(whatsapp, mensagemPeca(i))}>
              <Icone nome="whats" tamanho={18} />{disponivel ? 'Quero comprar' : 'Tirar dúvida'}
            </a>
          </div>
        </div>
      </div>
    </Modal>
  );
}
