# AELA — Adaptive Extremes Life Analytics
### API Java | FIAP Global Solution 2026 | Space Connect

---

## Visão Geral

O **AELA** é o back-end principal do sistema de monitoramento fisiológico individualizado para operadores em ambientes extremos: astronautas, bombeiros, alpinistas, médicos de campo e equipes de resgate.

Esta API gerencia operadores, missões, baselines fisiológicos e leituras em tempo real, calculando o **ReadinessScore** — o score de prontidão operacional individual por tipo de tarefa.

**O que nos diferencia:** sistemas existentes comparam o operador com a média da população.  
O AELA compara o operador **com ele mesmo**, antes, durante e depois da missão.

A solução expõe **API REST** e **Web Service SOAP** compartilhando a mesma camada de lógica de negócio, e integra com a **NASA APOD API** para enriquecer o contexto operacional das missões com dados astronômicos reais.

---

## Integrantes

| Nome | RM |
|---|---|
| Camila Pedroza da Cunha | RM 558768 |
| Nicolli Amy Kassa | RM 559104 |
| Isabelle Dallabeneta Carlesso | RM 554592 |

---

## Conexão com o Tema Espacial e ODS

A API resolve um problema documentado pelo **NASA Human Research Program**: nenhum sistema hoje traduz o estado fisiológico individual de um astronauta em uma decisão operacional concreta.

**ODS relacionados:**
- ODS 3 — Saúde e Bem-Estar
- ODS 9 — Indústria, Inovação e Infraestrutura
- ODS 8 — Trabalho Decente e Crescimento Econômico

---

## Stack Tecnológica

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 17 | Linguagem |
| Spring Boot | 3.3.4 | Framework principal |
| Spring Data JPA | (via Boot) | ORM / Repositórios |
| Spring Web Services | (via Boot) | Web Service SOAP |
| Spring Validation | (via Boot) | Validação de DTOs |
| Lombok | (via Boot) | Redução de boilerplate |
| WSDL4J | 1.6.x | Geração automática do WSDL |
| Oracle Database | 23c / XE | Persistência |
| ojdbc11 | 23.4.0.24.05 | Driver JDBC Oracle |
| JUnit 5 + Mockito | (via Boot) | Testes unitários |
| NASA APOD API | REST externo | Integração com serviço espacial externo |

---

## Estrutura do Projeto

```
aela-api/
├── src/main/java/br/com/aela/
│   ├── AelaApplication.java
│   ├── client/
│   │   └── NasaApodClient.java        ← integração NASA APOD API
│   ├── config/
│   │   └── WebServiceConfig.java      ← configuração do endpoint SOAP
│   ├── controller/
│   │   ├── OperadorController.java    ← CRUD de operadores, baseline, leituras
│   │   ├── ReadinessController.java   ← cálculo de score e ranking
│   │   └── MissaoController.java      ← gestão de missões + integração NASA
│   ├── dto/
│   │   └── AelaDto.java              ← todos os DTOs (Request/Response)
│   ├── exception/
│   │   ├── AelaException.java        ← exceções customizadas
│   │   └── GlobalExceptionHandler.java
│   ├── model/
│   │   ├── Operador.java
│   │   ├── Baseline.java
│   │   ├── LeituraFisiologica.java
│   │   ├── Missao.java
│   │   ├── AlertaFisiologico.java     ← entidade persistida pelo SOAP
│   │   ├── TipoAmbiente.java
│   │   └── TipoTarefa.java
│   ├── repository/
│   │   └── AlertaFisiologicoRepository.java
│   ├── service/
│   │   ├── OperadorService.java
│   │   ├── BaselineService.java
│   │   ├── LeituraService.java
│   │   ├── ReadinessService.java      ← reutilizado por REST e SOAP
│   │   └── impl/
│   └── soap/
│       ├── AelaEndpoint.java          ← endpoint SOAP (@Endpoint)
│       └── SoapMessages.java          ← classes JAXB de request/response
├── src/main/resources/
│   ├── aela.xsd                       ← contrato SOAP (gera o WSDL)
│   └── application.properties
├── src/test/
│   └── ReadinessServiceTest.java
└── pom.xml
```

---

## Pré-requisitos

- Java 17+ instalado (`java -version`)
- Maven 3.8+ instalado (`mvn -version`)
- Oracle Database rodando (XE local ou instância da FIAP)

---

## Configuração do Banco Oracle

Edite `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:oracle:thin:@oracle.fiap.com.br:1521:orcl
spring.datasource.username=rm
spring.datasource.password=ddmmaa
```

---

## Como Executar

```bash
# 1. Clone o repositório
git clone https://github.com/camipzcunha/AELA_GS_JAVA.git
cd aela-java-completo

# 2. Configure o banco em application.properties

# 3. Compile e execute
mvn spring-boot:run
```

A API REST estará disponível em `http://localhost:8080`  
O WSDL do serviço SOAP estará disponível em `http://localhost:8080/ws/aela.wsdl`

---

## Endpoints — API REST

