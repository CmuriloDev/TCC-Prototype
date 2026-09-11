package com.adapter.prototype.service;

import com.adapter.prototype.client.GeminiRequest;
import com.adapter.prototype.client.GeminiResponse;
import com.adapter.prototype.config.GeminiProperties;
import com.adapter.prototype.dto.ResumoRequest;
import com.adapter.prototype.dto.ResumoResponse;
import com.adapter.prototype.exception.ProvedorIndisponivelException;
import com.adapter.prototype.exception.TextoInvalidoException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
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

        GeminiResponse resposta;
        try {
            resposta = restTemplate.postForObject(url, requisicao, GeminiResponse.class);
        } catch (HttpStatusCodeException ex) {
            throw mapearErroHttp(ex);
        } catch (ResourceAccessException ex) {
            throw new ProvedorIndisponivelException(
                    "O serviço de IA está indisponível no momento. Tente novamente em instantes.", ex);
        } catch (RestClientException ex) {
            throw new ProvedorIndisponivelException(
                    "O serviço de IA está indisponível no momento. Tente novamente em instantes.", ex);
        }

        ResumoResponse response = new ResumoResponse();
        response.setResumo(extrairTextoGerado(resposta));
        return response;
    }

    /**
     * Traduz uma resposta HTTP não-2xx do Gemini (ex.: {@link HttpClientErrorException}
     * ou {@link HttpServerErrorException}) em uma mensagem apropriada ao caso.
     */
    private ProvedorIndisponivelException mapearErroHttp(HttpStatusCodeException ex) {
        if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            return new ProvedorIndisponivelException(
                    "Limite de requisições ao provedor de IA excedido.", ex);
        }

        return new ProvedorIndisponivelException(
                "O serviço de IA está indisponível no momento. Tente novamente em instantes.", ex);
    }

    private String extrairTextoGerado(GeminiResponse resposta) {
        List<GeminiResponse.Candidate> candidates =
                resposta != null ? resposta.getCandidates() : null;

        if (candidates == null || candidates.isEmpty()) {
            throw new ProvedorIndisponivelException("Não foi possível gerar o resumo. Tente novamente.");
        }

        GeminiResponse.Content content = candidates.get(0).getContent();
        List<GeminiResponse.Part> parts = content != null ? content.getParts() : null;

        if (parts == null || parts.isEmpty() || parts.get(0).getText() == null) {
            throw new ProvedorIndisponivelException("Não foi possível gerar o resumo. Tente novamente.");
        }

        return parts.get(0).getText();
    }
}
