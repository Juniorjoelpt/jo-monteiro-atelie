package br.com.mssolucoes.atelie.dominio;

import br.com.mssolucoes.atelie.dominio.Enums.TipoFoto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Foto ligada à encomenda. Fica numa pasta privada: só abre com login. */
@Entity
@Table(name = "encomenda_foto")
@Getter
@Setter
public class EncomendaFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encomenda_id", nullable = false)
    private Encomenda encomenda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoFoto tipo;

    @Column(nullable = false, length = 200)
    private String arquivo;

    @Column(length = 160)
    private String legenda;

    /** Preenchido quando a foto foi publicada no portfólio. */
    @Column(name = "portfolio_item_id")
    private Long portfolioItemId;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
