# Classificação por linha — ResumoService.java

- Data: 2026-10-26T09:59:31-03:00
- git version 2.55.0.windows.5; Maven (wrapper do projeto): 3.9.16; Java: openjdk version "17.0.20.1" 2026-08-18
- Arquivo: `prototype/src/main/java/com/adapter/prototype/service/ResumoService.java`
- Diff: `git diff -U0 --no-renames <base> <alvo> -- prototype/src/main/java/com/adapter/prototype/service/ResumoService.java`
- Script: `metricas/classificar-resumoservice.sh` + `metricas/ClassificaResumoService.java` (critério completo no cabeçalho do .java e na seção 3)
- Linhas classificadas individualmente: `metricas/classificacao-resumoservice.tsv`

Categorias: R = regra de negócio; I = integração com provedor; S = seleção de provedor; N = neutra; ? = dúvida (não contada como R nem I).

## 1. Tabela cenário × categoria

Formato de cada célula: adicionadas / removidas / total.

| Par | R | I | S | N | ? | Total alterado | % R sobre o total |
|---|---|---|---|---|---|---|---|
| baseline-acoplada -> cenario-1-acoplada | 0 / 0 / 0 | 17 / 21 / 38 | 0 / 0 / 0 | 2 / 3 / 5 | 1 / 1 / 2 | 45 | 0.0% |
| baseline-acoplada -> cenario-2-acoplada | 3 / 2 / 5 | 49 / 11 / 60 | 20 / 0 / 20 | 47 / 10 / 57 | 1 / 1 / 2 | 144 | 3.5% |
| baseline-acoplada -> cenario-3-acoplada | 0 / 0 / 0 | 14 / 4 / 18 | 0 / 0 / 0 | 7 / 0 / 7 | 0 / 0 / 0 | 25 | 0.0% |
| baseline-desacoplada -> cenario-1-desacoplada | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 0 | — (arquivo não alterado) |
| baseline-desacoplada -> cenario-2-desacoplada | 1 / 1 / 2 | 0 / 0 / 0 | 18 / 0 / 18 | 9 / 0 / 9 | 0 / 3 / 3 | 32 | 6.2% |
| baseline-desacoplada -> cenario-3-desacoplada | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 0 / 0 / 0 | 0 | — (arquivo não alterado) |

## 2. Lista completa das linhas classificadas como R ou ?

Número de linha: na revisão antiga para removidas (-), na nova para adicionadas (+).

| Par | Sinal | Linha | Categoria | Motivo | Texto |
|---|---|---|---|---|---|
| baseline-acoplada -> cenario-1-acoplada | - | 66 | ? | marcadores mistos R+I (gerarResumo) | `GeminiRequest corpo = new GeminiRequest(PROMPT_BASE + texto);` |
| baseline-acoplada -> cenario-1-acoplada | + | 63 | ? | marcadores mistos R+I (gerarResumo) | `GroqRequest corpo = new GroqRequest(groqProperties.getModel(), PROMPT_BASE + texto);` |
| baseline-acoplada -> cenario-2-acoplada | + | 70 | ? | sem marcador (gerarResumo) | `String textoResumido;` |
| baseline-acoplada -> cenario-2-acoplada | + | 86 | R | marcador R (gerarResumo) | `ResumoResponse response = new ResumoResponse();` |
| baseline-acoplada -> cenario-2-acoplada | + | 87 | R | marcador R (gerarResumo) | `response.setResumo(textoResumido);` |
| baseline-acoplada -> cenario-2-acoplada | + | 88 | R | marcador R (gerarResumo) | `return response;` |
| baseline-acoplada -> cenario-2-acoplada | - | 82 | R | marcador R (gerarResumo) | `ResumoResponse response = new ResumoResponse();` |
| baseline-acoplada -> cenario-2-acoplada | - | 83 | ? | marcadores mistos R+I (gerarResumo) | `response.setResumo(extrairTextoGerado(resposta));` |
| baseline-acoplada -> cenario-2-acoplada | - | 84 | R | marcador R (gerarResumo) | `return response;` |
| baseline-desacoplada -> cenario-2-desacoplada | - | 17 | ? | sem marcador (campo) | `private final AiSummarizerClient aiSummarizerClient;` |
| baseline-desacoplada -> cenario-2-desacoplada | - | 19 | ? | sem marcador (ResumoService) | `public ResumoService(AiSummarizerClient aiSummarizerClient) {` |
| baseline-desacoplada -> cenario-2-desacoplada | - | 20 | ? | sem marcador (ResumoService) | `this.aiSummarizerClient = aiSummarizerClient;` |
| baseline-desacoplada -> cenario-2-desacoplada | - | 40 | R | marcador R (gerarResumo) | `String resumo = aiSummarizerClient.gerarResumo(PROMPT_BASE + texto);` |
| baseline-desacoplada -> cenario-2-desacoplada | + | 50 | R | marcador R (gerarResumo) | `String resumo = cliente.gerarResumo(PROMPT_BASE + texto);` |

## 3. Script usado

