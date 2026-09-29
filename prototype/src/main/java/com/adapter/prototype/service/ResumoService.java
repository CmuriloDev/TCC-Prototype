package com.adapter.prototype.service;

import com.adapter.prototype.dto.ResumoRequest;
import com.adapter.prototype.dto.ResumoResponse;
import com.adapter.prototype.exception.TextoInvalidoException;
import com.adapter.prototype.provider.AiSummarizerClient;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {

    private static final int TAMANHO_MINIMO = 50;
    private static final int TAMANHO_MAXIMO = 5000;

    private final AiSummarizerClient aiSummarizerClient;

    public ResumoService(AiSummarizerClient aiSummarizerClient) {
        this.aiSummarizerClient = aiSummarizerClient;
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

        String resumo = aiSummarizerClient.gerarResumo(texto);

        ResumoResponse response = new ResumoResponse();
        response.setResumo(resumo);
        return response;
    }
}
