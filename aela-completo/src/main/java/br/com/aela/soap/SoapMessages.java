package br.com.aela.soap;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

/**
 * CLASSES SOAP — Request e Response
 *
 * Estas classes representam os envelopes XML das operações SOAP.
 * São mapeadas pelo Spring-WS via JAXB (Jakarta XML Binding).
 *
 * ─── Como o Spring-WS funciona ───────────────────────────────────
 * 1. Chega uma requisição SOAP com envelope XML
 * 2. Spring-WS extrai o <Body> do envelope
 * 3. JAXB desserializa o XML para a classe Java correspondente
 * 4. O método do @Endpoint é chamado com o objeto Java
 * 5. O objeto de resposta é serializado de volta para XML
 * 6. Spring-WS envolve em um envelope SOAP e retorna ao cliente
 *
 * ─── @XmlRootElement ─────────────────────────────────────────────
 * Mapeia a classe para o elemento XML raiz.
 * O namespace deve ser igual ao targetNamespace do XSD (aela.xsd).
 */
public class SoapMessages {

    // ════════════════════════════════════════════════════════════════
    // OPERAÇÃO 1: consultarReadiness
    // ════════════════════════════════════════════════════════════════

    /**
     * Não usado diretamente — o Spring-WS usa o XSD para validar.
     * Mantido aqui como documentação da estrutura esperada.
     */
}

// ─── consultarReadinessRequest ────────────────────────────────────

@Data
@XmlRootElement(name = "consultarReadinessRequest", namespace = "http://aela.com.br/soap")
@XmlAccessorType(XmlAccessType.FIELD)
class ConsultarReadinessRequest {

    @XmlElement(required = true)
    private Long operadorId;

    @XmlElement(required = true)
    private String tipoTarefa;

    @XmlElement(required = false)
    private String token;
}

// ─── consultarReadinessResponse ───────────────────────────────────

@Data
@XmlRootElement(name = "consultarReadinessResponse", namespace = "http://aela.com.br/soap")
@XmlAccessorType(XmlAccessType.FIELD)
class ConsultarReadinessResponse {

    private Long operadorId;
    private String operadorNome;
    private String tipoTarefa;
    private Double scoreGeral;
    private Double desvioCardiovascular;
    private Double desvioSensoriomotor;
    private Double desvioOcular;
    private Double desvioCognitivo;
    private Double desvioRecuperacao;
    private String classificacao;
    private String recomendacao;
    private String dataCalculo;
    private int codigoRetorno;
    private String mensagemRetorno;
}

// ════════════════════════════════════════════════════════════════
// OPERAÇÃO 2: registrarAlerta
// ════════════════════════════════════════════════════════════════

// ─── registrarAlertaRequest ───────────────────────────────────────

@Data
@XmlRootElement(name = "registrarAlertaRequest", namespace = "http://aela.com.br/soap")
@XmlAccessorType(XmlAccessType.FIELD)
class RegistrarAlertaRequest {

    @XmlElement(required = true)
    private Long operadorId;

    @XmlElement(required = false)
    private Long missaoId;

    @XmlElement(required = true)
    private String tipoAlerta;

    @XmlElement(required = true)
    private String severidade;

    @XmlElement(required = true)
    private String descricao;

    @XmlElement(required = false)
    private Double valorMetrica;

    @XmlElement(required = false)
    private Double valorBaseline;

    @XmlElement(required = false)
    private String origem;

    @XmlElement(required = false)
    private String token;
}

// ─── registrarAlertaResponse ──────────────────────────────────────

@Data
@XmlRootElement(name = "registrarAlertaResponse", namespace = "http://aela.com.br/soap")
@XmlAccessorType(XmlAccessType.FIELD)
class RegistrarAlertaResponse {

    private Long alertaId;
    private String operadorNome;
    private String tipoAlerta;
    private String severidade;
    private String dataRegistro;
    private Boolean sucesso;
    private int codigoRetorno;
    private String mensagem;
}
