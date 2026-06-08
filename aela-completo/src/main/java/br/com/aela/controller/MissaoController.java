package br.com.aela.controller;

import br.com.aela.client.NasaApodClient;
import br.com.aela.dto.AelaDto;
import br.com.aela.exception.AelaException;
import br.com.aela.model.Missao;
import br.com.aela.model.Operador;
import br.com.aela.repository.MissaoRepository;
import br.com.aela.repository.OperadorRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CONTROLLER: MissaoController
 *
 * Gestão de missões e tripulação.
 *
 * ─── Integração com Serviço Externo (SOA) ────────────────────────
 * O endpoint GET /api/missoes/{id}/contexto demonstra a integração
 * com a NASA APOD API — requisito obrigatório de integração entre
 * serviços da Global Solution. O AELA enriquece os dados da missão
 * com o contexto astronômico do dia, reforçando a narrativa de que
 * infraestrutura espacial sustenta o monitoramento humano em campo.
 */
@RestController
@RequestMapping("/api/missoes")
@RequiredArgsConstructor
@Slf4j
public class MissaoController {

    private final MissaoRepository missaoRepository;
    private final OperadorRepository operadorRepository;
    private final NasaApodClient nasaApodClient;  // integração com serviço externo

    /** POST /api/missoes — Cria nova missão */
    @PostMapping
    @Transactional
    public ResponseEntity<AelaDto.MissaoResponse> criar(
            @Valid @RequestBody AelaDto.MissaoRequest request) {

        Missao missao = Missao.builder()
                .nome(request.getNome())
                .descricao(request.getDescricao())
                .tipoAmbiente(request.getTipoAmbiente())
                .status("PLANEJADA")
                .build();

        Missao salva = missaoRepository.save(missao);
        log.info("[AELA] Missão criada: {} (ID: {})", salva.getNome(), salva.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(salva));
    }