```bash
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
```

```java
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classifica cada linha adicionada/removida de ResumoService.java entre duas
 * revisões em R (regra de negócio), I (integração com provedor), S (seleção de
 * provedor), N (neutra) ou ? (dúvida). O critério é fixo e determinístico:
 *
 *  1. N: linha em branco, só comentário/Javadoc, só chaves/parênteses/';',
 *     só anotação, ou import genérico.
 *  2. Import de tipo do provedor (Gemini/Groq, pacotes client/config,
 *     org.springframework.http, org.springframework.web.client,
 *     ProvedorIndisponivelException) = I; import de ProvedorInvalidoException = S.
 *  3. Contexto do método: dentro de normalizarProvedor/selecionarCliente ou de
 *     um bloco switch = S; dentro de chamar*, extrairTextoGerado*, mapearErroHttp = I.
 *  4. Demais linhas (campos, construtor, gerarResumo): a instrução completa à
 *     qual a linha pertence, sem comentários e sem literais de string, é
 *     comparada com marcadores R, I e S. Exatamente uma família = essa
 *     categoria; nenhuma ou mais de uma = "?".
 *
 * Uso: java ClassificaResumoService.java <arquivo> <base> <alvo> [<base> <alvo> ...]
 * Saída: TSV (par, sinal, linha, categoria, motivo, texto) na saída padrão.
 */
public class ClassificaResumoService {

    static final Pattern R = Pattern.compile(
            "TAMANHO_MINIMO|TAMANHO_MAXIMO|PROMPT_BASE|TextoInvalidoException|ResumoResponse"
            + "|\\bresponse\\b|texto\\.length|texto\\.isBlank|getTexto");
    static final Pattern I = Pattern.compile(
            "Gemini|Groq|gemini|groq|RestTemplate|HttpHeaders|HttpEntity|MediaType|HttpStatus"
            + "|ResourceAccessException|RestClientException|ProvedorIndisponivelException"
            + "|postForObject|\\bheaders\\b|\\burl\\b|\\bresposta\\b|\\brequisicao\\b"
            + "|RESPONSE_MIME_TYPE|TEMPERATURE|GenerationConfig|extrairTextoGerado");
    static final Pattern S = Pattern.compile(
            "provedor|Provedor|PROVEDOR|\\bclientes\\b|selecionarCliente|normalizarProvedor|\\bswitch\\b");

    static final Set<String> METODOS_S = Set.of("normalizarProvedor", "selecionarCliente");
    static final Pattern METODOS_I = Pattern.compile("chamar\\w*|extrairTextoGerado\\w*|mapearErroHttp");

    static final Pattern IMPORT_I = Pattern.compile(
            "Gemini|Groq|\\.client\\.|\\.config\\.|org\\.springframework\\.http\\."
            + "|org\\.springframework\\.web\\.client\\.|ProvedorIndisponivelException");
    static final Pattern DECL_METODO = Pattern.compile(
            "^\\s*(?:(?:public|private|protected|static|final|synchronized)\\s+)*"
            + "(?:[\\w<>\\[\\],.?]+\\s+)?(\\w+)\\s*\\(");

    /** Informação por linha do arquivo completo. */
    static class Linha {
        String texto;      // linha original
        String codigo;     // sem comentários e sem conteúdo de literais
        String membro = "";  // método/construtor que contém a linha ("" = corpo da classe)
        boolean emSwitch;
        int instrucao = -1;  // índice da instrução a que pertence
    }

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        String arquivo = args[0];
        System.out.println("par\tsinal\tlinha\tcategoria\tmotivo\ttexto");
        for (int i = 1; i + 1 < args.length; i += 2) {
            String base = args[i], alvo = args[i + 1];
            String par = base + " -> " + alvo;
            List<Linha> antigo = analisar(git("show", base + ":" + arquivo));
            List<Linha> novo = analisar(git("show", alvo + ":" + arquivo));
            String diff = git("diff", "-U0", "--no-renames", base, alvo, "--", arquivo);

            Pattern hunk = Pattern.compile("^@@ -(\\d+)(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@");
            int nAnt = 0, nNovo = 0;
            for (String l : diff.split("\n", -1)) {
                Matcher m = hunk.matcher(l);
                if (m.find()) {
                    nAnt = Integer.parseInt(m.group(1));
                    nNovo = Integer.parseInt(m.group(2));
                } else if (l.startsWith("-") && !l.startsWith("---")) {
                    String[] c = classificar(antigo, nAnt - 1);
                    System.out.println(par + "\t-\t" + nAnt + "\t" + c[0] + "\t" + c[1] + "\t" + l.substring(1));
                    nAnt++;
                } else if (l.startsWith("+") && !l.startsWith("+++")) {
                    String[] c = classificar(novo, nNovo - 1);
                    System.out.println(par + "\t+\t" + nNovo + "\t" + c[0] + "\t" + c[1] + "\t" + l.substring(1));
                    nNovo++;
                }
            }
        }
    }

    static String[] classificar(List<Linha> linhas, int idx) {
        Linha l = linhas.get(idx);
        String cod = l.codigo.trim();
        String bruto = l.texto.trim();

        if (bruto.isEmpty()) return new String[] {"N", "linha em branco"};
        if (cod.isEmpty()) return new String[] {"N", "comentário"};
        if (cod.matches("[{}();,\\s]+")) return new String[] {"N", "chaves/delimitadores"};
        if (cod.matches("@\\w+(\\(.*\\))?")) return new String[] {"N", "anotação"};
        if (cod.startsWith("import ")) {
            if (bruto.contains("ProvedorInvalidoException")) return new String[] {"S", "import de seleção"};
            if (IMPORT_I.matcher(bruto).find()) return new String[] {"I", "import de tipo do provedor"};
            return new String[] {"N", "import genérico"};
        }
        if (cod.startsWith("package ")) return new String[] {"N", "package"};

        if (METODOS_S.contains(l.membro)) return new String[] {"S", "método " + l.membro};
        if (METODOS_I.matcher(l.membro).matches()) return new String[] {"I", "método " + l.membro};
        if (l.emSwitch) return new String[] {"S", "bloco switch de despacho"};

        StringBuilder instr = new StringBuilder();
        for (Linha o : linhas) if (o.instrucao == l.instrucao) instr.append(o.codigo).append(' ');
        String s = instr.toString();
        List<String> fam = new ArrayList<>();
        if (R.matcher(s).find()) fam.add("R");
        if (I.matcher(s).find()) fam.add("I");
        if (S.matcher(s).find()) fam.add("S");
        String onde = l.membro.isEmpty() ? "campo" : l.membro;
        if (fam.size() == 1) return new String[] {fam.get(0), "marcador " + fam.get(0) + " (" + onde + ")"};
        if (fam.isEmpty()) return new String[] {"?", "sem marcador (" + onde + ")"};
        return new String[] {"?", "marcadores mistos " + String.join("+", fam) + " (" + onde + ")"};
    }

    /** Remove comentários e conteúdo de literais; registra membro, switch e instrução de cada linha. */
    static List<Linha> analisar(String fonte) {
        List<Linha> out = new ArrayList<>();
        boolean emBloco = false;
        for (String t : fonte.split("\n", -1)) {
            Linha l = new Linha();
            l.texto = t.replace("\r", "");
            StringBuilder c = new StringBuilder();
            for (int i = 0; i < l.texto.length(); i++) {
                char ch = l.texto.charAt(i);
                if (emBloco) {
                    if (ch == '*' && i + 1 < l.texto.length() && l.texto.charAt(i + 1) == '/') { emBloco = false; i++; }
                    continue;
                }
                if (ch == '/' && i + 1 < l.texto.length() && l.texto.charAt(i + 1) == '/') break;
                if (ch == '/' && i + 1 < l.texto.length() && l.texto.charAt(i + 1) == '*') { emBloco = true; i++; continue; }
                if (ch == '"' || ch == '\'') {
                    char q = ch;
                    c.append(q);
                    i++;
                    while (i < l.texto.length() && l.texto.charAt(i) != q) { if (l.texto.charAt(i) == '\\') i++; i++; }
                    c.append(q);
                    continue;
                }
                c.append(ch);
            }
            l.codigo = c.toString();
            out.add(l);
        }

        int prof = 0, profSwitch = -1, instr = 0;
        String membro = "";
        boolean abriuMembro = false, instrAberta = false;
        for (Linha l : out) {
            String cod = l.codigo.trim();
            if (prof == 1 && !cod.isEmpty() && !instrAberta) {
                Matcher m = DECL_METODO.matcher(cod);
                int igual = cod.indexOf('='), par = cod.indexOf('(');
                if (m.find() && (igual < 0 || igual > par)) { membro = m.group(1); abriuMembro = false; }
            }
            if (profSwitch < 0 && cod.matches("switch\\s*\\(.*")) profSwitch = prof;
            l.membro = membro;
            l.emSwitch = profSwitch >= 0;
            if (!cod.isEmpty()) {
                l.instrucao = instr;
                instrAberta = !(cod.endsWith(";") || cod.endsWith("{") || cod.endsWith("}"));
                if (!instrAberta) instr++;
            }
            for (char ch : cod.toCharArray()) {
                if (ch == '{') { prof++; if (!membro.isEmpty()) abriuMembro = true; }
                if (ch == '}') prof--;
            }
            if (profSwitch >= 0 && prof <= profSwitch) profSwitch = -1;
            if (prof == 1 && abriuMembro) { membro = ""; abriuMembro = false; }
        }
        return out;
    }

    static String git(String... args) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add("git");
        cmd.addAll(List.of(args));
        Process p = new ProcessBuilder(cmd).redirectErrorStream(false).start();
        InputStream in = p.getInputStream();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        in.transferTo(buf);
        if (p.waitFor() != 0) throw new IllegalStateException("git falhou: " + cmd);
        return buf.toString(StandardCharsets.UTF_8);
    }
}
```
