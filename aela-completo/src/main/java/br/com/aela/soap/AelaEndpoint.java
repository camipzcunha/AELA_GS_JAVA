package br.com.aela.soap;

import br.com.aela.dto.AelaDto;
import br.com.aela.exception.AelaException;
import br.com.aela.model.*;
import br.com.aela.repository.*;
import br.com.aela.service.ReadinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import java.time.LocalDateTime;

/**
 * ENDPOINT SOAP: AelaEndpoint
 *
 * ─── SOA ─────────────────────────────────────────────────────────
 * Este endpoint implementa o Web Service SOAP do AELA conforme
 * o contrato definido em aela.xsd. Cada método anotado com
 * @PayloadRoot é uma operação do serviço, identificada pelo
 * elemento XML raiz da requisição.
 *
 * ─── Integração REST ↔ SOAP ───────────────────────────────────────
 * A integração entre os serviços ocorre aqui: o endpoint SOAP
 * reutiliza o ReadinessService — a mesma lógica de negócio que
 * serve a API REST. Isso demonstra o princípio de REUTILIZAÇÃO
 * DE SERVIÇOS da SOA: a lógica existe uma vez, exposta por
 * múltiplos protocolos.
 *
 * ─── Namespace ────────────────────────────────────────────────────
 * NAMESPACE deve ser igual ao targetNamespace do XSD (aela.xsd).
 *
 * ─── Operações disponíveis ────────────────────────────────────────
 * 1. consultarReadiness  → consulta ReadinessScore via SOAP
 * 2. registrarAlerta     → cadastra alerta fisiológico crítico
 *
 * WSDL: http://localhost:8080/ws/aela.wsdl
 * Testar com SoapUI: importe o WSDL e execute as operações.
 */
@Endpoint
@RequiredArgsConstructor
@Slf4j
public class AelaEndpoint {

    private static final String NAMESPACE_URI = "http://aela.com.br/soap";

    // ── Dependências — reutiliza serviços já existentes (SOA) ────────
    private final ReadinessService readinessService;
    private final OperadorRepository operadorRepository;
    private final MissaoRepository missaoRepository;
    private final AlertaFisiologicoRepository alertaRepository;