    /** GET /api/missoes — Lista todas as missões */
    @GetMapping
    public ResponseEntity<List<AelaDto.MissaoResponse>> listar() {
        return ResponseEntity.ok(
            missaoRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList())
        );
    }

    @GetMapping("/teste-nasa")
    public ResponseEntity<String> testeNasa() {
        try {
            RestTemplate rt = new RestTemplate();
            String url = "https://api.nasa.gov/planetary/apod?api_key=DEMO_KEY";
            String response = rt.getForObject(url, String.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    /** GET /api/missoes/{id} — Detalha uma missão */
    @GetMapping("/{id}")
    public ResponseEntity<AelaDto.MissaoResponse> buscar(@PathVariable Long id) {
        Missao missao = missaoRepository.findById(id)
                .orElseThrow(() -> new AelaException.RecursoNaoEncontradoException("Missão", id));
        return ResponseEntity.ok(toResponse(missao));
    }

    /**
     * GET /api/missoes/{id}/contexto
     *
     * ─── INTEGRAÇÃO COM SERVIÇO EXTERNO ──────────────────────────
     * Retorna os dados da missão ENRIQUECIDOS com o contexto
     * astronômico atual consultado na NASA APOD API.
     *
     * Demonstra a integração entre a API REST do AELA e um
     * serviço externo REST (NASA), conforme exigido pela Global Solution.
     *
     * Resposta:
     * {
     *   "missao": { ...dados da missão... },
     *   "contextoAstronomico": {
     *     "fonte": "NASA Astronomy Picture of the Day",
     *     "titulo": "...",
     *     "descricao": "...",
     *     "data": "2026-06-08",
     *     "tipo": "image",
     *     "status": "DISPONIVEL"
     *   }
     * }
     */
    @GetMapping("/{id}/contexto")
    public ResponseEntity<Map<String, Object>> contextoMissao(@PathVariable Long id) {

        log.info("[AELA] Consultando contexto da missão ID: {}", id);

        Missao missao = missaoRepository.findById(id)
                .orElseThrow(() -> new AelaException.RecursoNaoEncontradoException("Missão", id));

        // Consulta a NASA APOD API — integração com serviço externo REST
        Map<String, String> contextoNasa = nasaApodClient.consultarContextoAstronomico();

        // Monta a resposta combinada: dados internos + dados externos
        Map<String, Object> resposta = new HashMap<>();
        resposta.put("missao", toResponse(missao));
        resposta.put("contextoAstronomico", contextoNasa);
        resposta.put("integracaoDescricao",
                "Dados astronômicos consultados em tempo real na NASA APOD API " +
                "(https://api.nasa.gov/planetary/apod). " +
                "O AELA usa infraestrutura espacial para contextualizar missões humanas em ambientes extremos.");

        return ResponseEntity.ok(resposta);
    }

    /**
     * POST /api/missoes/{id}/tripulacao/{operadorId}
     * Adiciona um operador à tripulação da missão.
     */
    @PostMapping("/{id}/tripulacao/{operadorId}")
    @Transactional
    public ResponseEntity<AelaDto.MissaoResponse> adicionarTripulante(
            @PathVariable Long id,
            @PathVariable Long operadorId) {

        Missao missao = missaoRepository.findById(id)
                .orElseThrow(() -> new AelaException.RecursoNaoEncontradoException("Missão", id));

        Operador operador = operadorRepository.findById(operadorId)
                .orElseThrow(() -> new AelaException.RecursoNaoEncontradoException("Operador", operadorId));

        boolean jaEstaNaTripulacao = missao.getTripulacao().stream()
                .anyMatch(o -> o.getId().equals(operadorId));

        if (jaEstaNaTripulacao) {
            throw new AelaException.RegraDeNegocioException(
                operador.getNome() + " já está na tripulação da missão " + missao.getNome()
            );
        }

        missao.getTripulacao().add(operador);
        missaoRepository.save(missao);

        log.info("[AELA] {} adicionado à tripulação de {}", operador.getNome(), missao.getNome());
        return ResponseEntity.ok(toResponse(missao));
    }

    /**
     * PATCH /api/missoes/{id}/iniciar
     * Inicia a missão — muda status para EM_ANDAMENTO e registra dataInicio.
     */
    @PatchMapping("/{id}/iniciar")
    @Transactional
    public ResponseEntity<AelaDto.MissaoResponse> iniciar(@PathVariable Long id) {

        Missao missao = missaoRepository.findById(id)
                .orElseThrow(() -> new AelaException.RecursoNaoEncontradoException("Missão", id));

        if (!"PLANEJADA".equals(missao.getStatus())) {
            throw new AelaException.RegraDeNegocioException(
                "Apenas missões com status PLANEJADA podem ser iniciadas. Status atual: " + missao.getStatus()
            );
        }

        missao.setStatus("EM_ANDAMENTO");
        missao.setDataInicio(LocalDateTime.now());
        missaoRepository.save(missao);

        log.info("[AELA] Missão {} iniciada em {}", missao.getNome(), missao.getDataInicio());
        return ResponseEntity.ok(toResponse(missao));
    }

    /**
     * PATCH /api/missoes/{id}/encerrar
     * Encerra a missão — inicia fase de acompanhamento pós-missão.
     */
    @PatchMapping("/{id}/encerrar")
    @Transactional
    public ResponseEntity<AelaDto.MissaoResponse> encerrar(@PathVariable Long id) {

        Missao missao = missaoRepository.findById(id)
                .orElseThrow(() -> new AelaException.RecursoNaoEncontradoException("Missão", id));

        missao.setStatus("CONCLUIDA");
        missao.setDataFim(LocalDateTime.now());
        missaoRepository.save(missao);

        log.info("[AELA] Missão {} concluída. Iniciando fase pós-missão.", missao.getNome());
        return ResponseEntity.ok(toResponse(missao));
    }

    // ── Helper ───────────────────────────────────────────────────────
    private AelaDto.MissaoResponse toResponse(Missao m) {
        return AelaDto.MissaoResponse.builder()
                .id(m.getId())
                .nome(m.getNome())
                .descricao(m.getDescricao())
                .tipoAmbiente(m.getTipoAmbiente())
                .status(m.getStatus())
                .totalTripulacao(m.getTripulacao() != null ? m.getTripulacao().size() : 0)
                .dataInicio(m.getDataInicio() != null ? m.getDataInicio().toString() : null)
                .dataFim(m.getDataFim() != null ? m.getDataFim().toString() : null)
                .dataCadastro(m.getDataCadastro() != null ? m.getDataCadastro().toString() : null)
                .build();
    }
}
