package br.com.mssolucoes.atelie.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Conta que se repete todo mês: aluguel, energia, internet, MEI, contador… */
@Entity
@Table(name = "conta_fixa")
@Getter
@Setter
public class ContaFixa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    /** Valor previsto (pode mudar a cada mês na hora de pagar). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "dia_vencimento", nullable = false)
    private int diaVencimento;

    /** Entra no custo fixo por hora da precificação. */
    @Column(name = "entra_na_precificacao", nullable = false)
    private boolean entraNaPrecificacao = true;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(length = 255)
    private String observacoes;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
