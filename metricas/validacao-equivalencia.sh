#!/usr/bin/env bash
# Registro sistemático da equivalência funcional (validação manual automatizada).
# Para cada estado (tag), cria um worktree temporário, empacota a aplicação,
# sobe na porta 8081 e envia POST /api/resumo com os casos C1..C10.
# Estados do Cenário 3 não chamam a API: executam ./mvnw -q test.
#
# Uso: ./metricas/validacao-equivalencia.sh [tag ...]
# Requer: GEMINI_API_KEY e GROQ_API_KEY no ambiente (nunca são impressas),
#         Java 17, curl, git. Saída: metricas/validacao-equivalencia-bruto.txt
set -u
export LC_ALL=C.UTF-8

RAIZ=$(git rev-parse --show-toplevel)
SAIDA="$RAIZ/metricas/validacao-equivalencia-bruto.txt"
TMP=$(mktemp -d)
PORTA=8081
URL="http://localhost:$PORTA/api/resumo"
MAX_REPETICOES=3     # repetições além da 1ª tentativa, só em falha transitória
INTERVALO=5

ESTADOS=("$@")
[ ${#ESTADOS[@]} -eq 0 ] && ESTADOS=(baseline-acoplada cenario-1-acoplada cenario-2-acoplada \
    baseline-desacoplada cenario-1-desacoplada cenario-2-desacoplada \
    cenario-3-acoplada cenario-3-desacoplada)

for v in GEMINI_API_KEY GROQ_API_KEY; do
    [ -n "${!v:-}" ] || { echo "Variável $v ausente. Abortando."; exit 1; }
done

MSG_VAZIO="O texto não pode estar vazio."
MSG_MIN="O texto deve ter no mínimo 50 caracteres."
MSG_MAX="O texto não pode ultrapassar 5000 caracteres."
MSG_INDISP="O serviço de IA está indisponível no momento. Tente novamente em instantes."
MSG_PROV="Provedor inválido. Valores aceitos: gemini, groq."

TEXTO_VALIDO="A arquitetura de software influencia diretamente a manutenibilidade de sistemas que dependem de serviços externos. Quando a integração com um provedor de inteligência artificial é feita de forma acoplada, mudanças no provedor tendem a se propagar para as regras de negócio. O padrão Adapter isola essas dependências atrás de uma interface estável, de modo que a substituição ou inclusão de provedores exija alterações localizadas, reduzindo o custo de evolução e o risco de regressões."
TEXTO_CURTO="Texto curto demais para resumir."
TEXTO_LONGO=$(printf 'a%.0s' $(seq 1 5100))

{
    echo "# Validação de equivalência funcional — saída bruta"
    echo "# Data: $(date -Iseconds)"
    echo "# $(git --version)"
    echo "# Maven (wrapper do projeto): $(sed -n 's/.*apache-maven-\([0-9.]*\)-bin.*/\1/p' "$RAIZ/prototype/.mvn/wrapper/maven-wrapper.properties")"
    echo "# Java: $(java -version 2>&1 | head -1)"
    echo "# Porta: $PORTA | repetições máx. em falha transitória: $MAX_REPETICOES (intervalo ${INTERVALO}s)"
    echo "# Texto válido: ${#TEXTO_VALIDO} caracteres | curto: ${#TEXTO_CURTO} | longo: ${#TEXTO_LONGO}"
    echo "# Colunas: estado | caso | tentativa | status_http | duracao_s | esperado | veredito | corpo (resumo truncado em 80 caracteres)"
} > "$SAIDA"

log() { echo "$*" | tee -a "$SAIDA"; }

pid_na_porta() {
    netstat -ano | awk -v p=":$PORTA" '$2 ~ p"$" && $4=="LISTENING" {print $5; exit}'
}

parar_app() {
    local pid; pid=$(pid_na_porta)
    [ -z "$pid" ] && return
    if tasklist //FI "PID eq $pid" //FO CSV //NH | grep -qi '"java.exe"'; then
        taskkill //PID "$pid" //F > /dev/null
    else
        log "ERRO: porta $PORTA ocupada por processo não-java (PID $pid); não finalizado."
        exit 1
    fi
    for _ in $(seq 1 30); do [ -z "$(pid_na_porta)" ] && return; sleep 1; done
}

# subir_app <jar> [VAR=valor ...] — variáveis sobrescritas só neste processo
subir_app() {
    local jar=$1; shift
    parar_app
    env "$@" java -jar "$jar" --server.port=$PORTA > "$TMP/app.log" 2>&1 &
    for _ in $(seq 1 90); do
        local c; c=$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$PORTA/" || true)
        [ "$c" != "000" ] && return 0
        sleep 1
    done
    log "ERRO: aplicação não subiu em 90s ($jar)"; tail -20 "$TMP/app.log" | tee -a "$SAIDA"
    return 1
}

campo() { # extrai o valor string de um campo JSON simples
    sed -n "s/.*\"$1\":\"\\(\\([^\"\\\\]\\|\\\\.\\)*\\)\".*/\\1/p" <<< "$2"
}

# caso <estado> <id> <texto> <provedor|-> <esperado: 200 | 400:msg | 502:msg>
caso() {
    local estado=$1 id=$2 texto=$3 prov=$4 esperado=$5
    local corpo
    if [ "$prov" = "-" ]; then
        corpo=$(printf '{"texto":"%s"}' "$texto")
    else
        corpo=$(printf '{"texto":"%s","provedor":"%s"}' "$texto" "$prov")
    fi
    printf '%s' "$corpo" > "$TMP/req.json"

    local t=1 status dur resp veredito trecho
    while :; do
        local out
        out=$(curl -s -m 120 -o "$TMP/resp.json" -w '%{http_code} %{time_total}' \
              -H 'Content-Type: application/json' --data-binary @"$TMP/req.json" "$URL")
        status=${out% *}; dur=${out#* }
        resp=$(cat "$TMP/resp.json" 2>/dev/null)

        veredito=DIVERGENTE
        case $esperado in
            200)  local r; r=$(campo resumo "$resp")
                  trecho="resumo=$(awk '{print substr($0,1,80)}' <<< "$r")"
                  [ "$status" != 200 ] && trecho="mensagem=$(campo mensagem "$resp")"
                  [ "$status" = 200 ] && [ -n "$r" ] && veredito=OK ;;
            *)    local m; m=$(campo mensagem "$resp")
                  trecho="mensagem=$m"
                  [ -z "$m" ] && trecho="corpo=$(head -c 200 <<< "$resp")"
                  [ "$status" = "${esperado%%:*}" ] && [ "$m" = "${esperado#*:}" ] && veredito=OK ;;
        esac
        log "$estado | $id | $t | $status | $dur | ${esperado%%:*} | $veredito | $trecho"

        # Falha transitória do provedor: só em casos que esperam 200.
        if [ "$veredito" = OK ] || [ "$esperado" != 200 ] || [ $t -gt $MAX_REPETICOES ]; then break; fi
        t=$((t + 1)); sleep $INTERVALO
    done
}

