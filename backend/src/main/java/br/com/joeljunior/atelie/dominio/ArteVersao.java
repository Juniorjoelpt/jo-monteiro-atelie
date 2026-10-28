package br.com.joeljunior.atelie.dominio;

import br.com.joeljunior.atelie.dominio.Enums.RespostaArte;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Uma versão da arte (mockup) de uma encomenda. A cliente vê pelo link da encomenda e responde:
 * aprova ou pede ajuste. Cada ajuste vira uma nova versão. O arquivo fica na pasta privada.
 */
@Entity
@Table(name = "arte_versao")
@Getter
@Setter
public class ArteVersao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encomenda_id", nullable = false)
    private Encomenda encomenda;

    /** 1, 2, 3… na ordem de envio. */
    @Column(nullable = false)
    private int numero;

    @Column(nullable = false, length = 200)
    private String arquivo;

    /** Recado da Jô junto com a arte ("troquei a fonte do nome", por exemplo). */
    @Column(length = 500)
    private String observacao;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RespostaArte resposta;

    @Column(name = "comentario_cliente", columnDefinition = "TEXT")
    private String comentarioCliente;

    /** true = a Jô marcou pelo sistema (a cliente respondeu pelo WhatsApp). */
    @Column(name = "respondida_no_sistema", nullable = false)
    private boolean respondidaNoSistema;

    @Column(name = "respondido_em")
    private LocalDateTime respondidoEm;

    /** Primeira vez que a cliente abriu esta versão pelo link. */
    @Column(name = "vista_em")
    private LocalDateTime vistaEm;

    @Column(name = "vista_ultima_em")
    private LocalDateTime vistaUltimaEm;

    /** Visitas (aberturas com mais de 30 minutos entre elas contam separado). */
    @Column(nullable = false)
    private int visualizacoes;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
