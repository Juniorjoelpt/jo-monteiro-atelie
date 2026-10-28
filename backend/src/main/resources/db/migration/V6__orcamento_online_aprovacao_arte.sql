-- Etapa 6: orçamento online com produto, personalização e fotos de referência; aprovação de arte pelo link

-- Orçamento pelo site: peça do catálogo (opcional), quantidade e o que vai personalizado
ALTER TABLE orcamento ADD COLUMN produto_id BIGINT;
ALTER TABLE orcamento ADD COLUMN quantidade INT NOT NULL DEFAULT 1;
ALTER TABLE orcamento ADD COLUMN personalizacao TEXT;
ALTER TABLE orcamento ADD CONSTRAINT fk_orcamento_produto FOREIGN KEY (produto_id) REFERENCES produto (id);

-- Fotos de referência mandadas pela cliente no orçamento (pasta privada)
CREATE TABLE orcamento_foto (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    orcamento_id  BIGINT NOT NULL,
    arquivo       VARCHAR(200) NOT NULL,
    criado_em     DATETIME(6) NOT NULL,
    CONSTRAINT fk_ofoto_orcamento FOREIGN KEY (orcamento_id) REFERENCES orcamento (id)
);
CREATE INDEX idx_ofoto_orcamento ON orcamento_foto (orcamento_id);

-- Encomenda: personalização e andamento da arte
ALTER TABLE encomenda ADD COLUMN personalizacao TEXT;
ALTER TABLE encomenda ADD COLUMN status_arte VARCHAR(20) NOT NULL DEFAULT 'SEM_ARTE';
ALTER TABLE encomenda ADD COLUMN codigo_arte VARCHAR(36);
CREATE UNIQUE INDEX uk_encomenda_codigo_arte ON encomenda (codigo_arte);
CREATE INDEX idx_encomenda_status_arte ON encomenda (status_arte);

-- Versões da arte enviadas para a cliente aprovar
CREATE TABLE arte_versao (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    encomenda_id           BIGINT NOT NULL,
    numero                 INT NOT NULL,
    arquivo                VARCHAR(200) NOT NULL,
    observacao             VARCHAR(500),
    resposta               VARCHAR(20),
    comentario_cliente     TEXT,
    respondida_no_sistema  BOOLEAN NOT NULL DEFAULT FALSE,
    respondido_em          DATETIME(6),
    criado_em              DATETIME(6) NOT NULL,
    CONSTRAINT fk_arte_encomenda FOREIGN KEY (encomenda_id) REFERENCES encomenda (id),
    CONSTRAINT uk_arte_numero UNIQUE (encomenda_id, numero)
);
