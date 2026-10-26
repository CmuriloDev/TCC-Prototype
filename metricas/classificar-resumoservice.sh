#!/usr/bin/env bash
# Classificação por linha das alterações em ResumoService.java (R/I/S/N/?).
# Gera metricas/classificacao-resumoservice.md e metricas/classificacao-resumoservice.tsv.
# Uso: ./metricas/classificar-resumoservice.sh   (requer git e Java 17)
set -euo pipefail
export LC_ALL=C.UTF-8

RAIZ=$(git rev-parse --show-toplevel)
cd "$RAIZ"
ARQ=prototype/src/main/java/com/adapter/prototype/service/ResumoService.java
TSV=metricas/classificacao-resumoservice.tsv
MD=metricas/classificacao-resumoservice.md
PARES=(baseline-acoplada cenario-1-acoplada baseline-acoplada cenario-2-acoplada
       baseline-acoplada cenario-3-acoplada baseline-desacoplada cenario-1-desacoplada
       baseline-desacoplada cenario-2-desacoplada baseline-desacoplada cenario-3-desacoplada)

java -Dfile.encoding=UTF-8 metricas/ClassificaResumoService.java "$ARQ" "${PARES[@]}" > "$TSV"

{
    echo "# Classificação por linha — ResumoService.java"
    echo
    echo "- Data: $(date -Iseconds)"
    echo "- $(git --version); Maven (wrapper do projeto): $(sed -n 's/.*apache-maven-\([0-9.]*\)-bin.*/\1/p' prototype/.mvn/wrapper/maven-wrapper.properties); Java: $(java -version 2>&1 | head -1)"
    echo "- Arquivo: \`$ARQ\`"
    echo "- Diff: \`git diff -U0 --no-renames <base> <alvo> -- $ARQ\`"
    echo "- Script: \`metricas/classificar-resumoservice.sh\` + \`metricas/ClassificaResumoService.java\` (critério completo no cabeçalho do .java e na seção 3)"
    echo "- Linhas classificadas individualmente: \`$TSV\`"
    echo
    echo "Categorias: R = regra de negócio; I = integração com provedor; S = seleção de provedor; N = neutra; ? = dúvida (não contada como R nem I)."
    echo
    echo "## 1. Tabela cenário × categoria"
    echo
    echo "Formato de cada célula: adicionadas / removidas / total."
    echo
    echo "| Par | R | I | S | N | ? | Total alterado | % R sobre o total |"
    echo "|---|---|---|---|---|---|---|---|"
    for ((i = 0; i < ${#PARES[@]}; i += 2)); do
        par="${PARES[i]} -> ${PARES[i+1]}"
        awk -F'\t' -v par="$par" '
            $1 == par { n[$4 $2]++; tot++ }
            END {
                linha = "| " par
                for (k = 1; k <= 5; k++) {
                    c = substr("RISN?", k, 1)
                    a = n[c "+"] + 0; r = n[c "-"] + 0
                    linha = linha " | " a " / " r " / " a + r
                }
                rr = n["R+"] + n["R-"]
                pct = tot ? sprintf("%.1f%%", 100 * rr / tot) : "— (arquivo não alterado)"
                print linha " | " tot + 0 " | " pct " |"
            }' "$TSV"
    done
    echo
    echo "## 2. Lista completa das linhas classificadas como R ou ?"
    echo
    echo "Número de linha: na revisão antiga para removidas (-), na nova para adicionadas (+)."
    echo
    echo "| Par | Sinal | Linha | Categoria | Motivo | Texto |"
    echo "|---|---|---|---|---|---|"
    awk -F'\t' 'NR > 1 && ($4 == "R" || $4 == "?") {
        t = $6; gsub(/\|/, "\\|", t); gsub(/^ +/, "", t)
        print "| " $1 " | " $2 " | " $3 " | " $4 " | " $5 " | `" t "` |" }' "$TSV"
    echo
    echo "## 3. Script usado"
    echo
    echo '```bash'
    cat metricas/classificar-resumoservice.sh
    echo '```'
    echo
    echo '```java'
    cat metricas/ClassificaResumoService.java
    echo '```'
} > "$MD"
echo "Gerado: $MD e $TSV"
