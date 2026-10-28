// Logos do Ateliê Jô Monteiro, extraídas do manual de identidade visual (fundo transparente).
const ARQUIVOS = {
  horizontal: [1000, 317],
  empilhada: [1000, 385],
  circular: [560, 561],
  simbolo: [360, 346],
  mascote: [360, 693],
};

/**
 * variante: horizontal (mascote de busto) | empilhada (mascote de corpo inteiro) | circular (selo)
 *           | simbolo (a Jô no círculo laranja) | mascote (corpo inteiro, sem texto)
 * clara: versão com texto branco, para fundo laranja ou preto
 */
export default function Marca({ variante = 'horizontal', clara = false, decorativa = false, className = '', ...resto }) {
  const [w, h] = ARQUIVOS[variante] || ARQUIVOS.horizontal;
  const temClara = variante !== 'simbolo' && variante !== 'mascote';
  return (
    <img src={`/marca/${variante}${clara && temClara ? '-clara' : ''}.webp`} width={w} height={h}
      alt={decorativa ? '' : 'Ateliê Jô Monteiro'} aria-hidden={decorativa || undefined}
      className={`logo ${className}`} {...resto} />
  );
}

/** Lugar de foto que ainda não existe: a Jô no círculo, suave. */
export function SemImagem() {
  return <img src="/marca/simbolo.webp" alt="" aria-hidden="true" className="sem-imagem" />;
}
