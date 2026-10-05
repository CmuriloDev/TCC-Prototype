package com.adapter.prototype.service;

import com.adapter.prototype.dto.ResumoRequest;
import com.adapter.prototype.dto.ResumoResponse;
import com.adapter.prototype.exception.ProvedorInvalidoException;
import com.adapter.prototype.exception.TextoInvalidoException;
import com.adapter.prototype.provider.AiSummarizerClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.TreeSet;

@Service
public class ResumoService {

    private static final int TAMANHO_MINIMO = 50;
    private static final int TAMANHO_MAXIMO = 5000;
    private static final String PROMPT_BASE =
            "Resuma o seguinte texto acadêmico de forma clara e objetiva: ";

    /** Implementações do Target indexadas pelo nome do bean de cada Adapter. */
    private final Map<String, AiSummarizerClient> clientes;
    private final String provedorPadrao;

    public ResumoService(Map<String, AiSummarizerClient> clientes,
                         @Value("${ai.provider.default}") String provedorPadrao) {
        this.clientes = clientes;
        this.provedorPadrao = provedorPadrao;
    }

    public ResumoResponse gerarResumo(ResumoRequest request) {
        String texto = request != null ? request.getTexto() : null;

        if (texto == null || texto.isBlank()) {
            throw new TextoInvalidoException("O texto não pode estar vazio.");
        }

        if (texto.length() < TAMANHO_MINIMO) {
            throw new TextoInvalidoException(
                    "O texto deve ter no mínimo " + TAMANHO_MINIMO + " caracteres.");
        }

        if (texto.length() > TAMANHO_MAXIMO) {
            throw new TextoInvalidoException(
                    "O texto não pode ultrapassar " + TAMANHO_MAXIMO + " caracteres.");
        }

        AiSummarizerClient cliente = selecionarCliente(request.getProvedor());
        String resumo = cliente.gerarResumo(PROMPT_BASE + texto);

        ResumoResponse response = new ResumoResponse();
        response.setResumo(resumo);
        return response;
    }

    private AiSummarizerClient selecionarCliente(String provedor) {
        String nome = (provedor == null || provedor.isBlank())
                ? provedorPadrao
                : provedor.trim().toLowerCase();

        AiSummarizerClient cliente = clientes.get(nome);
        if (cliente == null) {
            throw new ProvedorInvalidoException(
                    "Provedor inválido. Valores aceitos: "
                            + String.join(", ", new TreeSet<>(clientes.keySet())) + ".");
        }
        return cliente;
    }
}
