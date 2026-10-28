-- Ateliê Jô Monteiro — estrutura inicial

CREATE TABLE usuario (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome        VARCHAR(120) NOT NULL,
    email       VARCHAR(160) NOT NULL,
    senha_hash  VARCHAR(100) NOT NULL,
    ativo       BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em   DATETIME(6) NOT NULL,
    CONSTRAINT uk_usuario_email UNIQUE (email)
);

CREATE TABLE configuracao (
    id            BIGINT PRIMARY KEY,
    nome_atelie   VARCHAR(120) NOT NULL,
    whatsapp      VARCHAR(20)  NOT NULL,
    instagram     VARCHAR(80),
    chave_pix     VARCHAR(140),
    titular_pix   VARCHAR(120),
    endereco      VARCHAR(255),
    horario       VARCHAR(255),
    cidade        VARCHAR(120),
    sobre_texto   TEXT,
    atualizado_em DATETIME(6) NOT NULL
);

CREATE TABLE cliente (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome         VARCHAR(140) NOT NULL,
    whatsapp     VARCHAR(20),
    email        VARCHAR(160),
    cidade       VARCHAR(120),
    aniversario  DATE,
    tipo         VARCHAR(20) NOT NULL,
    observacoes  TEXT,
    criado_em    DATETIME(6) NOT NULL
);
CREATE INDEX idx_cliente_nome ON cliente (nome);
CREATE INDEX idx_cliente_whatsapp ON cliente (whatsapp);

CREATE TABLE orcamento (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id      BIGINT,
    nome            VARCHAR(140) NOT NULL,
    whatsapp        VARCHAR(20),
    tipo_peca       VARCHAR(80) NOT NULL,
    medidas         VARCHAR(160),
    cores           VARCHAR(160),
    data_desejada   DATE,
    detalhes        TEXT,
    valor_proposto  DECIMAL(12,2),
    status          VARCHAR(20) NOT NULL,
    origem          VARCHAR(10) NOT NULL,
    criado_em       DATETIME(6) NOT NULL,
    CONSTRAINT fk_orcamento_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id)
);
CREATE INDEX idx_orcamento_status ON orcamento (status);

CREATE TABLE encomenda (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id     BIGINT NOT NULL,
    orcamento_id   BIGINT,
    descricao      VARCHAR(200) NOT NULL,
    frente         VARCHAR(20) NOT NULL,
    valor_total    DECIMAL(12,2) NOT NULL,
    valor_sinal    DECIMAL(12,2) NOT NULL DEFAULT 0,
    prazo_entrega  DATE,
    status         VARCHAR(20) NOT NULL,
    observacoes    TEXT,
    criado_em      DATETIME(6) NOT NULL,
    CONSTRAINT fk_encomenda_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
    CONSTRAINT fk_encomenda_orcamento FOREIGN KEY (orcamento_id) REFERENCES orcamento (id)
);
CREATE INDEX idx_encomenda_status ON encomenda (status);
CREATE INDEX idx_encomenda_prazo ON encomenda (prazo_entrega);

CREATE TABLE lancamento (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    tipo             VARCHAR(10) NOT NULL,
    categoria        VARCHAR(20) NOT NULL,
    descricao        VARCHAR(200) NOT NULL,
    valor            DECIMAL(12,2) NOT NULL,
    data_lancamento  DATE NOT NULL,
    forma_pagamento  VARCHAR(20) NOT NULL,
    encomenda_id     BIGINT,
    criado_em        DATETIME(6) NOT NULL,
    CONSTRAINT fk_lancamento_encomenda FOREIGN KEY (encomenda_id) REFERENCES encomenda (id)
);
CREATE INDEX idx_lancamento_data ON lancamento (data_lancamento);

CREATE TABLE material (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome             VARCHAR(140) NOT NULL,
    unidade          VARCHAR(20) NOT NULL,
    quantidade       DECIMAL(12,2) NOT NULL DEFAULT 0,
    estoque_minimo   DECIMAL(12,2) NOT NULL DEFAULT 0,
    custo_unitario   DECIMAL(12,2) NOT NULL DEFAULT 0,
    criado_em        DATETIME(6) NOT NULL
);

CREATE TABLE portfolio_item (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo       VARCHAR(140) NOT NULL,
    categoria    VARCHAR(20) NOT NULL,
    arquivo      VARCHAR(200) NOT NULL,
    ordem        INT NOT NULL DEFAULT 0,
    publicado    BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em    DATETIME(6) NOT NULL
);
