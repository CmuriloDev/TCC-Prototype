# Validação — rodada 2 (casos dependentes do Gemini)

Reexecução dos 4 casos que ficaram DIVERGENTE na rodada 1 (26/10/2026, `validacao-equivalencia.md`), todos chamadas reais ao Gemini que voltaram 502 em 4 tentativas:
(a) C1 em baseline-acoplada; (b) C1 em cenario-2-acoplada; (c) C8 em cenario-2-acoplada; (d) C8 em cenario-2-desacoplada.

- Script: `metricas/validacao-rodada-2.sh`; saída bruta: `metricas/validacao-rodada-2-bruto.txt`.
- Modelo: `gemini-3.6-flash` (propriedade `gemini.api.model`, igual nos 4 estados), endpoint `https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent`; chave lida de `GEMINI_API_KEY` e enviada só no header `x-goog-api-key` (nunca registrada).
- Porta: a Versão A define `server.port=3000` no `application.properties`; a Versão B não define (padrão 8080). As duas foram iniciadas com `--server.port=8081`.
- Cada estado em `git worktree add --detach` da tag (removido ao final), `./mvnw -q -DskipTests package`, `java -jar ... --server.port=8081 --logging.level.org.springframework.web=DEBUG`. O log DEBUG é só uma flag de execução: o `GlobalExceptionHandler` não registra nada, e sem ele o 502 não deixaria rastro do status devolvido pelo Gemini.
- Corpos idênticos aos da rodada 1: C1 = `{"texto":"<texto válido de 485 caracteres>"}`; C8 = `{"texto":"<mesmo texto>","provedor":"gemini"}`.
- Até 5 tentativas por caso, 20 s de intervalo, uma requisição por vez, parando no primeiro 200 com resumo não vazio. 20 s também entre casos do mesmo estado.
- Controles: logo após cada caso-alvo, o mesmo pedido no outro lado do par A x B (C1 em baseline-desacoplada; C1 em cenario-2-desacoplada), para separar instabilidade do provedor de comportamento da versão. Execução sequencial (porta única), não paralela.
- Dois horários: **R1** em 27/10/2026, 23:43–23:46, e **R2** em 28/10/2026, 08:22–08:26 (cerca de 8 h 40 min depois).
- "Tamanho do resumo" = número de caracteres do campo `resumo` no JSON da resposta (escapes como `\n` contam 2).

## Parte 1 — diagnóstico direto (fora da aplicação)

| Horário | Chamada | Status | Duração (s) | Observação |
|---|---|---|---|---|
| 27/10 23:41:11 | `GET .../models/gemini-3.6-flash` | 200 | 0,48 | modelo existe; `supportedGenerationMethods` inclui `generateContent`; `"thinking": true` |
| 27/10 23:41:11 | `GET .../models?pageSize=1000` | 200 | 0,49 | `models/gemini-3.6-flash` consta na lista da chave |
| 27/10 23:41:25 | `POST ...:generateContent` | 200 | 13,03 | 705 tokens de raciocínio, 94 de resposta |
| 27/10 23:42:10 | `POST ...:generateContent` | 200 | 6,35 | 760 de raciocínio, 197 de resposta |
| 27/10 23:42:36 | `POST ...:generateContent` | 200 | 6,35 | 886 de raciocínio, 109 de resposta |
| 28/10 08:27:18 | `POST ...:generateContent` (sonda pós-R2) | 200 | 5,89 | — |
| 28/10 08:27:44 | idem | 200 | 13,99 | — |
| 28/10 08:28:18 | idem | 200 | 5,89 | — |
| 28/10 08:28:44 | idem | 200 | 5,05 | — |
| 28/10 08:29:10 | idem | 200 | 10,43 | — |

- O nome do modelo na propriedade corresponde a um modelo disponível para a chave.
- Nenhum 429 nem erro de quota em nenhuma chamada direta. Nenhum 503 nas chamadas diretas. A sonda pós-R2 (até 5 chamadas, parando no 1º erro) foi feita para capturar o texto do 503 visto na R2, mas todas deram 200, então **o texto da mensagem do provedor para o 503 não foi registrado**.
- Mesmo com sucesso, a latência direta chegou a 13,03 s e 13,99 s, a menos de 2 s do `READ_TIMEOUT = 15 s` do `GeminiConfig` (igual em A e B). O modelo gasta 700–900 tokens de raciocínio por resposta. Isso é compatível com as falhas de ~15 s da rodada 1 serem timeout de leitura em respostas lentas do provedor.

## Tabela estado × caso × tentativa

