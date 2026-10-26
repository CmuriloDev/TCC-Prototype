#!/usr/bin/env bash
# CBO (Coupling Between Objects) de cada classe de src/main nos 8 estados,
# calculado pela ferramenta CK (com.github.mauricioaniche:ck:0.7.0).
# Gera metricas/cbo.md e metricas/cbo-por-classe.csv.
# Uso: ./metricas/cbo.sh   (requer git, Java 17 e acesso ao Maven Central na 1ª execução;
#      CK_LIB=<pasta com os jars do CK> evita o download)
set -euo pipefail
export LC_ALL=C.UTF-8

RAIZ=$(git rev-parse --show-toplevel)
cd "$RAIZ"
CK_VERSAO=0.7.0
CSV=metricas/cbo-por-classe.csv
MD=metricas/cbo.md
TMP=$(mktemp -d)
ESTADOS=(baseline-acoplada cenario-1-acoplada cenario-2-acoplada cenario-3-acoplada
         baseline-desacoplada cenario-1-desacoplada cenario-2-desacoplada cenario-3-desacoplada)

winpath() { if command -v cygpath > /dev/null; then cygpath -w "$1"; else echo "$1"; fi; }

if [ -z "${CK_LIB:-}" ]; then
    CK_LIB="$TMP/lib"
    cat > "$TMP/pom.xml" <<EOF
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>local</groupId><artifactId>ck-runner</artifactId><version>1</version>
  <dependencies>
    <dependency><groupId>com.github.mauricioaniche</groupId><artifactId>ck</artifactId><version>$CK_VERSAO</version></dependency>
  </dependencies>
</project>
EOF
    ( cd prototype && ./mvnw -q -f "$(winpath "$TMP/pom.xml")" dependency:copy-dependencies \
        -DoutputDirectory="$(winpath "$CK_LIB")" )
fi
CP="$(winpath "$CK_LIB")/*"

echo "estado,classe,tipo,cbo,arquivo" > "$CSV"
for estado in "${ESTADOS[@]}"; do
    wt="$TMP/wt-$estado"; out="$TMP/out-$estado"
    git worktree add --detach "$wt" "$estado" > /dev/null 2>&1
    mkdir -p "$out"
    # Runner <dir> <useJars> <maxFilesPerPartition> <variablesAndFields> <outputDir>
    java -cp "$CP" com.github.mauricioaniche.ck.Runner \
        "$(winpath "$wt/prototype/src/main/java")" false 0 false "$(winpath "$out")/" > "$out/ck.log" 2>&1
    # class.csv: file,class,type,cbo,...  (file -> caminho relativo a prototype/)
    awk -F, -v e="$estado" 'NR > 1 {
        f = $1; gsub(/\\/, "/", f); sub(/.*\/prototype\//, "prototype/", f)
        print e "," $2 "," $3 "," $4 "," f }' "$out/class.csv" | sort -t, -k2,2 >> "$CSV"
    git worktree remove --force "$wt"
done
git worktree prune

{
    echo "# CBO (acoplamento entre classes) por estado"
    echo
    echo "- Data: $(date -Iseconds)"
    echo "- $(git --version); Maven (wrapper do projeto): $(sed -n 's/.*apache-maven-\([0-9.]*\)-bin.*/\1/p' prototype/.mvn/wrapper/maven-wrapper.properties); Java: $(java -version 2>&1 | head -1)"
    echo "- Ferramenta: CK $CK_VERSAO (com.github.mauricioaniche:ck:$CK_VERSAO, Maven Central)"
    echo "- Comando por estado: \`java -cp \"<ck-lib>/*\" com.github.mauricioaniche.ck.Runner <worktree>/prototype/src/main/java false 0 false <saida>/\`"
    echo "  (argumentos: diretório, useJars=false, maxFilesPerPartition=0 (automático), variablesAndFields=false, diretório de saída)"
    echo "- Escopo: somente prototype/src/main/java. Com useJars=false, tipos externos (Spring, JDK) não são resolvidos por classpath; o CK os conta pelo nome declarado."
    echo "- O CK reporta classes internas (ex.: GeminiResponse\$Candidate) como tipos próprios; elas entram nas contagens abaixo, e a coluna \"Top-level\" mostra só as de primeiro nível."
    echo "- Jars usados: $(ls "$CK_LIB" | tr '\n' ' ')"
    echo "- Por classe: \`$CSV\`"
    echo
    echo "| Estado | Tipos (CK) | Top-level | CBO ResumoService | CBO médio | CBO máximo (classe) |"
    echo "|---|---|---|---|---|---|"
    for estado in "${ESTADOS[@]}"; do
        awk -F, -v e="$estado" '
            NR > 1 && $1 == e {
                n++; s += $4
                if ($3 != "innerclass") top++
                if ($4 > max) { max = $4; cmax = $2 } else if ($4 == max) cmax = cmax ", " $2
                if ($2 ~ /\.ResumoService$/) rs = $4
            }
            END {
                sub(/com\.adapter\.prototype\./, "", cmax); gsub(/com\.adapter\.prototype\./, "", cmax)
                printf "| %s | %d | %d | %s | %.2f | %d (%s) |\n", e, n, top, rs, s / n, max, cmax
            }' "$CSV"
    done
} > "$MD"
rm -rf "$TMP"
echo "Gerado: $MD e $CSV"
