#!/usr/bin/env bash
# Validação — rodada 2: reexecuta só os casos que dependem do Gemini e ficaram
# DIVERGENTE na rodada 1, mais os controles do outro lado de cada par A x B.
# Cada estado roda em worktree temporário (git worktree add --detach), na porta
# 8081, com log DEBUG de org.springframework.web (flag de execução, sem alterar
# código) para registrar o status devolvido pelo Gemini quando a aplicação dá 502.
#
# Uso: ./metricas/validacao-rodada-2.sh <rotulo-da-rodada>
# Requer: GEMINI_API_KEY no ambiente (nunca é impressa). Saída: anexa em
#         metricas/validacao-rodada-2-bruto.txt
set -u
export LC_ALL=C.UTF-8

ROTULO=${1:?informe o rótulo da rodada}
RAIZ=$(git rev-parse --show-toplevel)
SAIDA="$RAIZ/metricas/validacao-rodada-2-bruto.txt"
TMP=$(mktemp -d)
PORTA=8081
URL="http://localhost:$PORTA/api/resumo"
MAX_TENTATIVAS=5
INTERVALO=20

[ -n "${GEMINI_API_KEY:-}" ] || { echo "Variável GEMINI_API_KEY ausente. Abortando."; exit 1; }
sem_chave() { sed "s/$GEMINI_API_KEY/<CHAVE_REMOVIDA>/g"; }

TEXTO_VALIDO="A arquitetura de software influencia diretamente a manutenibilidade de sistemas que dependem de serviços externos. Quando a integração com um provedor de inteligência artificial é feita de forma acoplada, mudanças no provedor tendem a se propagar para as regras de negócio. O padrão Adapter isola essas dependências atrás de uma interface estável, de modo que a substituição ou inclusão de provedores exija alterações localizadas, reduzindo o custo de evolução e o risco de regressões."

# estado | caso | provedor (- = campo ausente) | papel
PLANO=(
    "baseline-acoplada|C1|-|alvo (a)"
    "baseline-desacoplada|C1|-|controle de (a)"
    "cenario-2-acoplada|C1|-|alvo (b)"
    "cenario-2-acoplada|C8|gemini|alvo (c)"
    "cenario-2-desacoplada|C8|gemini|alvo (d)"
    "cenario-2-desacoplada|C1|-|controle de (b)"
)

log() { echo "$*" | sem_chave | tee -a "$SAIDA"; }

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

subir_app() {
    parar_app
    java -jar "$1" --server.port=$PORTA --logging.level.org.springframework.web=DEBUG > "$TMP/app.log" 2>&1 &
    for _ in $(seq 1 90); do
        local c; c=$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$PORTA/" || true)
        [ "$c" != "000" ] && return 0
        sleep 1
    done
    log "ERRO: aplicação não subiu em 90s ($1)"; tail -20 "$TMP/app.log" | sem_chave | tee -a "$SAIDA"
    return 1
}

campo() { # valor string de um campo JSON simples
    sed -n "s/.*\"$1\":\"\\(\\([^\"\\\\]\\|\\\\.\\)*\\)\".*/\\1/p" <<< "$2"
}

# caso <estado> <id> <provedor|-> <papel>
caso() {
    local estado=$1 id=$2 prov=$3 papel=$4
    if [ "$prov" = "-" ]; then
        printf '{"texto":"%s"}' "$TEXTO_VALIDO" > "$TMP/req.json"
    else
        printf '{"texto":"%s","provedor":"%s"}' "$TEXTO_VALIDO" "$prov" > "$TMP/req.json"
    fi
    log "### $estado | $id | $papel | corpo da requisição: $(sed "s/$TEXTO_VALIDO/<TEXTO_VALIDO 485 caracteres>/" "$TMP/req.json")"

    local t
    for t in $(seq 1 $MAX_TENTATIVAS); do
        local inicio_log hora out status dur resp r
        inicio_log=$(wc -l < "$TMP/app.log")
        hora=$(date '+%Y-%m-%d %H:%M:%S')
        out=$(curl -s -m 120 -o "$TMP/resp.json" -w '%{http_code} %{time_total}' \
              -H 'Content-Type: application/json' --data-binary @"$TMP/req.json" "$URL")
        status=${out% *}; dur=${out#* }
        resp=$(cat "$TMP/resp.json" 2>/dev/null)
        r=$(campo resumo "$resp")
        sleep 1
        log "LINHA | $ROTULO | $estado | $id | $t | $hora | $status | $dur | ${#r} | $(tr '\n' ' ' <<< "$resp" | awk '{print substr($0,1,300)}')"
        log "  corpo completo: $resp"
        if [ "$status" != 200 ]; then
            log "  log da aplicação nesta tentativa:"
            tail -n +$((inicio_log + 1)) "$TMP/app.log" | grep -a -v '^\s*$' | sem_chave | sed 's/^/    /' | tee -a "$SAIDA"
        fi
        [ "$status" = 200 ] && [ -n "$r" ] && break
        [ $t -lt $MAX_TENTATIVAS ] && sleep $INTERVALO
    done
}

{
    echo
    echo "################################################################"
    echo "# Rodada: $ROTULO | início: $(date -Iseconds)"
    echo "# Porta: $PORTA | tentativas máx.: $MAX_TENTATIVAS | intervalo: ${INTERVALO}s | uma requisição por vez"
    echo "# Java: $(java -version 2>&1 | head -1)"
    echo "# Colunas LINHA: rodada | estado | caso | tentativa | horário | status | duração_s | tamanho_resumo | corpo (300 caracteres)"
} | tee -a "$SAIDA"

estado_atual=""; jar=""
for item in "${PLANO[@]}"; do
    IFS='|' read -r estado id prov papel <<< "$item"
    if [ "$estado" != "$estado_atual" ]; then
        if [ -n "$estado_atual" ]; then parar_app; git -C "$RAIZ" worktree remove --force "$TMP/wt"; fi
        estado_atual=$estado
        git -C "$RAIZ" worktree add --detach "$TMP/wt" "$estado" > /dev/null 2>&1 || { log "ERRO: worktree $estado"; exit 1; }
        log "## estado=$estado commit=$(git -C "$TMP/wt" rev-parse --short HEAD)"
        ( cd "$TMP/wt/prototype" && ./mvnw -q -DskipTests package ) > "$TMP/build.log" 2>&1 \
            || { log "ERRO: build $estado"; tail -20 "$TMP/build.log" | tee -a "$SAIDA"; exit 1; }
        jar=$(ls "$TMP"/wt/prototype/target/*.jar | head -1)
        subir_app "$jar" || exit 1
    else
        sleep $INTERVALO
    fi
    caso "$estado" "$id" "$prov" "$papel"
done
parar_app
git -C "$RAIZ" worktree remove --force "$TMP/wt"
git -C "$RAIZ" worktree prune
log "# Rodada $ROTULO concluída: $(date -Iseconds)"
rm -rf "$TMP"