| Horário | Estado | Caso | Papel | Tent. | Status | Duração (s) | Tam. resumo | Trecho do corpo | Causa |
|---|---|---|---|---|---|---|---|---|---|
| R1 27/10 23:44:08 | baseline-acoplada | C1 | alvo (a) | 1 | 200 | 7,34 | 803 | `{"resumo":"Aqui está um resumo claro e objetivo do texto:\n\nA integração direta e acoplada de provedores…` | — |
| R1 27/10 23:44:27 | baseline-desacoplada | C1 | controle (a) | 1 | 200 | 7,80 | 782 | `{"resumo":"Aqui está um resumo claro e objetivo do texto:\n\nO acoplamento direto a serviços externos…` | — |
| R1 27/10 23:44:49 | cenario-2-acoplada | C1 | alvo (b) | 1 | 200 | 6,56 | 411 | `{"resumo":"**Resumo:**\n\nO acoplamento direto a provedores externos de inteligência artificial…` | — |
| R1 27/10 23:45:17 | cenario-2-acoplada | C8 | alvo (c) | 1 | 200 | 4,68 | 421 | `{"resumo":"**Resumo:**\n\nO acoplamento direto com provedores externos de inteligência artificial…` | — |
| R1 27/10 23:45:34 | cenario-2-desacoplada | C8 | alvo (d) | 1 | 200 | 5,88 | 359 | `{"resumo":"**Resumo:**\n\nO acoplamento direto de serviços de IA às regras de negócio…` | — |
| R1 27/10 23:46:02 | cenario-2-desacoplada | C1 | controle (b) | 1 | 200 | 5,68 | 375 | `{"resumo":"**Resumo:**\n\nO acoplamento direto de serviços de Inteligência Artificial…` | — |
| R2 28/10 08:22:17 | baseline-acoplada | C1 | alvo (a) | 1 | 200 | 7,32 | 481 | `{"resumo":"**Resumo:**\n\nO texto destaca que o acoplamento direto com provedores externos…` | — |
| R2 28/10 08:22:36 | baseline-desacoplada | C1 | controle (a) | 1 | 200 | 7,59 | 440 | `{"resumo":"**Resumo:**\n\nO acoplamento direto entre um sistema e provedores externos…` | — |
| R2 28/10 08:22:59 | cenario-2-acoplada | C1 | alvo (b) | 1 | 200 | 12,03 | 406 | `{"resumo":"Aqui está um resumo claro e objetivo do texto:\n\nA integração acoplada com serviços externos de IA…` | — |
| R2 28/10 08:23:33 | cenario-2-acoplada | C8 | alvo (c) | 1 | 200 | 8,81 | 415 | `{"resumo":"**Resumo:**\n\nO acoplamento direto com provedores de inteligência artificial…` | — |
| R2 28/10 08:23:57 | cenario-2-desacoplada | C8 | alvo (d) | 1 | 502 | 6,70 | 0 | `{"mensagem":"O serviço de IA está indisponível no momento. Tente novamente em instantes."}` | Gemini respondeu `503 SERVICE_UNAVAILABLE` (log da aplicação) |
| R2 28/10 08:24:26 | cenario-2-desacoplada | C8 | alvo (d) | 2 | 502 | 11,31 | 0 | idem | Gemini respondeu `503 SERVICE_UNAVAILABLE` |
| R2 28/10 08:25:00 | cenario-2-desacoplada | C8 | alvo (d) | 3 | 502 | 7,53 | 0 | idem | Gemini respondeu `503 SERVICE_UNAVAILABLE` |
| R2 28/10 08:25:29 | cenario-2-desacoplada | C8 | alvo (d) | 4 | 200 | 6,20 | 429 | `{"resumo":"**Resumo:**\n\nO acoplamento direto com provedores de inteligência artificial prejudica…` | — |
| R2 28/10 08:25:58 | cenario-2-desacoplada | C1 | controle (b) | 1 | 200 | 6,16 | 783 | `{"resumo":"Aqui está um resumo claro e objetivo do texto:\n\n**Resumo:**\nIntegrar serviços externos…` | — |

Log da aplicação nas três tentativas 502 (resumido; as linhas completas estão no bruto). As três têm a mesma sequência, mudando só os horários:

```
o.s.web.client.RestTemplate : HTTP POST https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent
o.s.web.client.RestTemplate : Response 503 SERVICE_UNAVAILABLE
.m.m.a.ExceptionHandlerExceptionResolver : Using @ExceptionHandler ...GlobalExceptionHandler#tratarProvedorIndisponivel(ProvedorIndisponivelException)
o.s.web.servlet.DispatcherServlet : Completed 502 BAD_GATEWAY
```

