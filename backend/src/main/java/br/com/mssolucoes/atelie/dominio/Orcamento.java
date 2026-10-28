package br.com.mssolucoes.atelie.dominio;

import br.com.mssolucoes.atelie.dominio.Enums.Origem;
import br.com.mssolucoes.atelie.dominio.Enums.StatusOrcamento;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "orcamento")
@Getter
@Setter
public class Orcamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(nullable = false, length = 140)
    private String nome;

    @Column(length = 20)
    private String whatsapp;

    @Column(name = "tipo_peca", nullable = false, length = 80)
    private String tipoPeca;

    /** Peça do catálogo escolhida pela cliente (opcional). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Column(nullable = false)
    private int quantidade = 1;

    /** Nome, frase, tema, foto… o que vai estampado ou escrito na peça. */
    @Column(columnDefinition = "TEXT")
    private String personalizacao;

    @Column(length = 160)
    private String medidas;

    @Column(length = 160)
    private String cores;

    @Column(name = "data_desejada")
    private LocalDate dataDesejada;

    @Column(columnDefinition = "TEXT")
    private String detalhes;

    @Column(name = "valor_proposto", precision = 12, scale = 2)
    private BigDecimal valorProposto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOrcamento status = StatusOrcamento.NOVO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Origem origem = Origem.MANUAL;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
