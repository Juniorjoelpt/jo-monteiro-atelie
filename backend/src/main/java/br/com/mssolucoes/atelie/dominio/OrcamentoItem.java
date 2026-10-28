package br.com.mssolucoes.atelie.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Peça do catálogo que a cliente colocou na sacola do site e mandou no pedido de orçamento. */
@Entity
@Table(name = "orcamento_item")
@Getter
@Setter
public class OrcamentoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orcamento_id", nullable = false)
    private Orcamento orcamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private int quantidade = 1;
}
