-- Etapa 5: catálogo de peças no site

-- Catálogo: a peça da ficha técnica aparece no site com texto e fotos próprios
ALTER TABLE produto ADD COLUMN no_catalogo BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE produto ADD COLUMN mostrar_preco BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE produto ADD COLUMN texto_catalogo TEXT;
ALTER TABLE produto ADD COLUMN prazo_producao VARCHAR(80);
ALTER TABLE produto ADD COLUMN ordem_catalogo INT NOT NULL DEFAULT 0;

CREATE TABLE produto_foto (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    produto_id  BIGINT NOT NULL,
    arquivo     VARCHAR(200) NOT NULL,
    ordem       INT NOT NULL DEFAULT 0,
    criado_em   DATETIME(6) NOT NULL,
    CONSTRAINT fk_pfoto_produto FOREIGN KEY (produto_id) REFERENCES produto (id)
);
CREATE INDEX idx_pfoto_produto ON produto_foto (produto_id, ordem);
