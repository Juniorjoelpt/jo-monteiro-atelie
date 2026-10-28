-- Etapa 8: acompanhamento do pedido pela cliente, arte vista, quadro de produção (urgente) e histórico de ações

-- Encomenda: urgente (sai primeiro no quadro) e última vez que a cliente abriu o link do pedido
ALTER TABLE encomenda ADD COLUMN urgente BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE encomenda ADD COLUMN acompanhado_em DATETIME(6);

-- Arte: quando a cliente abriu cada versão pela primeira e pela última vez, e quantas vezes
ALTER TABLE arte_versao ADD COLUMN vista_em DATETIME(6);
ALTER TABLE arte_versao ADD COLUMN vista_ultima_em DATETIME(6);
ALTER TABLE arte_versao ADD COLUMN visualizacoes INT NOT NULL DEFAULT 0;

-- Histórico: quem fez o quê e quando (encomendas, vendas, financeiro, estoque)
CREATE TABLE historico (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    area          VARCHAR(20) NOT NULL,
    encomenda_id  BIGINT,
    usuario_id    BIGINT,
    autor         VARCHAR(140) NOT NULL,
    acao          VARCHAR(40) NOT NULL,
    descricao     VARCHAR(600) NOT NULL,
    sensivel      BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em     DATETIME(6) NOT NULL,
    CONSTRAINT fk_historico_encomenda FOREIGN KEY (encomenda_id) REFERENCES encomenda (id) ON DELETE SET NULL,
    CONSTRAINT fk_historico_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE SET NULL
);
CREATE INDEX idx_historico_criado ON historico (criado_em);
CREATE INDEX idx_historico_encomenda ON historico (encomenda_id, criado_em);
CREATE INDEX idx_historico_usuario ON historico (usuario_id, criado_em);
CREATE INDEX idx_encomenda_urgente ON encomenda (urgente);