### Operadores
| Método | Endpoint | HTTP | Descrição |
|---|---|---|---|
| `POST` | `/api/operadores` | 201 | Cadastra operador |
| `GET` | `/api/operadores` | 200 | Lista operadores ativos |
| `GET` | `/api/operadores/{id}` | 200 | Detalha operador |
| `PUT` | `/api/operadores/{id}` | 200 | Atualiza operador |
| `DELETE` | `/api/operadores/{id}` | 204 | Desativa operador (soft delete) |
| `POST` | `/api/operadores/{id}/baseline` | 201 | Registra baseline fisiológico |
| `GET` | `/api/operadores/{id}/baseline` | 200 | Consulta baseline |
| `POST` | `/api/operadores/{id}/leituras` | 201 | Registra leitura fisiológica |
| `GET` | `/api/operadores/{id}/leituras` | 200 | Histórico de leituras |

### ReadinessScore
| Método | Endpoint | HTTP | Descrição |
|---|---|---|---|
| `GET` | `/api/readiness/operadores/{id}?tipoTarefa=EVA` | 200 | Score individual |
| `GET` | `/api/readiness/missoes/{id}/ranking?tipoTarefa=EVA` | 200 | Ranking da tripulação |

**Valores válidos para `tipoTarefa`:** `EVA` · `OPERACAO_COGNITIVA` · `TAREFA_FISICA` · `PILOTAGEM` · `MONITORAMENTO` · `RESGATE`

### Missões
| Método | Endpoint | HTTP | Descrição |
|---|---|---|---|
| `POST` | `/api/missoes` | 201 | Cria missão |
| `GET` | `/api/missoes` | 200 | Lista missões |
| `GET` | `/api/missoes/{id}` | 200 | Detalha missão |
| `POST` | `/api/missoes/{id}/tripulacao/{opId}` | 200 | Adiciona tripulante |
| `PATCH` | `/api/missoes/{id}/iniciar` | 200 | Inicia missão |
| `PATCH` | `/api/missoes/{id}/encerrar` | 200 | Encerra missão |
| `GET` | `/api/missoes/{id}/contexto` | 200 | Missão + contexto NASA (integração externa) |

### Monitoramento
| Endpoint | Descrição |
|---|---|
| `GET /actuator/health` | Status da aplicação e banco |

---

## Web Service SOAP

O AELA expõe um Web Service SOAP implementado com **Spring Web Services**.  
O contrato é definido pelo arquivo `aela.xsd`, a partir do qual o WSDL é gerado automaticamente.

**WSDL:** `http://localhost:8080/ws/aela.wsdl`  
**Endpoint:** `http://localhost:8080/ws/`  
**Namespace:** `http://aela.com.br/soap`

### Operações disponíveis

| Operação | Tipo | Descrição |
|---|---|---|
| `consultarReadiness` | Consulta | Calcula o ReadinessScore de um operador via SOAP. Reutiliza o mesmo `ReadinessService` da API REST. |
| `registrarAlerta` | Cadastro | Registra um alerta fisiológico crítico no Oracle, gerado por wearable, satélite ou sistema externo. |

### Como testar no SoapUI

1. Abra o SoapUI → **New SOAP Project**
2. No campo WSDL, insira: `http://localhost:8080/ws/aela.wsdl`
3. Clique em OK — as duas operações são importadas automaticamente
4. Execute `consultarReadinessRequest` com o envelope abaixo

### Envelope — consultarReadiness

**Request:**
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:tns="http://aela.com.br/soap">
   <soapenv:Header/>
   <soapenv:Body>
      <tns:consultarReadinessRequest>
         <tns:operadorId>1</tns:operadorId>
         <tns:tipoTarefa>EVA</tns:tipoTarefa>
      </tns:consultarReadinessRequest>
   </soapenv:Body>
</soapenv:Envelope>
```

**Response:**
```xml
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
   <SOAP-ENV:Body>
      <ns2:consultarReadinessResponse xmlns:ns2="http://aela.com.br/soap">
         <ns2:operadorId>1</ns2:operadorId>
         <ns2:operadorNome>Cmt. Isabelle</ns2:operadorNome>
         <ns2:scoreGeral>88.41</ns2:scoreGeral>
         <ns2:classificacao>APTO</ns2:classificacao>
         <ns2:recomendacao>Cmt. Isabelle está apta para EVA.</ns2:recomendacao>
         <ns2:codigoRetorno>200</ns2:codigoRetorno>
      </ns2:consultarReadinessResponse>
   </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

### Envelope — registrarAlerta

**Request:**
```xml
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:tns="http://aela.com.br/soap">
   <soapenv:Header/>
   <soapenv:Body>
      <tns:registrarAlertaRequest>
         <tns:operadorId>1</tns:operadorId>
         <tns:missaoId>1</tns:missaoId>
         <tns:tipoAlerta>CARDIOVASCULAR</tns:tipoAlerta>
         <tns:severidade>ALTA</tns:severidade>
         <tns:descricao>Frequência cardíaca acima de 150 bpm por mais de 5 minutos</tns:descricao>
         <tns:valorMetrica>158.0</tns:valorMetrica>
         <tns:valorBaseline>65.0</tns:valorBaseline>
         <tns:origem>WEARABLE</tns:origem>
      </tns:registrarAlertaRequest>
   </soapenv:Body>
</soapenv:Envelope>
```

