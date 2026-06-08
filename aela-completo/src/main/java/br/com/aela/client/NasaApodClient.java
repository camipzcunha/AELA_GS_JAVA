package br.com.aela.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * CLIENTE: NasaApodClient
 *
 * ─── SOA — Integração entre Serviços ─────────────────────────────
 * Este componente implementa o requisito de INTEGRAÇÃO com serviço
 * externo: consome a NASA APOD API (Astronomy Picture of the Day)
 * e enriquece o contexto operacional da missão com dados reais
 * do ambiente espacial.
 *
 * A NASA APOD API é pública e não exige cadastro com DEMO_KEY.
 * URL base: https://api.nasa.gov/planetary/apod
 *
 * ─── Relevância para o AELA ──────────────────────────────────────
 * O ambiente espacial afeta diretamente os operadores. A API da NASA
 * fornece dados contextuais sobre condições astronômicas do dia da
 * missão — reforçando a narrativa de que o AELA usa infraestrutura
 * espacial para proteger o corpo humano em campo.
 *
 * ─── Uso ─────────────────────────────────────────────────────────
 * Chamado pelo MissaoController no endpoint GET /api/missoes/{id}/contexto
 * que retorna dados da missão + contexto astronômico da NASA.
 *
 * ─── Observação ──────────────────────────────────────────────────
 * A chave DEMO_KEY permite até 30 req/hora e 50 req/dia.
 * Para produção, cadastre uma chave em https://api.nasa.gov/
 * e configure nasa.api.key no application.properties.
 */
@Component
@Slf4j
public class NasaApodClient {

    private static final String APOD_URL =
            "https://api.nasa.gov/planetary/apod?api_key={apiKey}&date={date}";

    private final RestTemplate restTemplate;

    @Value("${nasa.api.key:DEMO_KEY}")
    private String apiKey;

    public NasaApodClient() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Consulta o contexto astronômico do dia atual na NASA APOD API.
     *
     * Retorna um mapa com:
     *   - titulo: título da imagem/fenômeno do dia
     *   - descricao: explicação científica
     *   - data: data da consulta
     *   - url: URL da imagem (opcional — não exibida por questão de copyright)
     *   - tipo: "image" ou "video"
     *
     * Em caso de falha na API (timeout, rate limit, rede), retorna um
     * mapa com mensagem de indisponibilidade — a API REST não falha
     * por conta de uma falha no serviço externo (resilience pattern).
     */
    public Map<String, String> consultarContextoAstronomico(String string) {
        return consultarContextoAstronomico(LocalDate.now().toString());
    }

    /**
     * Consulta o contexto astronômico de uma data específica.
     *
     */
    @SuppressWarnings({"unchecked", "unchecked"})
    public Map<String, String> consultarContextoAstronomico() {
        log.info("[AELA] Consultando NASA APOD API");
        Map<String, String> resultado = new HashMap<>();

        try {
            String url = "https://api.nasa.gov/planetary/apod?api_key=" + apiKey;
            Map<String, Object> apodResponse =
                    restTemplate.getForObject(url, Map.class);

            if (apodResponse != null) {
                resultado.put("fonte", "NASA Astronomy Picture of the Day");
                resultado.put("titulo",
                        String.valueOf(apodResponse.getOrDefault("title", "N/A")));
                resultado.put("descricao",
                        truncar(String.valueOf(apodResponse.getOrDefault("explanation", "")), 300));
                resultado.put("data",
                        String.valueOf(apodResponse.getOrDefault("date", "")));
                resultado.put("tipo",
                        String.valueOf(apodResponse.getOrDefault("media_type", "image")));
                resultado.put("status", "DISPONIVEL");

                log.info("[AELA] NASA APOD OK: {}", resultado.get("titulo"));
            }

        } catch (RestClientException e) {
            log.warn("[AELA] NASA APOD indisponível: {}", e.getMessage());
            resultado.put("status", "INDISPONIVEL");
            resultado.put("fonte", "NASA Astronomy Picture of the Day");
            resultado.put("mensagem", "Contexto astronômico temporariamente indisponível.");
        }

        return resultado;
    }

    /**
     * Trunca texto longo para evitar respostas excessivamente grandes.
     */
    private String truncar(String texto, int limite) {
        if (texto == null || texto.length() <= limite) return texto;
        return texto.substring(0, limite) + "...";
    }
}
