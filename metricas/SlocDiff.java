import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Linhas efetivas (SLOC) adicionadas/removidas entre duas revisões, por arquivo.
 *
 * Uma linha é efetiva quando contém algum caractere fora de comentários e não é
 * em branco. Em .java, o arquivo completo da revisão é tokenizado (antiga para
 * linhas removidas, nova para adicionadas), reconhecendo //, /* *\/, Javadoc e
 * literais de string/caractere (um "//" dentro de string é código). Em
 * .properties, comentários são linhas cujo primeiro caractere não branco é '#'.
 * Demais extensões: só linhas em branco são descartadas (sinalizado na saída).
 *
 * Grupos: T = arquivo em src/test (aplicado primeiro); P = nome Gemini* ou Groq*
 * ou pacote provider/gemini|groq; O-config = src/main/resources; O-codigo = demais.
 *
 * Uso: java SlocDiff.java <base> <alvo> [<base> <alvo> ...]   (escopo: prototype/)
 * Saída TSV: par, grupo, arquivo, brutas+, brutas-, efetivas+, efetivas-, regra
 */
public class SlocDiff {

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        System.out.println("par\tgrupo\tarquivo\tbrutas_add\tbrutas_rem\tefetivas_add\tefetivas_rem\tregra");
        for (int i = 0; i + 1 < args.length; i += 2) {
            String base = args[i], alvo = args[i + 1];
            String numstat = git(true, "diff", "--numstat", "--no-renames", base, alvo, "--", "prototype/");
            for (String l : numstat.split("\n")) {
                if (l.isBlank()) continue;
                String[] c = l.split("\t");
                String arq = c[2];
                boolean[] antigo = efetivas(arq, git(false, "show", base + ":" + arq));
                boolean[] novo = efetivas(arq, git(false, "show", alvo + ":" + arq));
                String diff = git(true, "diff", "-U0", "--no-renames", base, alvo, "--", arq);
                int[] r = contar(diff, antigo, novo);
                System.out.println(base + " -> " + alvo + "\t" + grupo(arq) + "\t" + arq + "\t"
                        + c[0] + "\t" + c[1] + "\t" + r[0] + "\t" + r[1] + "\t" + regra(arq));
            }
        }
    }

    static String grupo(String arq) {
        String nome = arq.substring(arq.lastIndexOf('/') + 1);
        if (arq.contains("/src/test/")) return "T";
        if (nome.startsWith("Gemini") || nome.startsWith("Groq")
                || arq.contains("/provider/gemini/") || arq.contains("/provider/groq/")) return "P";
        if (arq.contains("/src/main/resources/")) return "O-config";
        return "O-codigo";
    }

    static String regra(String arq) {
        if (arq.endsWith(".java")) return "java";
        if (arq.endsWith(".properties")) return "properties";
        return "outro(so-brancos)";
    }

    /** Conta, nas linhas +/- do diff -U0, quantas são efetivas na revisão correspondente. */
    static int[] contar(String diff, boolean[] antigo, boolean[] novo) {
        Pattern hunk = Pattern.compile("^@@ -(\\d+)(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@");
        int add = 0, rem = 0, a = 0, n = 0;
        for (String l : diff.split("\n", -1)) {
            Matcher m = hunk.matcher(l);
            if (m.find()) { a = Integer.parseInt(m.group(1)); n = Integer.parseInt(m.group(2)); }
            else if (l.startsWith("-") && !l.startsWith("---")) { if (antigo[a - 1]) rem++; a++; }
            else if (l.startsWith("+") && !l.startsWith("+++")) { if (novo[n - 1]) add++; n++; }
        }
        return new int[] {add, rem};
    }

    /** Marca, por linha (índice 0), se ela é efetiva. Arquivo ausente na revisão = vazio. */
    static boolean[] efetivas(String arq, String fonte) {
        if (fonte == null) return new boolean[0];
        String[] linhas = fonte.split("\n", -1);
        boolean[] ef = new boolean[linhas.length];
        boolean emBloco = false;
        for (int k = 0; k < linhas.length; k++) {
            String t = linhas[k].replace("\r", "");
            if (arq.endsWith(".java")) {
                boolean codigo = false;
                for (int i = 0; i < t.length(); i++) {
                    char ch = t.charAt(i);
                    char prox = i + 1 < t.length() ? t.charAt(i + 1) : 0;
                    if (emBloco) {
                        if (ch == '*' && prox == '/') { emBloco = false; i++; }
                        continue;
                    }
                    if (ch == '/' && prox == '/') break;
                    if (ch == '/' && prox == '*') { emBloco = true; i++; continue; }
                    if (ch == '"' || ch == '\'') {
                        codigo = true;
                        char q = ch;
                        i++;
                        while (i < t.length() && t.charAt(i) != q) { if (t.charAt(i) == '\\') i++; i++; }
                        continue;
                    }
                    if (!Character.isWhitespace(ch)) codigo = true;
                }
                ef[k] = codigo;
            } else if (arq.endsWith(".properties")) {
                String s = t.strip();
                ef[k] = !s.isEmpty() && !s.startsWith("#");
            } else {
                ef[k] = !t.isBlank();
            }
        }
        return ef;
    }

    /** Executa git; com obrigatorio=false, retorna null se falhar (arquivo inexistente na revisão). */
    static String git(boolean obrigatorio, String... args) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add("git");
        cmd.addAll(List.of(args));
        Process p = new ProcessBuilder(cmd).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        p.getInputStream().transferTo(buf);
        if (p.waitFor() != 0) {
            if (obrigatorio) throw new IllegalStateException("git falhou: " + cmd);
            return null;
        }
        return buf.toString(StandardCharsets.UTF_8);
    }
}
