package com.adapter.prototype.provider.groq;

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

/**
 * Adapter do padrão Adapter: implementa o Target ({@link AiSummarizerClient})
 * traduzindo a chamada genérica para o formato específico da API do Groq
 * (Adaptee) e convertendo a resposta de volta em texto puro.
 */
@Component
public class GroqAdapter implements AiSummarizerClient {

    private final RestTemplate restTemplate;
    private final GroqProperties groqProperties;

    public GroqAdapter(@Qualifier("groqRestTemplate") RestTemplate restTemplate,
                       GroqProperties groqProperties) {
        this.restTemplate = restTemplate;
        this.groqProperties = groqProperties;
    }

    @Override
    public String gerarResumo(String texto) throws ProvedorIndisponivelException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqProperties.getKey());

        GroqRequest corpo = new GroqRequest(groqProperties.getModel(), texto);
        HttpEntity<GroqRequest> requisicao = new HttpEntity<>(corpo, headers);

        GroqResponse resposta;
        try {
            resposta = restTemplate.postForObject(groqProperties.getUrl(), requisicao, GroqResponse.class);
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
     * Traduz uma resposta HTTP não-2xx do Groq (ex.: {@link HttpClientErrorException}
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

    private String extrairTextoGerado(GroqResponse resposta) {
        List<GroqResponse.Choice> choices = resposta != null ? resposta.getChoices() : null;

        if (choices == null || choices.isEmpty()) {
            throw new ProvedorIndisponivelException("Não foi possível gerar o resumo. Tente novamente.");
        }

        GroqResponse.Message message = choices.get(0).getMessage();

        if (message == null || message.getContent() == null) {
            throw new ProvedorIndisponivelException("Não foi possível gerar o resumo. Tente novamente.");
        }

        return message.getContent();
    }
}
