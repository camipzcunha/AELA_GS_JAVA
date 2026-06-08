package br.com.aela.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ENTIDADE: AlertaFisiologico
 *
 * Registra alertas críticos gerados pelo sistema AELA ou por sistemas
 * externos (wearables, satélites, outros módulos).
 *
 * ─── SOA ─────────────────────────────────────────────────────────
 * Esta entidade é o payload persistido pela operação SOAP
 * "registrarAlerta". Separa claramente o dado (entidade JPA)
 * da lógica de processamento (AlertaService).
 *
 * ─── Tipos de alerta ─────────────────────────────────────────────
 * CARDIOVASCULAR, OCULAR, COGNITIVO, SONO, SATURACAO_O2, GERAL
 *
 * ─── Severidade ──────────────────────────────────────────────────
 * BAIXA → log apenas
 * MEDIA → notificação ao comandante
 * ALTA  → recomendação de restrição
 * CRITICA → afastamento imediato
 */
@Entity
@Table(name = "TB_ALERTA_FISIOLOGICO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertaFisiologico {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "seq_alerta"
    )
    @SequenceGenerator(
        name = "seq_alerta",
        sequenceName = "SEQ_ALERTA",
        allocationSize = 1
    )
    @Column(name = "ID_ALERTA")
    private Long id;

    // ── Relacionamento com Operador ──────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_OPERADOR", nullable = false)
    private Operador operador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_MISSAO")
    private Missao missao; // nullable: alerta pode ocorrer fora de missão

    // ── Classificação do alerta ──────────────────────────────────────

    @Column(name = "TP_ALERTA", nullable = false, length = 30)
    private String tipoAlerta;

    @Column(name = "DS_SEVERIDADE", nullable = false, length = 20)
    private String severidade;

    @Column(name = "DS_ALERTA", nullable = false, length = 500)
    private String descricao;

    // ── Valores que dispararam o alerta ─────────────────────────────

    @Column(name = "NR_VALOR_METRICA")
    private Double valorMetrica;

    @Column(name = "NR_VALOR_BASELINE")
    private Double valorBaseline;

    // ── Origem do alerta ─────────────────────────────────────────────
    // WEARABLE | SATELLITE | MANUAL | SISTEMA_AELA

    @Column(name = "DS_ORIGEM", length = 30)
    private String origem;

    // ── Controle ─────────────────────────────────────────────────────

    @Column(name = "DT_REGISTRO", nullable = false, updatable = false)
    private LocalDateTime dataRegistro;

    @Column(name = "FL_RESOLVIDO")
    @Builder.Default
    private Boolean resolvido = false;

    @PrePersist
    public void prePersist() {
        this.dataRegistro = LocalDateTime.now();
        if (this.resolvido == null) this.resolvido = false;
    }
}
