# metricas/

Scripts e saídas da medição do TCC: comparação entre a Versão A (acoplada) e a Versão B (desacoplada, padrão Adapter) em 3 cenários de mudança. Todos os scripts só leem o repositório: estados antigos são executados em `git worktree` temporário, removido ao final; nenhuma tag ou branch é alterada.

## Estados medidos (tags)

| Versão | Baseline | Cenário 1 (troca de provedor) | Cenário 2 (novo provedor) | Cenário 3 (formato de comunicação) |
|---|---|---|---|---|
| A (acoplada) | `baseline-acoplada` (295d660) | `cenario-1-acoplada` (bc0cf1c) | `cenario-2-acoplada` (cf372e9) | `cenario-3-acoplada` (a648c39) |
| B (desacoplada) | `baseline-desacoplada` (497ed71) | `cenario-1-desacoplada` (86dc06d) | `cenario-2-desacoplada` (d784cc8) | `cenario-3-desacoplada` (ea76236) |

Os 6 pares de medição são `baseline-<versão> -> cenario-N-<versão>`. Critério de contagem: `git diff --numstat --no-renames <base> <tag> -- prototype/`. Grupos: T = qualquer arquivo em `src/test` (regra aplicada primeiro); P = nome `Gemini*`/`Groq*` ou pacote `provider/gemini|groq`; O = demais, separado em código e configuração (`src/main/resources`).

## Arquivos

| Arquivo | Conteúdo |
|---|---|
| `medir.sh` | Medição principal: arquivos e linhas por par e por grupo (P/T/Oc/Of), nos modos `--no-renames` e padrão (com detecção de renomeação). Recebe os pares como argumentos; sem argumentos, usa os 6 pares. |
| `medicao-final.txt` | Saída de `medir.sh` com os 6 pares. |
| `validacao-equivalencia.sh` | Sobe cada estado na porta 8081 e envia os casos C1–C10 para `POST /api/resumo`; nos estados do Cenário 3, roda `./mvnw -q test`. |
| `validacao-equivalencia.md` | Matriz estado × caso (OK/DIVERGENTE), resultado dos testes do Cenário 3 e análise das divergências. |
| `validacao-equivalencia-bruto.txt` | Todas as tentativas: estado, caso, tentativa, status HTTP, duração, veredito e trecho do corpo. |
| `classificar-resumoservice.sh` + `ClassificaResumoService.java` | Classificação de cada linha alterada em `ResumoService.java` em R (regra de negócio), I (integração), S (seleção de provedor), N (neutra) ou ? (dúvida). O critério está no cabeçalho do `.java`. |
| `classificacao-resumoservice.md` | Tabela par × categoria, % de R, lista completa das linhas R e ? e o script usado. |
| `classificacao-resumoservice.tsv` | Todas as linhas classificadas (par, sinal, número da linha, categoria, motivo, texto). |
| `sloc.sh` + `SlocDiff.java` | Linhas efetivas (sem linhas em branco e sem linhas só de comentário) adicionadas/removidas por par e grupo. |
| `sloc.md` | Tabelas de SLOC por par e grupo, ao lado do total bruto do critério original. |
| `sloc-por-arquivo.tsv` | SLOC bruto e efetivo por arquivo e par. |
| `cbo.sh` | CBO de cada classe de `src/main` nos 8 estados, calculado pela ferramenta CK 0.7.0. |
| `cbo.md` | Por estado: número de tipos, CBO do `ResumoService`, CBO médio e máximo. |
| `cbo-por-classe.csv` | CBO de cada classe em cada estado. |

## Pré-requisitos

- Git e Bash (no Windows, o Git Bash).
- Clone com as tags (`git clone` já traz as tags; num clone existente, rode `git fetch --tags`).
- `medir.sh`: só Git.
- `classificar-resumoservice.sh`, `sloc.sh`: Java 17 (os `.java` são executados direto com `java Arquivo.java`, sem compilar antes).
- `cbo.sh`: Java 17 e acesso ao Maven Central na primeira execução (baixa o CK pelo Maven Wrapper do projeto). Para usar jars já baixados: `CK_LIB=<pasta> ./metricas/cbo.sh`.
- `validacao-equivalencia.sh`: Java 17, `curl`, porta 8081 livre e as variáveis `GEMINI_API_KEY` e `GROQ_API_KEY` no ambiente (as chaves não são impressas). Faz chamadas reais às APIs.

## Como executar

Na raiz do repositório (os scripts também funcionam a partir de outra pasta do repositório):

```bash
./metricas/medir.sh > metricas/medicao-final.txt        # 6 pares padrão
./metricas/medir.sh "baseline-acoplada cenario-1-acoplada"   # um par específico
./metricas/classificar-resumoservice.sh
./metricas/sloc.sh
./metricas/cbo.sh
./metricas/validacao-equivalencia.sh                       # todos os estados
./metricas/validacao-equivalencia.sh cenario-2-acoplada   # um estado
```

Totais esperados no modo `--no-renames` (arquivos/linhas): A = 11/483, 9/388, 4/235; B = 11/635, 11/380, 4/234 (cenários 1, 2 e 3).
