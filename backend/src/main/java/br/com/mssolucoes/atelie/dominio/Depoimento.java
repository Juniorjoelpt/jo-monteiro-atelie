package br.com.mssolucoes.atelie.dominio;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** O que as clientes disseram, para mostrar no site. */
@Entity
@Table(name = "depoimento")
@Getter
@Setter
public class Depoimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    /** "Mãe da Alice · kit de aniversário", "Professora · Dia dos Professores"… */
    @Column(length = 120)
    private String contexto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Column(nullable = false)
    private int estrelas = 5;

    /** Arquivo na pasta pública de uploads (opcional). */
    @Column(length = 200)
    private String foto;

    @Column(nullable = false)
    private boolean publicado = true;

    @Column(nullable = false)
    private int ordem;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
