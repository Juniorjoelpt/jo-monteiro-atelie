// Reduz fotos do celular antes de enviar: o servidor aceita até 8 MB por foto, e uma foto de 4 MB
// vira ~300 KB, o que deixa o envio rápido mesmo no 4G.

const TIPOS_ACEITOS = ['image/jpeg', 'image/png', 'image/webp'];

/** Devolve um File em JPEG com o lado maior de até `max` px (ou o original, se já for pequeno). */
export async function reduzirImagem(arquivo, max = 1600, qualidade = 0.85) {
  let bitmap;
  try {
    bitmap = await createImageBitmap(arquivo);
  } catch {
    // O navegador não conseguiu abrir (ex.: HEIC no computador): manda o original se for de um tipo aceito
    if (TIPOS_ACEITOS.includes(arquivo.type)) return arquivo;
    throw new Error('Não consegui abrir esta foto. Use uma imagem JPG ou PNG.');
  }
  const escala = Math.min(1, max / Math.max(bitmap.width, bitmap.height));
  if (escala === 1 && arquivo.size < 900 * 1024 && TIPOS_ACEITOS.includes(arquivo.type)) {
    bitmap.close?.();
    return arquivo;
  }
  const canvas = document.createElement('canvas');
  canvas.width = Math.round(bitmap.width * escala);
  canvas.height = Math.round(bitmap.height * escala);
  const ctx = canvas.getContext('2d');
  ctx.fillStyle = '#fff'; // PNG transparente vira fundo branco no JPEG
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  ctx.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  bitmap.close?.();
  const blob = await new Promise((ok) => canvas.toBlob(ok, 'image/jpeg', qualidade));
  if (!blob) throw new Error('Não consegui preparar esta foto. Tente outra.');
  const nome = (arquivo.name || 'foto').replace(/\.[^.]+$/, '') + '.jpg';
  return new File([blob], nome, { type: 'image/jpeg' });
}
