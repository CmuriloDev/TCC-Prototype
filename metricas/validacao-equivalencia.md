# Validação sistemática da equivalência funcional (Versões A e B)

- Data da execução: 2026-10-26 (início 09:49:30 -03:00)
- git version 2.55.0.windows.5; Maven 3.9.16 (wrapper do projeto, igual em todos os estados); Java: openjdk 17.0.20.1 (Eclipse Adoptium)
- Script: `metricas/validacao-equivalencia.sh`; saída bruta: `metricas/validacao-equivalencia-bruto.txt`
- Cada estado foi executado em `git worktree` temporário (removido ao final), empacotado com `./mvnw -q -DskipTests package` e iniciado com `java -jar ... --server.port=8081`. A Versão A define `server.port=3000` e a Versão B não define porta (padrão 8080); a porta 8081 foi forçada nos dois lados.
- Requisição: `POST /api/resumo`, `Content-Type: application/json`. Texto válido: 485 caracteres (texto acadêmico sobre acoplamento e Adapter); curto: 32; longo: 5100.
- C5: a aplicação é reiniciada com a variável `GEMINI_API_KEY` ou `GROQ_API_KEY` sobrescrita só nesse processo (`chave-invalida-teste`). No Cenário 2, C5-gemini envia `"provedor":"gemini"` e C5-groq envia `"provedor":"groq"`; nos demais estados o campo não é enviado.
- Falha transitória (casos que esperam 200): até 3 repetições além da 1ª tentativa (máx. 4), com 5 s de intervalo. Todas as tentativas estão no bruto.
- Veredito: OK se o status e a mensagem (ou resumo não vazio) batem com o esperado na última tentativa; DIVERGENTE caso contrário.

## Matriz estado × caso

| Estado | C1 | C2 | C3 | C4 | C5-gemini | C5-groq | C6 | C7 | C8 | C9 | C10 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| baseline-acoplada | OK² (rodada 1: DIVERGENTE, 4 tent.) | OK | OK | OK | OK | — | — | — | — | — | — |
| cenario-1-acoplada | OK | OK | OK | OK | — | OK | — | — | — | — | — |
| cenario-2-acoplada | OK² (rodada 1: DIVERGENTE, 4 tent.) | OK | OK | OK | OK | OK | OK | OK | OK² (rodada 1: DIVERGENTE, 4 tent.) | OK | OK |
| baseline-desacoplada | OK (2 tent.) | OK | OK | OK | OK | — | — | — | — | — | — |
| cenario-1-desacoplada | OK | OK | OK | OK | — | OK | — | — | — | — | — |
| cenario-2-desacoplada | OK | OK | OK | OK | OK | OK | OK | OK | OK² (rodada 1: DIVERGENTE, 4 tent.) | OK | OK (3 tent.) |

"—" = caso não se aplica ao estado (provedor inativo ou campo `provedor` inexistente).

² Reexecutado na rodada 2 (`validacao-rodada-2.md`), em dois horários: 27/10/2026 23:44 e 28/10/2026 08:22. Nos dois, (a) baseline-acoplada C1, (b) cenario-2-acoplada C1 e (c) cenario-2-acoplada C8 deram 200 na 1ª tentativa. (d) cenario-2-desacoplada C8 deu 200 na 1ª tentativa no primeiro horário; no segundo, deu 200 na 4ª, após 3 respostas `503 SERVICE_UNAVAILABLE` do Gemini (falha do provedor, registrada no log da aplicação). O veredito da rodada 1 está mantido entre parênteses como histórico.

Esperados: C1/C6/C7/C8/C10 = 200 com resumo não vazio; C2 = 400 "O texto não pode estar vazio."; C3 = 400 "O texto deve ter no mínimo 50 caracteres."; C4 = 400 "O texto não pode ultrapassar 5000 caracteres."; C5 = 502 "O serviço de IA está indisponível no momento. Tente novamente em instantes."; C9 = 400 "Provedor inválido. Valores aceitos: gemini, groq.".

## Cenário 3 (formato simulado — API não chamada)

| Estado | `./mvnw -q test` | Testes | Falhas | Erros | Ignorados |
|---|---|---|---|---|---|
| cenario-3-acoplada | exit 0 | 6 (1 `contextLoads` + 5 de extração) | 0 | 0 | 0 |
| cenario-3-desacoplada | exit 0 | 6 (1 `contextLoads` + 5 de extração) | 0 | 0 | 0 |

