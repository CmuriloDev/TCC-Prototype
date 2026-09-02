package com.adapter.prototype.service;

import com.adapter.prototype.client.GeminiRequest;
import com.adapter.prototype.client.GeminiResponse;
import com.adapter.prototype.config.GeminiProperties;
import com.adapter.prototype.dto.ResumoRequest;
import com.adapter.prototype.dto.ResumoResponse;
import com.adapter.prototype.exception.TextoInvalidoException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class ResumoService {

    private static final int TAMANHO_MINIMO = 50;
    private static final int TAMANHO_MAXIMO = 5000;
    private static final String PROMPT_BASE =
            "Resuma o seguinte texto acadêmico de forma clara e objetiva: ";

    private final RestTemplate restTemplate;
    private final GeminiProperties geminiProperties;

    public ResumoService(RestTemplate geminiRestTemplate, GeminiProperties geminiProperties) {
        this.restTemplate = geminiRestTemplate;
        this.geminiProperties = geminiProperties;
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

        // Chamada direta e acoplada à API do Gemini, propositalmente sem
        // camada de abstração intermediária.
        String url = geminiProperties.getUrl() + "/" + geminiProperties.getModel()
                + ":generateContent";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", geminiProperties.getKey());

        GeminiRequest corpo = new GeminiRequest(PROMPT_BASE + texto);
        HttpEntity<GeminiRequest> requisicao = new HttpEntity<>(corpo, headers);

        GeminiResponse resposta =
                restTemplate.postForObject(url, requisicao, GeminiResponse.class);

        ResumoResponse response = new ResumoResponse();
        response.setResumo(extrairTextoGerado(resposta));
        return response;
    }

    private String extrairTextoGerado(GeminiResponse resposta) {
        List<GeminiResponse.Candidate> candidates =
                resposta != null ? resposta.getCandidates() : null;

        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("Resposta da API do Gemini sem candidatos.");
        }

        GeminiResponse.Content content = candidates.get(0).getContent();
        List<GeminiResponse.Part> parts = content != null ? content.getParts() : null;

        if (parts == null || parts.isEmpty() || parts.get(0).getText() == null) {
            throw new IllegalStateException("Resposta da API do Gemini sem texto gerado.");
        }

        return parts.get(0).getText();
    }
}
