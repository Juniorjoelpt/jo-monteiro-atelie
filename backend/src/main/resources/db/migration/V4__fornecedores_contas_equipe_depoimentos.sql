-- Etapa 4: fornecedores e compras, contas fixas, equipe e depoimentos

-- Fornecedores de material
CREATE TABLE fornecedor (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome         VARCHAR(140) NOT NULL,
    whatsapp     VARCHAR(20),
    contato      VARCHAR(160),
    cidade       VARCHAR(120),
    observacoes  TEXT,
    ativo        BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em    DATETIME(6) NOT NULL
);

-- Compra com vários materiais: soma no estoque, atualiza o custo e vira despesa
CREATE TABLE compra (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    fornecedor_id    BIGINT,
    data_compra      DATE NOT NULL,
    frete            DECIMAL(12,2) NOT NULL DEFAULT 0,
    total            DECIMAL(12,2) NOT NULL,
    forma_pagamento  VARCHAR(20) NOT NULL,
    observacoes      VARCHAR(255),
    status           VARCHAR(20) NOT NULL,
    criado_em        DATETIME(6) NOT NULL,
    CONSTRAINT fk_compra_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES fornecedor (id)
);
CREATE INDEX idx_compra_data ON compra (data_compra);

CREATE TABLE compra_item (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    compra_id       BIGINT NOT NULL,
    material_id     BIGINT NOT NULL,
    quantidade      DECIMAL(12,3) NOT NULL,
    valor           DECIMAL(12,2) NOT NULL,
    -- (valor + parte do frete) ÷ quantidade
    custo_unitario  DECIMAL(12,4) NOT NULL,
    CONSTRAINT fk_citem_compra FOREIGN KEY (compra_id) REFERENCES compra (id),
    CONSTRAINT fk_citem_material FOREIGN KEY (material_id) REFERENCES material (id)
);
CREATE INDEX idx_citem_material ON compra_item (material_id);

ALTER TABLE lancamento ADD COLUMN compra_id BIGINT;
ALTER TABLE lancamento ADD CONSTRAINT fk_lanc_compra FOREIGN KEY (compra_id) REFERENCES compra (id);

-- Contas fixas do mês: aluguel, internet, MEI…
CREATE TABLE conta_fixa (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome                   VARCHAR(120) NOT NULL,
    valor                  DECIMAL(12,2) NOT NULL,
    dia_vencimento         INT NOT NULL,
    entra_na_precificacao  BOOLEAN NOT NULL DEFAULT TRUE,
    ativa                  BOOLEAN NOT NULL DEFAULT TRUE,
    observacoes            VARCHAR(255),
    criado_em              DATETIME(6) NOT NULL
);

-- O pagamento de cada mês é um lançamento ligado à conta e ao mês (competência = dia 1 do mês)
ALTER TABLE lancamento ADD COLUMN conta_fixa_id BIGINT;
ALTER TABLE lancamento ADD COLUMN competencia DATE;
ALTER TABLE lancamento ADD CONSTRAINT fk_lanc_conta FOREIGN KEY (conta_fixa_id) REFERENCES conta_fixa (id);
CREATE UNIQUE INDEX uk_lanc_conta_mes ON lancamento (conta_fixa_id, competencia);

-- Custos fixos da precificação podem vir sozinhos da soma das contas fixas
ALTER TABLE configuracao ADD COLUMN custos_fixos_automatico BOOLEAN NOT NULL DEFAULT FALSE;

-- Equipe: a dona do ateliê (ADMIN) e ajudantes com acesso limitado
ALTER TABLE usuario ADD COLUMN perfil VARCHAR(20) NOT NULL DEFAULT 'ADMIN';

-- Depoimentos exibidos no site
CREATE TABLE depoimento (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome       VARCHAR(120) NOT NULL,
    contexto   VARCHAR(120),
    texto      TEXT NOT NULL,
    estrelas   INT NOT NULL DEFAULT 5,
    foto       VARCHAR(200),
    publicado  BOOLEAN NOT NULL DEFAULT TRUE,
    ordem      INT NOT NULL DEFAULT 0,
    criado_em  DATETIME(6) NOT NULL
);