Não há stack trace: o `GlobalExceptionHandler` trata a exceção sem registrá-la. O DEBUG do `RestTemplate` registra o status da resposta do Gemini, mas não o corpo, então o texto do erro 503 do provedor não aparece no log.

## Conclusão por caso

| Caso | Rodada 1 (26/10) | R1 (27/10 23:44) | R2 (28/10 08:22) | Conclusão |
|---|---|---|---|---|
| (a) baseline-acoplada C1 | 502 × 4 | 200 na 1ª | 200 na 1ª | **OK** |
| (b) cenario-2-acoplada C1 | 502 × 4 | 200 na 1ª | 200 na 1ª | **OK** |
| (c) cenario-2-acoplada C8 | 502 × 4 | 200 na 1ª | 200 na 1ª | **OK** |
| (d) cenario-2-desacoplada C8 | 502 × 4 | 200 na 1ª | 200 na 4ª, após 3 × 503 do Gemini | **OK** (com FALHA DO PROVEDOR transitória: `503 SERVICE_UNAVAILABLE`) |

Os 503 de (d) na R2 são falha do provedor, não da versão:

1. O log mostra que a aplicação recebeu `503 SERVICE_UNAVAILABLE` do Gemini e apenas o traduziu para 502 com a mensagem padrão.
2. O mesmo pedido (C8, `"provedor":"gemini"`) na Versão A, 24 s antes da 1ª falha de (d), deu 200.
3. Na mesma instância de (d), a 4ª tentativa deu 200, e o controle C1 (também Gemini) deu 200 logo depois.
4. A Versão A mapeia o 503 do Gemini para a mesma resposta (`ResumoService.mapearErroHttp`: só 429 tem mensagem própria; demais status → "O serviço de IA está indisponível no momento. Tente novamente em instantes."). O código de mapeamento é o mesmo em A e B.

Sobre as falhas rápidas da rodada 1 (0,89 s, 2,90 s e 6,00 s): continuam sem mensagem registrada e não podem ser atribuídas com certeza. A rodada 2 mostrou que o Gemini devolve 503 em 6,7–11,3 s, e que a aplicação apresenta esse caso com a mensagem "indisponível" (um 429 teria a mensagem "Limite de requisições ao provedor de IA excedido."). Isso é compatível com aquelas falhas serem 503 do provedor, mas não as comprova.

## Parte 3 — comparação A × B

| Par | Horário | Versão A (status · tam. resumo) | Versão B (status · tam. resumo) | Equivalente? |
|---|---|---|---|---|
| C1 baseline-acoplada × baseline-desacoplada | R1 | 200 · 803 | 200 · 782 | Sim |
| | R2 | 200 · 481 | 200 · 440 | Sim |
| C8 cenario-2-acoplada × cenario-2-desacoplada | R1 | 200 · 421 | 200 · 359 | Sim |
| | R2 | 200 · 415 (1ª tent.) | 200 · 429 (4ª tent.; 1ª–3ª: 502 por 503 do Gemini) | Sim no resultado final; as tentativas 502 de B são do provedor |

- Status e forma da resposta (`{"resumo": "..."}` não vazio no sucesso; `{"mensagem": "O serviço de IA está indisponível no momento. Tente novamente em instantes."}` no 502) são iguais nas duas versões.
- O tamanho do resumo varia entre 359 e 803 caracteres. Varia na mesma proporção dentro da mesma versão e entre horários (baseline-acoplada: 803 em R1, 481 em R2), porque o modelo gera texto diferente a cada chamada. O tamanho não é um critério de diferença entre as versões.

**Divergência entre A e B:** nenhuma divergência atribuível à arquitetura.

## Incidentes de registro (honestidade dos dados)

- Na R2, o trecho de 300 do script cortava por **bytes** (`head -c 300`). Em 2 linhas (cenario-2-acoplada C1 e C8) o corte caiu no meio de um caractere acentuado e deixou 1 byte órfão em cada uma. Os 2 bytes foram removidos do bruto com `iconv -c` (o arquivo é UTF-8 válido). O corpo completo dessas tentativas está íntegro na linha `corpo completo` seguinte.
- No log das tentativas 502, o `grep` que recorta o log tratou o arquivo como binário e trocou o restante do log daquela janela pela linha `Binary file (standard input) matches`. As linhas antes disso, que incluem o `Response 503` e o `Completed 502`, foram gravadas.
- As duas falhas foram corrigidas no script depois da R2 (corte por caractere com `awk substr` e `grep -a`). Os dados de R1 e R2 foram gerados com a versão anterior do script.
- A R2 estava agendada para 09:00 de 28/10. A sessão que aguardava foi encerrada antes, e a R2 foi iniciada manualmente às 08:22, a pedido do autor.