**Response:**
```xml
<SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/">
   <SOAP-ENV:Body>
      <ns2:registrarAlertaResponse xmlns:ns2="http://aela.com.br/soap">
         <ns2:alertaId>7</ns2:alertaId>
         <ns2:operadorNome>Cmt. Isabelle</ns2:operadorNome>
         <ns2:tipoAlerta>CARDIOVASCULAR</ns2:tipoAlerta>
         <ns2:severidade>ALTA</ns2:severidade>
         <ns2:sucesso>true</ns2:sucesso>
         <ns2:codigoRetorno>201</ns2:codigoRetorno>
         <ns2:mensagem>Alerta registrado com sucesso. ID: 7</ns2:mensagem>
      </ns2:registrarAlertaResponse>
   </SOAP-ENV:Body>
</SOAP-ENV:Envelope>
```

---

## Integração com Serviço Externo — NASA APOD API

O endpoint `GET /api/missoes/{id}/contexto` consome a **NASA Astronomy Picture of the Day API** em tempo real, retornando os dados da missão enriquecidos com o contexto astronômico do dia.

**URL consultada:** `https://api.nasa.gov/planetary/apod?api_key=DEMO_KEY`

**Exemplo de resposta:**
```json
{
  "missao": {
    "id": 1,
    "nome": "Artemis IV — Órbita Lunar",
    "status": "EM_ANDAMENTO"
  },
  "contextoAstronomico": {
    "fonte": "NASA Astronomy Picture of the Day",
    "titulo": "Comet R3 PanSTARRS Through Time",
    "descricao": "What happens to a comet as it leaves our inner Solar System?...",
    "data": "2026-06-08",
    "tipo": "image",
    "status": "DISPONIVEL"
  }
}
```

> A chave `DEMO_KEY` permite 30 req/hora. Para produção, cadastre uma chave gratuita em [api.nasa.gov](https://api.nasa.gov) e configure `nasa.api.key` no `application.properties`.

---

## Fluxo de Uso

```
1. POST /api/operadores                        → cadastra o operador
2. POST /api/operadores/1/baseline             → registra o baseline (o "zero" dele)
3. POST /api/missoes                           → cria a missão
4. POST /api/missoes/1/tripulacao/1            → adiciona operador à missão
5. PATCH /api/missoes/1/iniciar                → inicia a missão
6. POST /api/operadores/1/leituras             → registra leitura (durante a missão)
7. GET  /api/readiness/operadores/1?tipoTarefa=EVA → ReadinessScore
8. GET  /api/readiness/missoes/1/ranking?tipoTarefa=EVA → ranking da tripulação
9. GET  /api/missoes/1/contexto                → dados da missão + contexto NASA
10. PATCH /api/missoes/1/encerrar              → encerra missão
```

---

## Executar Testes

```bash
mvn test
```

Os testes unitários do `ReadinessServiceTest` validam:
- Score >= 80 para operador com pequenos desvios (APTO)
- Score < 50 para operador com desvios severos (INAPTO)
- Exceções corretas para operadores sem baseline ou leituras
- Diferença de score entre EVA e OPERACAO_COGNITIVA conforme os pesos por tarefa

---

## Exemplos de Requisição (cURL)

```bash
# Cadastrar operador
curl -X POST http://localhost:8080/api/operadores \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Cmt. Isabelle",
    "matricula": "AELA-001",
    "email": "isabelle@aela.io",
    "tipoAmbiente": "ESPACO",
    "especialidade": "Operações Extraveiculares"
  }'

# Registrar baseline
curl -X POST http://localhost:8080/api/operadores/1/baseline \
  -H "Content-Type: application/json" \
  -d '{
    "freqCardiacaBasal": 65.0,
    "pressaoSistolica": 120.0,
    "pressaoDiastolica": 80.0,
    "tempoReacaoMs": 200.0,
    "scoreEquilibrio": 90.0,
    "pressaoOcular": 15.0,
    "acuidadeVisual": 1.0,
    "scoreCognitivo": 88.0,
    "horasSono": 8.0,
    "saturacaoO2": 98.0
  }'

# Registrar leitura
curl -X POST http://localhost:8080/api/operadores/1/leituras \
  -H "Content-Type: application/json" \
  -d '{
    "freqCardiaca": 72.0,
    "pressaoSistolica": 125.0,
    "pressaoDiastolica": 83.0,
    "tempoReacaoMs": 220.0,
    "scoreEquilibrio": 85.0,
    "pressaoOcular": 16.5,
    "acuidadeVisual": 0.95,
    "scoreCognitivo": 82.0,
    "horasSono": 6.5,
    "saturacaoO2": 97.0,
    "fonte": "WEARABLE"
  }'

# Calcular ReadinessScore
curl "http://localhost:8080/api/readiness/operadores/1?tipoTarefa=EVA"

# Contexto da missão com dados NASA
curl "http://localhost:8080/api/missoes/1/contexto"
```