    // ════════════════════════════════════════════════════════════════
    // OPERAÇÃO 1: consultarReadiness
    // Consulta o ReadinessScore de um operador via SOAP.
    //
    // Envelope SOAP de exemplo (SoapUI):
    //
    // <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
    //                   xmlns:tns="http://aela.com.br/soap">
    //    <soapenv:Header/>
    //    <soapenv:Body>
    //       <tns:consultarReadinessRequest>
    //          <tns:operadorId>1</tns:operadorId>
    //          <tns:tipoTarefa>EVA</tns:tipoTarefa>
    //       </tns:consultarReadinessRequest>
    //    </soapenv:Body>
    // </soapenv:Envelope>
    // ════════════════════════════════════════════════════════════════

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "consultarReadinessRequest")
    @ResponsePayload
    public ConsultarReadinessResponse consultarReadiness(
            @RequestPayload ConsultarReadinessRequest request) {

        log.info("[AELA-SOAP] consultarReadiness: operadorId={}, tarefa={}",
                request.getOperadorId(), request.getTipoTarefa());

        ConsultarReadinessResponse response = new ConsultarReadinessResponse();

        try {
            // Converte a string do XML para o enum TipoTarefa
            TipoTarefa tipoTarefa = TipoTarefa.valueOf(request.getTipoTarefa().toUpperCase());

            // INTEGRAÇÃO SOAP ↔ REST:
            // Reutiliza o mesmo ReadinessService da API REST.
            // A lógica de negócio existe uma única vez — princípio SOA.
            AelaDto.ReadinessScoreResponse score =
                    readinessService.calcular(request.getOperadorId(), tipoTarefa);

            // Mapeia o resultado para o envelope de resposta SOAP
            response.setOperadorId(score.getOperadorId());
            response.setOperadorNome(score.getOperadorNome());
            response.setTipoTarefa(score.getTipoTarefa());
            response.setScoreGeral(score.getScoreGeral());
            response.setDesvioCardiovascular(score.getDesvioCardiovascular());
            response.setDesvioSensoriomotor(score.getDesvioSensoriomotor());
            response.setDesvioOcular(score.getDesvioOcular());
            response.setDesvioCognitivo(score.getDesvioCognitivo());
            response.setDesvioRecuperacao(score.getDesvioRecuperacao());
            response.setClassificacao(score.getClassificacao());
            response.setRecomendacao(score.getRecomendacao());
            response.setDataCalculo(score.getDataCalculo());
            response.setCodigoRetorno(200);
            response.setMensagemRetorno("ReadinessScore calculado com sucesso.");

            log.info("[AELA-SOAP] consultarReadiness OK: score={}, classificacao={}",
                    score.getScoreGeral(), score.getClassificacao());

        } catch (IllegalArgumentException e) {
            // TipoTarefa inválido
            log.warn("[AELA-SOAP] TipoTarefa inválido: {}", request.getTipoTarefa());
            response.setCodigoRetorno(400);
            response.setMensagemRetorno("TipoTarefa inválido: " + request.getTipoTarefa() +
                    ". Valores aceitos: EVA, OPERACAO_COGNITIVA, TAREFA_FISICA, PILOTAGEM, MONITORAMENTO, RESGATE");

        } catch (AelaException.RecursoNaoEncontradoException e) {
            log.warn("[AELA-SOAP] Operador não encontrado: {}", request.getOperadorId());
            response.setCodigoRetorno(404);
            response.setMensagemRetorno(e.getMessage());

        } catch (AelaException.RegraDeNegocioException e) {
            log.warn("[AELA-SOAP] Regra de negócio: {}", e.getMessage());
            response.setCodigoRetorno(422);
            response.setMensagemRetorno(e.getMessage());

        } catch (Exception e) {
            log.error("[AELA-SOAP] Erro inesperado em consultarReadiness", e);
            response.setCodigoRetorno(500);
            response.setMensagemRetorno("Erro interno do servidor. Consulte os logs.");
        }

        return response;
    }

    // ════════════════════════════════════════════════════════════════
    // OPERAÇÃO 2: registrarAlerta
    // Cadastra um alerta fisiológico crítico no banco Oracle.
    //
    // Envelope SOAP de exemplo (SoapUI):
    //
    // <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
    //                   xmlns:tns="http://aela.com.br/soap">
    //    <soapenv:Header/>
    //    <soapenv:Body>
    //       <tns:registrarAlertaRequest>
    //          <tns:operadorId>1</tns:operadorId>
    //          <tns:missaoId>1</tns:missaoId>
    //          <tns:tipoAlerta>CARDIOVASCULAR</tns:tipoAlerta>
    //          <tns:severidade>ALTA</tns:severidade>
    //          <tns:descricao>Frequência cardíaca acima de 150 bpm por mais de 5 minutos</tns:descricao>
    //          <tns:valorMetrica>158.0</tns:valorMetrica>
    //          <tns:valorBaseline>65.0</tns:valorBaseline>
    //          <tns:origem>WEARABLE</tns:origem>
    //       </tns:registrarAlertaRequest>
    //    </soapenv:Body>
    // </soapenv:Envelope>
    // ════════════════════════════════════════════════════════════════

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "registrarAlertaRequest")
    @ResponsePayload
    public RegistrarAlertaResponse registrarAlerta(
            @RequestPayload RegistrarAlertaRequest request) {

        log.info("[AELA-SOAP] registrarAlerta: operadorId={}, tipo={}, severidade={}",
                request.getOperadorId(), request.getTipoAlerta(), request.getSeveridade());

        RegistrarAlertaResponse response = new RegistrarAlertaResponse();

        try {
            // Busca o operador no banco
            Operador operador = operadorRepository.findById(request.getOperadorId())
                    .orElseThrow(() -> new AelaException.RecursoNaoEncontradoException(
                            "Operador", request.getOperadorId()));

            // Missão é opcional
            Missao missao = null;
            if (request.getMissaoId() != null) {
                missao = missaoRepository.findById(request.getMissaoId()).orElse(null);
            }

            // Valida severidade
            String severidade = request.getSeveridade().toUpperCase();
            if (!severidade.matches("BAIXA|MEDIA|ALTA|CRITICA")) {
                throw new IllegalArgumentException(
                        "Severidade inválida: " + request.getSeveridade() +
                        ". Valores aceitos: BAIXA, MEDIA, ALTA, CRITICA");
            }

            // Constrói e persiste o alerta
            AlertaFisiologico alerta = AlertaFisiologico.builder()
                    .operador(operador)
                    .missao(missao)
                    .tipoAlerta(request.getTipoAlerta().toUpperCase())
                    .severidade(severidade)
                    .descricao(request.getDescricao())
                    .valorMetrica(request.getValorMetrica())
                    .valorBaseline(request.getValorBaseline())
                    .origem(request.getOrigem() != null ? request.getOrigem() : "SISTEMA_AELA")
                    .build();

            AlertaFisiologico salvo = alertaRepository.save(alerta);

            // Monta a resposta
            response.setAlertaId(salvo.getId());
            response.setOperadorNome(operador.getNome());
            response.setTipoAlerta(salvo.getTipoAlerta());
            response.setSeveridade(salvo.getSeveridade());
            response.setDataRegistro(salvo.getDataRegistro().toString());
            response.setSucesso(true);
            response.setCodigoRetorno(201);
            response.setMensagem("Alerta registrado com sucesso. ID: " + salvo.getId());

            log.info("[AELA-SOAP] registrarAlerta OK: alertaId={}, operador={}",
                    salvo.getId(), operador.getNome());

        } catch (IllegalArgumentException e) {
            log.warn("[AELA-SOAP] Parâmetro inválido em registrarAlerta: {}", e.getMessage());
            response.setSucesso(false);
            response.setCodigoRetorno(400);
            response.setMensagem(e.getMessage());

        } catch (AelaException.RecursoNaoEncontradoException e) {
            log.warn("[AELA-SOAP] Operador não encontrado: {}", request.getOperadorId());
            response.setSucesso(false);
            response.setCodigoRetorno(404);
            response.setMensagem(e.getMessage());

        } catch (Exception e) {
            log.error("[AELA-SOAP] Erro inesperado em registrarAlerta", e);
            response.setSucesso(false);
            response.setCodigoRetorno(500);
            response.setMensagem("Erro interno ao registrar alerta. Consulte os logs.");
        }

        return response;
    }
}
