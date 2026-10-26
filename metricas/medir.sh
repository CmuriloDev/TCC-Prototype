#!/usr/bin/env bash
# Medicao somente-leitura dos cenarios.
# Uso: ./metricas/medir.sh                       (os 6 pares padrao abaixo)
#      ./metricas/medir.sh "<base> <alvo>" ...   (pares informados)
# Adaptado de medir.sh: O dividido em Oc (codigo) e Of (configuracao), por
# regra de nome, sem dependencias externas.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

PARES=(
  "295d660 cenario-1-acoplada"
  "295d660 cenario-2-acoplada"
  "295d660 cenario-3-acoplada"
  "497ed71 cenario-1-desacoplada"
  "497ed71 cenario-2-desacoplada"
  "497ed71 cenario-3-desacoplada"
)
[ $# -gt 0 ] && PARES=("$@")

echo "# Data: $(date -Iseconds)"
echo "# $(git --version) | commit atual: $(git rev-parse --short HEAD)"
echo "# Maven (wrapper do projeto): $(sed -n 's/.*apache-maven-\([0-9.]*\)-bin.*/\1/p' prototype/.mvn/wrapper/maven-wrapper.properties) | Java: $(java -version 2>&1 | head -1) (nao usados nesta medicao)"
echo

# Classificacao unica: recebe o caminho (novo, quando renomeado)
grupo() {
  local p="$1" base
  base=$(basename "$p")
  # T antes de P: um teste no pacote provider/ continua sendo teste
  if [[ "$p" == prototype/src/test/* ]]; then
    echo T
  elif [[ "$p" == *"/provider/gemini/"* || "$p" == *"/provider/groq/"* || "$base" == Gemini* || "$base" == Groq* ]]; then
    echo P
  elif [[ "$p" == prototype/src/main/resources/* ]]; then
    echo Of
  else
    echo Oc
  fi
}

# Imprime tabela + totais para um par e um modo ("--no-renames" ou "")
medir() {
  local a="$1" b="$2" modo="$3"
  local ns st
  mapfile -t ns < <(git diff --numstat $modo "$a" "$b" -- prototype/)
  mapfile -t st < <(git diff --name-status $modo "$a" "$b" -- prototype/)

  declare -A arq=( [P]=0 [T]=0 [Oc]=0 [Of]=0 ) lin=( [P]=0 [T]=0 [Oc]=0 [Of]=0 )
  echo "| arquivo | status | + | - | grupo |"
  echo "|---|---|---:|---:|:---:|"
  local i
  for i in "${!st[@]}"; do
    IFS=$'\t' read -r s c1 c2 <<< "${st[$i]}"
    IFS=$'\t' read -r add del _ <<< "${ns[$i]}"
    local caminho="$c1" mostra="${c1#prototype/}"
    if [[ "$s" == R* ]]; then
      caminho="$c2"; mostra="${c1#prototype/} → ${c2#prototype/}"
    fi
    local g; g=$(grupo "$caminho")
    echo "| $mostra | $s | $add | $del | $g |"
    arq[$g]=$(( arq[$g] + 1 ))
    lin[$g]=$(( lin[$g] + add + del ))
  done
  local oa=$(( arq[Oc] + arq[Of] )) ol=$(( lin[Oc] + lin[Of] ))
  local ta=$(( arq[P] + arq[T] + oa )) tl=$(( lin[P] + lin[T] + ol ))
  echo
  echo "TOTAIS|P ${arq[P]} arq / ${lin[P]} lin|T ${arq[T]} arq / ${lin[T]} lin|O ${oa} arq / ${ol} lin (codigo ${arq[Oc]} / ${lin[Oc]}, config ${arq[Of]} / ${lin[Of]})|GERAL $ta arq / $tl lin"
}

for par in "${PARES[@]}"; do
  read -r a b <<< "$par"
  echo "################ $a -> $b ($(git rev-parse --short "$b^{commit}"))"
  echo "--- EXCLUIDOS (fora de prototype/):"
  git diff --name-status --no-renames "$a" "$b" -- . ':!prototype/' || true
  echo "--- MODO --no-renames"
  medir "$a" "$b" "--no-renames"
  echo "--- MODO padrao (deteccao de renomeacao)"
  medir "$a" "$b" ""
  echo
done
