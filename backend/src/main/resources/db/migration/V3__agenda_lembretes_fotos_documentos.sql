-- Etapa 3: relatórios, proposta/contrato, agenda, lembretes e fotos por encomenda

-- Data em que a encomenda foi entregue (para os relatórios por período)
ALTER TABLE encomenda ADD COLUMN entregue_em DATE;
UPDATE encomenda SET entregue_em = COALESCE(prazo_entrega, CAST(criado_em AS DATE)) WHERE status = 'ENTREGUE';

-- Dados que aparecem na proposta e no contrato
ALTER TABLE configuracao ADD COLUMN responsavel VARCHAR(120);
ALTER TABLE configuracao ADD COLUMN documento VARCHAR(30);
ALTER TABLE configuracao ADD COLUMN sinal_percentual DECIMAL(5,2) NOT NULL DEFAULT 50;
ALTER TABLE configuracao ADD COLUMN validade_proposta_dias INT NOT NULL DEFAULT 15;
ALTER TABLE configuracao ADD COLUMN condicoes_proposta TEXT;
ALTER TABLE configuracao ADD COLUMN politica_cancelamento TEXT;

ALTER TABLE cliente ADD COLUMN documento VARCHAR(30);
ALTER TABLE cliente ADD COLUMN endereco VARCHAR(255);

-- Compromissos da agenda: visita técnica, prova, reunião, produção…
CREATE TABLE compromisso (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo        VARCHAR(160) NOT NULL,
    tipo          VARCHAR(20) NOT NULL,
    inicio        DATETIME(6) NOT NULL,
    fim           DATETIME(6),
    local_evento  VARCHAR(160),
    cliente_id    BIGINT,
    encomenda_id  BIGINT,
    observacoes   TEXT,
    concluido     BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em     DATETIME(6) NOT NULL,
    CONSTRAINT fk_comp_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id),
    CONSTRAINT fk_comp_encomenda FOREIGN KEY (encomenda_id) REFERENCES encomenda (id)
);
CREATE INDEX idx_comp_inicio ON compromisso (inicio);

-- Lembretes de WhatsApp já enviados (para não aparecerem de novo)
CREATE TABLE lembrete (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    chave       VARCHAR(120) NOT NULL,
    enviado_em  DATETIME(6) NOT NULL,
    CONSTRAINT uk_lembrete_chave UNIQUE (chave)
);

-- Fotos da encomenda: referência mandada pela cliente e peça pronta (arquivos privados)
CREATE TABLE encomenda_foto (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    encomenda_id       BIGINT NOT NULL,
    tipo               VARCHAR(20) NOT NULL,
    arquivo            VARCHAR(200) NOT NULL,
    legenda            VARCHAR(160),
    portfolio_item_id  BIGINT,
    criado_em          DATETIME(6) NOT NULL,
    CONSTRAINT fk_foto_encomenda FOREIGN KEY (encomenda_id) REFERENCES encomenda (id),
    CONSTRAINT fk_foto_portfolio FOREIGN KEY (portfolio_item_id) REFERENCES portfolio_item (id) ON DELETE SET NULL
);
CREATE INDEX idx_foto_encomenda ON encomenda_foto (encomenda_id);
