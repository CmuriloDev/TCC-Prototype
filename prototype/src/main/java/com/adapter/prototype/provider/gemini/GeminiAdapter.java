package com.adapter.prototype.provider.gemini;

import com.adapter.prototype.provider.AiSummarizerClient;
import com.adapter.prototype.provider.ProvedorIndisponivelException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Adapter do padrão Adapter: implementa o Target ({@link AiSummarizerClient})
 * traduzindo a chamada genérica para o formato específico da API do Gemini
 * (Adaptee) e convertendo a resposta de volta em texto puro.
 */
@Component
public class GeminiAdapter implements AiSummarizerClient {

    private static final String RESPONSE_MIME_TYPE = "text/plain";
    private static final double TEMPERATURE = 0.7;

    private final RestTemplate restTemplate;
    private final GeminiProperties geminiProperties;

    public GeminiAdapter(@Qualifier("geminiRestTemplate") RestTemplate restTemplate,
                         GeminiProperties geminiProperties) {
        this.restTemplate = restTemplate;
        this.geminiProperties = geminiProperties;
    }

    @Override
    public String gerarResumo(String texto) throws ProvedorIndisponivelException {
        String url = geminiProperties.getUrl() + "/" + geminiProperties.getModel()
                + ":generateContent";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", geminiProperties.getKey());

        GeminiRequest corpo = new GeminiRequest(texto);
        corpo.setGenerationConfig(new GeminiRequest.GenerationConfig(RESPONSE_MIME_TYPE, TEMPERATURE));
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

        return extrairTextoGerado(resposta);
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

    // Visibilidade package-private (em vez de private) especificamente para
    // permitir teste unitário direto, sem expor a extração como API pública.
    String extrairTextoGerado(GeminiResponse resposta) {
        GeminiResponse.Response corpoResposta = resposta != null ? resposta.getResponse() : null;
        List<GeminiResponse.Candidate> candidates =
                corpoResposta != null ? corpoResposta.getCandidates() : null;

        if (candidates == null || candidates.isEmpty()) {
            throw new ProvedorIndisponivelException("Não foi possível gerar o resumo. Tente novamente.");
        }

        GeminiResponse.Content content = candidates.get(0).getContent();
        List<GeminiResponse.Part> parts = content != null ? content.getParts() : null;

        if (parts == null || parts.isEmpty()) {
            throw new ProvedorIndisponivelException("Não foi possível gerar o resumo. Tente novamente.");
        }

        String textoGerado = parts.stream()
                .map(GeminiResponse.Part::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining());

        if (textoGerado.isBlank()) {
            throw new ProvedorIndisponivelException("Não foi possível gerar o resumo. Tente novamente.");
        }

        return textoGerado;
    }
}
