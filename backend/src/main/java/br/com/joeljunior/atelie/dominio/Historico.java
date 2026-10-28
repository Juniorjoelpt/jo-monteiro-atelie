package br.com.joeljunior.atelie.dominio;

import br.com.joeljunior.atelie.dominio.Enums.AreaHistorico;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Uma ação registrada: quem fez, o quê e quando. Serve para esclarecer depois quem mudou um status,
 * um valor ou o estoque. "sensivel" = fala de dinheiro (a ajudante não vê).
 */
@Entity
@Table(name = "historico")
@Getter
@Setter
public class Historico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AreaHistorico area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encomenda_id")
    private Encomenda encomenda;

    @Column(name = "usuario_id")
    private Long usuarioId;

    /** Nome de quem fez (fica mesmo se o acesso for apagado), ou "Cliente (pelo link)". */
    @Column(nullable = false, length = 140)
    private String autor;

    @Column(nullable = false, length = 40)
    private String acao;

    @Column(nullable = false, length = 600)
    private String descricao;

    @Column(nullable = false)
    private boolean sensivel;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