for estado in "${ESTADOS[@]}"; do
    wt="$TMP/wt-$estado"
    git -C "$RAIZ" worktree add --detach "$wt" "$estado" > /dev/null 2>&1 || { log "ERRO: worktree $estado"; continue; }
    commit=$(git -C "$wt" rev-parse --short HEAD)
    log "## estado=$estado commit=$commit"

    if [[ $estado == cenario-3-* ]]; then
        ( cd "$wt/prototype" && ./mvnw -q test ) > "$TMP/test.log" 2>&1
        log "mvnw -q test: exit=$?"
        for f in "$wt"/prototype/target/surefire-reports/TEST-*.xml; do
            [ -f "$f" ] || continue
            log "suite $(basename "$f" .xml | sed 's/^TEST-//')" \
                "$(grep -o '<testsuite [^>]*' "$f" | grep -o '\(tests\|failures\|errors\|skipped\)="[0-9]*"' | tr '\n' ' ')"
            grep -o '<testcase name="[^"]*"' "$f" | sed 's/<testcase name=/  caso: /' | tee -a "$SAIDA"
        done
    else
        ( cd "$wt/prototype" && ./mvnw -q -DskipTests package ) > "$TMP/build.log" 2>&1 \
            || { log "ERRO: build $estado"; tail -20 "$TMP/build.log" | tee -a "$SAIDA"; }
        jar=$(ls "$wt"/prototype/target/*.jar | head -1)

        if subir_app "$jar"; then
            caso "$estado" C1 "$TEXTO_VALIDO" - 200
            caso "$estado" C2 "" - "400:$MSG_VAZIO"
            caso "$estado" C3 "$TEXTO_CURTO" - "400:$MSG_MIN"
            caso "$estado" C4 "$TEXTO_LONGO" - "400:$MSG_MAX"
            if [[ $estado == cenario-2-* ]]; then
                caso "$estado" C6 "$TEXTO_VALIDO" groq 200
                caso "$estado" C7 "$TEXTO_VALIDO" "  GROQ " 200
                caso "$estado" C8 "$TEXTO_VALIDO" gemini 200
                caso "$estado" C9 "$TEXTO_VALIDO" openai "400:$MSG_PROV"
                caso "$estado" C10 "$TEXTO_VALIDO" "" 200
            fi
        fi

        # C5: chave inválida de cada provedor ativo no estado
        props=$(git -C "$RAIZ" show "$estado:prototype/src/main/resources/application.properties")
        if grep -q '^gemini.api.key' <<< "$props"; then
            prov=-; [[ $estado == cenario-2-* ]] && prov=gemini
            subir_app "$jar" GEMINI_API_KEY=chave-invalida-teste && caso "$estado" C5-gemini "$TEXTO_VALIDO" "$prov" "502:$MSG_INDISP"
        fi
        if grep -q '^groq.api.key' <<< "$props"; then
            prov=-; [[ $estado == cenario-2-* ]] && prov=groq
            subir_app "$jar" GROQ_API_KEY=chave-invalida-teste && caso "$estado" C5-groq "$TEXTO_VALIDO" "$prov" "502:$MSG_INDISP"
        fi
        parar_app
    fi
    git -C "$RAIZ" worktree remove --force "$wt"
done
git -C "$RAIZ" worktree prune
rm -rf "$TMP"
