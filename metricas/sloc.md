# Linhas de código efetivas (SLOC) por cenário e versão

- Data: 2026-10-26T09:59:38-03:00
- git version 2.55.0.windows.5; Maven (wrapper do projeto): 3.9.16; Java: openjdk version "17.0.20.1" 2026-08-18
- Arquivos e linhas: `git diff --numstat --no-renames <base> <alvo> -- prototype/` e `git diff -U0 --no-renames` por arquivo
- Efetiva = não em branco e não só comentário. .java: tokenização do arquivo completo na revisão (antiga para removidas, nova para adicionadas), reconhecendo //, /* */ e Javadoc, ignorando marcadores dentro de literais. .properties: comentário começa por #.
- Grupos: T = src/test (aplicado primeiro); P = Gemini*/Groq* ou pacote provider/gemini|groq; O-código; O-config (src/main/resources).
- Script: `metricas/sloc.sh` + `metricas/SlocDiff.java`; por arquivo: `metricas/sloc-por-arquivo.tsv`

Colunas: arquivos, efetivas adicionadas, efetivas removidas, total efetivo, total bruto (critério original), bruto adicionadas / removidas / total.

## Todos os grupos

| Par | Arquivos | Efetivas + | Efetivas − | Total efetivo | Total bruto | Bruto + / − / total |
|---|---|---|---|---|---|---|
| baseline-acoplada -> cenario-1-acoplada | 11 | 146 | 155 | 301 | 483 | 235 / 248 / 483 |
| baseline-acoplada -> cenario-2-acoplada | 9 | 231 | 16 | 247 | 388 | 364 / 24 / 388 |
| baseline-acoplada -> cenario-3-acoplada | 4 | 176 | 9 | 185 | 235 | 226 / 9 / 235 |
| baseline-desacoplada -> cenario-1-desacoplada | 11 | 194 | 203 | 397 | 635 | 311 / 324 / 635 |
| baseline-desacoplada -> cenario-2-desacoplada | 11 | 238 | 5 | 243 | 380 | 375 / 5 / 380 |
| baseline-desacoplada -> cenario-3-desacoplada | 4 | 174 | 9 | 183 | 234 | 225 / 9 / 234 |

## P (provedor)

| Par | Arquivos | Efetivas + | Efetivas − | Total efetivo | Total bruto | Bruto + / − / total |
|---|---|---|---|---|---|---|
| baseline-acoplada -> cenario-1-acoplada | 8 | 125 | 130 | 255 | 426 | 209 / 217 / 426 |
| baseline-acoplada -> cenario-2-acoplada | 4 | 125 | 0 | 125 | 207 | 207 / 0 / 207 |
| baseline-acoplada -> cenario-3-acoplada | 2 | 44 | 5 | 49 | 63 | 58 / 5 / 63 |
| baseline-desacoplada -> cenario-1-desacoplada | 10 | 191 | 200 | 391 | 625 | 306 / 319 / 625 |
| baseline-desacoplada -> cenario-2-desacoplada | 6 | 192 | 1 | 193 | 308 | 307 / 1 / 308 |
| baseline-desacoplada -> cenario-3-desacoplada | 3 | 61 | 9 | 70 | 89 | 80 / 9 / 89 |

## O-código

| Par | Arquivos | Efetivas + | Efetivas − | Total efetivo | Total bruto | Bruto + / − / total |
|---|---|---|---|---|---|---|
| baseline-acoplada -> cenario-1-acoplada | 2 | 18 | 22 | 40 | 47 | 21 / 26 / 47 |
| baseline-acoplada -> cenario-2-acoplada | 4 | 103 | 16 | 119 | 173 | 149 / 24 / 173 |
| baseline-acoplada -> cenario-3-acoplada | 1 | 17 | 4 | 21 | 25 | 21 / 4 / 25 |
| baseline-desacoplada -> cenario-2-desacoplada | 4 | 42 | 4 | 46 | 61 | 57 / 4 / 61 |

## O-config

| Par | Arquivos | Efetivas + | Efetivas − | Total efetivo | Total bruto | Bruto + / − / total |
|---|---|---|---|---|---|---|
| baseline-acoplada -> cenario-1-acoplada | 1 | 3 | 3 | 6 | 10 | 5 / 5 / 10 |
| baseline-acoplada -> cenario-2-acoplada | 1 | 3 | 0 | 3 | 8 | 8 / 0 / 8 |
| baseline-desacoplada -> cenario-1-desacoplada | 1 | 3 | 3 | 6 | 10 | 5 / 5 / 10 |
| baseline-desacoplada -> cenario-2-desacoplada | 1 | 4 | 0 | 4 | 11 | 11 / 0 / 11 |

## T (teste)

| Par | Arquivos | Efetivas + | Efetivas − | Total efetivo | Total bruto | Bruto + / − / total |
|---|---|---|---|---|---|---|
| baseline-acoplada -> cenario-3-acoplada | 1 | 115 | 0 | 115 | 147 | 147 / 0 / 147 |
| baseline-desacoplada -> cenario-3-desacoplada | 1 | 113 | 0 | 113 | 145 | 145 / 0 / 145 |

