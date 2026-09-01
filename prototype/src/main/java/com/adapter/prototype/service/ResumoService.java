package com.adapter.prototype.service;

import com.adapter.prototype.dto.ResumoRequest;
import com.adapter.prototype.dto.ResumoResponse;
import com.adapter.prototype.exception.TextoInvalidoException;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {

    private static final int TAMANHO_MINIMO = 50;
    private static final int TAMANHO_MAXIMO = 5000;
    private static final int TAMANHO_RESUMO = 100;
    private static final String SUFIXO_SIMULADO = " [resumo simulado]";

    public ResumoResponse gerarResumo(ResumoRequest request) {
        String texto = request != null ? request.getTexto() : null;

        if (texto == null || texto.isBlank()) {
            throw new TextoInvalidoException("O campo 'texto' não pode ser nulo ou vazio.");
        }

        if (texto.length() < TAMANHO_MINIMO) {
            throw new TextoInvalidoException(
                    "O texto deve ter no mínimo " + TAMANHO_MINIMO + " caracteres.");
        }

        if (texto.length() > TAMANHO_MAXIMO) {
            throw new TextoInvalidoException(
                    "O texto deve ter no máximo " + TAMANHO_MAXIMO + " caracteres.");
        }

        int fim = Math.min(TAMANHO_RESUMO, texto.length());
        String resumo = texto.substring(0, fim) + SUFIXO_SIMULADO;

        ResumoResponse response = new ResumoResponse();
        response.setResumo(resumo);
        return response;
    }
}