Os 5 casos de extração (mesmos nomes nas duas versões; A: `ResumoServiceExtracaoGeminiTest`, B: `GeminiAdapterExtracaoTest`):

1. `deveRetornarTextoQuandoResponseTemUmaUnicaPart`
2. `deveConcatenarTextoQuandoResponseTemMultiplasParts`
3. `deveLancarErroQuandoPartsEstaVazia`
4. `deveLancarErroQuandoTextoConcatenadoEstaEmBranco`
5. `deveLancarErroQuandoResponseEstaAusente`

## Análise das divergências

Todas as células DIVERGENTE são chamadas reais ao **Gemini** que voltaram 502 nas 4 tentativas:

| Estado | Caso | Durações das tentativas (s) |
|---|---|---|
| baseline-acoplada | C1 | 15,10 · 15,00 · 2,90 · 0,89 |
| cenario-2-acoplada | C1 | 15,11 · 15,01 · 15,01 · 15,01 |
| cenario-2-acoplada | C8 | 15,01 · 15,00 · 15,01 · 6,00 |
| cenario-2-desacoplada | C8 | 15,01 · 15,00 · 15,01 · 15,00 |

- As tentativas de ~15,0 s coincidem com o `READ_TIMEOUT = Duration.ofSeconds(15)` definido no `GeminiConfig` das duas versões (timeout de leitura no Gemini → 502 da aplicação).
- As tentativas curtas (0,89 s, 2,90 s, 6,00 s) também voltaram 502. **A mensagem do corpo dessas falhas não foi registrada**: na versão do script usada nesta rodada, os casos que esperam 200 só gravavam o campo `resumo`. O script foi corrigido depois (passa a gravar `mensagem` quando o status não é 200), mas esta rodada não foi repetida, para não ultrapassar o limite de 3 repetições da regra combinada. Portanto, não se sabe se essas falhas curtas foram 503, 429 (limite) ou outro erro do Gemini.
- Nenhuma falha ocorreu em chamadas ao Groq, nem em validações (C2–C4, C9), nem nos casos de chave inválida (C5): todas deram OK nos dois lados.
- Na mesma rodada, o Gemini respondeu 200 em baseline-desacoplada C1 (2ª tentativa), cenario-2-acoplada C10 (1ª), cenario-2-desacoplada C1 (1ª) e cenario-2-desacoplada C10 (3ª). C10 sem provedor explícito usa o Gemini como padrão nos dois lados.
- Execução preliminar (teste do script, só baseline-acoplada, mesmo dia, antes da rodada completa; a saída foi sobrescrita pela rodada completa e está registrada apenas no log da sessão): `baseline-acoplada | C1 | 1 | 200 | 8.615999 | 200 | OK | resumo=Aqui está um resumo claro e objetivo do texto:\n\n**Resumo:**\nIntegrar provedor`. C2, C3, C4 e C5-gemini também deram OK nessa execução.

**Divergência entre A e B:** nenhuma divergência de comportamento atribuível à arquitetura. As diferenças de veredito entre A e B (C1 no baseline e no Cenário 2) aparecem só em chamadas ao Gemini que falharam por timeout ou erro do provedor; o mesmo caso C8 falhou nos dois lados, e em todos os casos que não dependem do Gemini os dois lados têm o mesmo status e a mesma mensagem. Para fechar a matriz com C1 (baseline-acoplada e cenario-2-acoplada) e C8 (cenario-2 A e B) em OK, falta uma nova rodada com o Gemini estável, que precisa da autorização do autor.

## Rodada 2 (reexecução dos 4 casos Gemini)

Feita com autorização do autor; detalhes em `validacao-rodada-2.md` e `validacao-rodada-2-bruto.txt`. Os 4 casos passaram a OK nos dois horários. A chamada direta ao Gemini (fora da aplicação) confirmou que o modelo `gemini-3.6-flash` está disponível para a chave, sem 429 nem erro de quota, com latência de até 13,99 s (perto do `READ_TIMEOUT` de 15 s). A única falha da rodada 2 foi `503 SERVICE_UNAVAILABLE` do Gemini em cenario-2-desacoplada C8 (3 tentativas), enquanto o mesmo pedido na Versão A, segundos antes, deu 200. Conclusão mantida: nenhuma divergência atribuível à arquitetura.
