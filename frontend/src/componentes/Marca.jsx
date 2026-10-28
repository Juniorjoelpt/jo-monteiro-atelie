// Logos do Ateliê Jô Monteiro, a partir do manual de identidade visual (fundo transparente),
// já com a mascote nova em 3D.
const ARQUIVOS = {
  horizontal: [1000, 317],
  empilhada: [1000, 367],
  circular: [560, 561],
  simbolo: [360, 360],
};

/**
 * variante: horizontal (mascote de busto) | empilhada (mascote de corpo inteiro) | circular (selo)
 *           | simbolo (a Jô no círculo laranja)
 * clara: versão com texto branco, para fundo laranja ou preto
 */
export default function Marca({ variante = 'horizontal', clara = false, decorativa = false, className = '', ...resto }) {
  const [w, h] = ARQUIVOS[variante] || ARQUIVOS.horizontal;
  const temClara = variante !== 'simbolo';
  return (
    <img src={`/marca/${variante}${clara && temClara ? '-clara' : ''}.webp`} width={w} height={h}
      alt={decorativa ? '' : 'Ateliê Jô Monteiro'} aria-hidden={decorativa || undefined}
      className={`logo ${className}`} {...resto} />
  );
}

// Mascote da Jô em 3D, corpo inteiro e fundo transparente, em cada pose.
const POSES = {
  acenando: [508, 900], // dando tchau com a caderneta amarela
  apontando: [560, 858], // apontando para o lado, mão na cintura
  pensando: [401, 900], // mão no queixo
  raposa: [514, 900], // caderneta e a raposinha na mão
  notebook: [560, 863], // camiseta da raposa, acenando com o notebook
  bebe: [534, 900], // com um bebê no colo
  garrafa: [320, 570], // bebendo água com a caderneta
  'garrafa-raposa': [432, 900], // camiseta da raposa, garrafa preta
};

export function Mascote({ pose = 'acenando', className = '', ...resto }) {
  const [w, h] = POSES[pose] || POSES.acenando;
  return (
    <img src={`/marca/mascote-${pose}.webp`} width={w} height={h} alt="" aria-hidden="true"
      className={`logo mascote ${className}`} {...resto} />
  );
}

/** Lugar de foto que ainda não existe: a Jô no círculo, suave. */
export function SemImagem() {
  return <img src="/marca/simbolo.webp" alt="" aria-hidden="true" className="sem-imagem" />;
}
