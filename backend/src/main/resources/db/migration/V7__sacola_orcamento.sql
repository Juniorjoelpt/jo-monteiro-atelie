-- Etapa 7: a cliente monta uma sacola no site e pede orçamento de várias peças do catálogo de uma vez

CREATE TABLE orcamento_item (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    orcamento_id  BIGINT NOT NULL,
    produto_id    BIGINT NOT NULL,
    quantidade    INT NOT NULL,
    CONSTRAINT fk_oitem_orcamento FOREIGN KEY (orcamento_id) REFERENCES orcamento (id),
    CONSTRAINT fk_oitem_produto FOREIGN KEY (produto_id) REFERENCES produto (id)
);
CREATE INDEX idx_oitem_orcamento ON orcamento_item (orcamento_id);
