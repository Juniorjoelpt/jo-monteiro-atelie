-- Etapa 2: fichas técnicas e precificação, baixa de material por encomenda, vendas de pronta-entrega

-- Parâmetros de precificação (valor da hora e rateio dos custos fixos)
ALTER TABLE configuracao ADD COLUMN salario_desejado DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE configuracao ADD COLUMN horas_mes INT NOT NULL DEFAULT 160;
ALTER TABLE configuracao ADD COLUMN custos_fixos_mensais DECIMAL(12,2) NOT NULL DEFAULT 0;
ALTER TABLE configuracao ADD COLUMN margem_padrao DECIMAL(5,2) NOT NULL DEFAULT 30;

-- Mais casas decimais para medir fio em metros/gramas
ALTER TABLE material MODIFY quantidade DECIMAL(12,3) NOT NULL DEFAULT 0;
ALTER TABLE material MODIFY estoque_minimo DECIMAL(12,3) NOT NULL DEFAULT 0;
ALTER TABLE material MODIFY custo_unitario DECIMAL(12,4) NOT NULL DEFAULT 0;

-- Produto = ficha técnica (receita de materiais + horas) e, se for pronta-entrega, estoque de peças prontas
CREATE TABLE produto (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome            VARCHAR(140) NOT NULL,
    descricao       TEXT,
    frente          VARCHAR(20) NOT NULL,
    horas_mao_obra  DECIMAL(8,2) NOT NULL DEFAULT 0,
    outros_custos   DECIMAL(12,2) NOT NULL DEFAULT 0,
    margem          DECIMAL(5,2),
    preco_venda     DECIMAL(12,2),
    pronta_entrega  BOOLEAN NOT NULL DEFAULT FALSE,
    estoque         INT NOT NULL DEFAULT 0,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em       DATETIME(6) NOT NULL
);
CREATE INDEX idx_produto_nome ON produto (nome);

CREATE TABLE produto_material (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    produto_id   BIGINT NOT NULL,
    material_id  BIGINT NOT NULL,
    quantidade   DECIMAL(12,3) NOT NULL,
    CONSTRAINT fk_pm_produto FOREIGN KEY (produto_id) REFERENCES produto (id),
    CONSTRAINT fk_pm_material FOREIGN KEY (material_id) REFERENCES material (id),
    CONSTRAINT uk_pm UNIQUE (produto_id, material_id)
);

-- Encomenda ligada à ficha
ALTER TABLE encomenda ADD COLUMN produto_id BIGINT;
ALTER TABLE encomenda ADD COLUMN quantidade INT NOT NULL DEFAULT 1;
ALTER TABLE encomenda ADD COLUMN horas_trabalhadas DECIMAL(8,2);
ALTER TABLE encomenda ADD COLUMN materiais_baixados BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE encomenda ADD CONSTRAINT fk_encomenda_produto FOREIGN KEY (produto_id) REFERENCES produto (id);

-- Vendas de pronta-entrega (ateliê, feira, Instagram…)
CREATE TABLE venda (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_venda       DATE NOT NULL,
    cliente_id       BIGINT,
    canal            VARCHAR(20) NOT NULL,
    forma_pagamento  VARCHAR(20) NOT NULL,
    desconto         DECIMAL(12,2) NOT NULL DEFAULT 0,
    total            DECIMAL(12,2) NOT NULL,
    observacao       VARCHAR(255),
    status           VARCHAR(20) NOT NULL,
    criado_em        DATETIME(6) NOT NULL,
    CONSTRAINT fk_venda_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id)
);
CREATE INDEX idx_venda_data ON venda (data_venda);

CREATE TABLE venda_item (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    venda_id        BIGINT NOT NULL,
    produto_id      BIGINT,
    descricao       VARCHAR(160) NOT NULL,
    quantidade      INT NOT NULL,
    preco_unitario  DECIMAL(12,2) NOT NULL,
    custo_unitario  DECIMAL(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_vi_venda FOREIGN KEY (venda_id) REFERENCES venda (id),
    CONSTRAINT fk_vi_produto FOREIGN KEY (produto_id) REFERENCES produto (id)
);

-- Cada saída de material: por encomenda (ficha ou manual) ou produção para o estoque
CREATE TABLE consumo_material (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    material_id     BIGINT NOT NULL,
    encomenda_id    BIGINT,
    produto_id      BIGINT,
    origem          VARCHAR(20) NOT NULL,
    quantidade      DECIMAL(12,3) NOT NULL,
    custo_unitario  DECIMAL(12,4) NOT NULL,
    custo_total     DECIMAL(12,2) NOT NULL,
    data_consumo    DATE NOT NULL,
    criado_em       DATETIME(6) NOT NULL,
    CONSTRAINT fk_cm_material FOREIGN KEY (material_id) REFERENCES material (id),
    CONSTRAINT fk_cm_encomenda FOREIGN KEY (encomenda_id) REFERENCES encomenda (id),
    CONSTRAINT fk_cm_produto FOREIGN KEY (produto_id) REFERENCES produto (id)
);
CREATE INDEX idx_cm_encomenda ON consumo_material (encomenda_id);
CREATE INDEX idx_cm_data ON consumo_material (data_consumo);

ALTER TABLE lancamento ADD COLUMN venda_id BIGINT;
ALTER TABLE lancamento ADD CONSTRAINT fk_lancamento_venda FOREIGN KEY (venda_id) REFERENCES venda (id);
