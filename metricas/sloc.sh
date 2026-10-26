#!/usr/bin/env bash
# Linhas efetivas (SLOC) dos 6 pares de medição, por grupo P/T/O.
# Gera metricas/sloc.md e metricas/sloc-por-arquivo.tsv.
# Uso: ./metricas/sloc.sh   (requer git e Java 17)
set -euo pipefail
export LC_ALL=C.UTF-8

RAIZ=$(git rev-parse --show-toplevel)
cd "$RAIZ"
TSV=metricas/sloc-por-arquivo.tsv
MD=metricas/sloc.md
PARES=(baseline-acoplada cenario-1-acoplada baseline-acoplada cenario-2-acoplada
       baseline-acoplada cenario-3-acoplada baseline-desacoplada cenario-1-desacoplada
       baseline-desacoplada cenario-2-desacoplada baseline-desacoplada cenario-3-desacoplada)

java -Dfile.encoding=UTF-8 metricas/SlocDiff.java "${PARES[@]}" > "$TSV"

# tabela <filtro awk de grupo> : uma linha por par
tabela() {
    awk -F'\t' -v filtro="$1" '
        NR > 1 && ($2 ~ filtro) {
            p = $1; if (!(p in f)) ordem[++n] = p
            f[p]++; ba[p] += $4; br[p] += $5; ea[p] += $6; er[p] += $7
        }
        END {
            for (i = 1; i <= n; i++) { p = ordem[i]
                printf "| %s | %d | %d | %d | %d | %d | %d / %d / %d |\n", p, f[p], ea[p], er[p], ea[p] + er[p], ba[p] + br[p], ba[p], br[p], ba[p] + br[p] }
        }' "$TSV"
}

{
    echo "# Linhas de código efetivas (SLOC) por cenário e versão"
    echo
    echo "- Data: $(date -Iseconds)"
    echo "- $(git --version); Maven (wrapper do projeto): $(sed -n 's/.*apache-maven-\([0-9.]*\)-bin.*/\1/p' prototype/.mvn/wrapper/maven-wrapper.properties); Java: $(java -version 2>&1 | head -1)"
    echo "- Arquivos e linhas: \`git diff --numstat --no-renames <base> <alvo> -- prototype/\` e \`git diff -U0 --no-renames\` por arquivo"
    echo "- Efetiva = não em branco e não só comentário. .java: tokenização do arquivo completo na revisão (antiga para removidas, nova para adicionadas), reconhecendo //, /* */ e Javadoc, ignorando marcadores dentro de literais. .properties: comentário começa por #."
    echo "- Grupos: T = src/test (aplicado primeiro); P = Gemini*/Groq* ou pacote provider/gemini|groq; O-código; O-config (src/main/resources)."
    echo "- Script: \`metricas/sloc.sh\` + \`metricas/SlocDiff.java\`; por arquivo: \`$TSV\`"
    echo
    echo "Colunas: arquivos, efetivas adicionadas, efetivas removidas, total efetivo, total bruto (critério original), bruto adicionadas / removidas / total."
    echo
    for g in "Todos os grupos:." "P (provedor):^P$" "O-código:^O-codigo$" "O-config:^O-config$" "T (teste):^T$"; do
        echo "## ${g%%:*}"
        echo
        echo "| Par | Arquivos | Efetivas + | Efetivas − | Total efetivo | Total bruto | Bruto + / − / total |"
        echo "|---|---|---|---|---|---|---|"
        tabela "${g#*:}"
        echo
    done
    if grep -q 'outro(so-brancos)' "$TSV"; then
        echo "Atenção: há arquivos que não são .java nem .properties (só linhas em branco descartadas)."
    fi
} > "$MD"
echo "Gerado: $MD e $TSV"
