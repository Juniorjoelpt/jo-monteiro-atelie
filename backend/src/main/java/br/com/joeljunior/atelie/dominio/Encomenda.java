package br.com.joeljunior.atelie.dominio;

import br.com.joeljunior.atelie.dominio.Enums.Frente;
import br.com.joeljunior.atelie.dominio.Enums.StatusArte;
import br.com.joeljunior.atelie.dominio.Enums.StatusEncomenda;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "encomenda")
@Getter
@Setter
public class Encomenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orcamento_id")
    private Orcamento orcamento;

    /** Ficha técnica usada (opcional). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Column(nullable = false)
    private int quantidade = 1;

    /** Horas reais; se vazio, usa as horas da ficha × quantidade. */
    @Column(name = "horas_trabalhadas", precision = 8, scale = 2)
    private BigDecimal horasTrabalhadas;

    @Column(name = "materiais_baixados", nullable = false)
    private boolean materiaisBaixados;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Frente frente = Frente.PAPELARIA;

    @Column(name = "valor_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @Column(name = "valor_sinal", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorSinal = BigDecimal.ZERO;

    @Column(name = "prazo_entrega")
    private LocalDate prazoEntrega;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusEncomenda status = StatusEncomenda.AGUARDANDO_SINAL;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    /** O que vai escrito/estampado: nome, frase, tema, foto… */
    @Column(columnDefinition = "TEXT")
    private String personalizacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_arte", nullable = false, length = 20)
    private StatusArte statusArte = StatusArte.SEM_ARTE;

    /** Código do link do pedido: a cliente acompanha o andamento e aprova a arte (gerado ao criar a encomenda). */
    @Column(name = "codigo_arte", length = 36)
    private String codigoArte;

    @Column(name = "entregue_em")
    private LocalDate entregueEm;

    /** Sai primeiro no quadro de produção e ganha destaque na lista. */
    @Column(nullable = false)
    private boolean urgente;

    /** Última vez que a cliente abriu o link do pedido (sem contar quem está logada no sistema). */
    @Column(name = "acompanhado_em")
    private LocalDateTime acompanhadoEm;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @PrePersist
    void gerarCodigo() {
        if (codigoArte == null) {
            codigoArte = java.util.UUID.randomUUID().toString();
        }
    }
}
