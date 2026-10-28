package br.com.mssolucoes.atelie.dominio;

import br.com.mssolucoes.atelie.dominio.Enums.Frente;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Ficha técnica de uma peça: materiais, horas e outros custos.
 * Se for de pronta-entrega, também guarda quantas peças prontas existem.
 */
@Entity
@Table(name = "produto")
@Getter
@Setter
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 140)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Frente frente = Frente.PAPELARIA;

    @Column(name = "horas_mao_obra", nullable = false, precision = 8, scale = 2)
    private BigDecimal horasMaoObra = BigDecimal.ZERO;

    /** Embalagem, tag, argola comprada pronta… por peça. */
    @Column(name = "outros_custos", nullable = false, precision = 12, scale = 2)
    private BigDecimal outrosCustos = BigDecimal.ZERO;

    /** Margem em %; vazio = usa a margem padrão das configurações. */
    @Column(precision = 5, scale = 2)
    private BigDecimal margem;

    /** Preço que ela cobra; vazio = usa o preço sugerido. */
    @Column(name = "preco_venda", precision = 12, scale = 2)
    private BigDecimal precoVenda;

    @Column(name = "pronta_entrega", nullable = false)
    private boolean prontaEntrega;

    @Column(nullable = false)
    private int estoque;

    @Column(nullable = false)
    private boolean ativo = true;

    @OneToMany(mappedBy = "produto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<ProdutoMaterial> materiais = new ArrayList<>();

    // ---- Catálogo do site ----

    @Column(name = "no_catalogo", nullable = false)
    private boolean noCatalogo;

    /** Falso = o site mostra "sob consulta". */
    @Column(name = "mostrar_preco", nullable = false)
    private boolean mostrarPreco = true;

    /** Texto para a cliente (a descrição da ficha é interna). */
    @Column(name = "texto_catalogo", columnDefinition = "TEXT")
    private String textoCatalogo;

    /** "Até 15 dias", "1 semana"… */
    @Column(name = "prazo_producao", length = 80)
    private String prazoProducao;

    @Column(name = "ordem_catalogo", nullable = false)
    private int ordemCatalogo;

    @OneToMany(mappedBy = "produto", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem, id")
    private List<ProdutoFoto> fotos = new ArrayList<>();

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
