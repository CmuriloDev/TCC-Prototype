package com.adapter.prototype.service;

import com.adapter.prototype.client.GeminiRequest;
import com.adapter.prototype.client.GeminiResponse;
import com.adapter.prototype.client.GroqRequest;
import com.adapter.prototype.client.GroqResponse;
import com.adapter.prototype.config.GeminiProperties;
import com.adapter.prototype.config.GroqProperties;
import com.adapter.prototype.dto.ResumoRequest;
import com.adapter.prototype.dto.ResumoResponse;
import com.adapter.prototype.exception.ProvedorIndisponivelException;
import com.adapter.prototype.exception.ProvedorInvalidoException;
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

    private static final String PROVEDOR_PADRAO = "gemini";

    private final RestTemplate geminiRestTemplate;
    private final GeminiProperties geminiProperties;
    private final RestTemplate groqRestTemplate;
    private final GroqProperties groqProperties;

    public ResumoService(RestTemplate geminiRestTemplate, GeminiProperties geminiProperties,
            RestTemplate groqRestTemplate, GroqProperties groqProperties) {
        this.geminiRestTemplate = geminiRestTemplate;
        this.geminiProperties = geminiProperties;
        this.groqRestTemplate = groqRestTemplate;
        this.groqProperties = groqProperties;
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

        String provedor = normalizarProvedor(request.getProvedor());

        String textoResumido;
        // Roteamento direto e acoplado por provedor, propositalmente sem
        // interface comum entre as chamadas do Gemini e do Groq.
        switch (provedor) {
            case "gemini":
                textoResumido = chamarGemini(texto);
                break;
            case "groq":
                textoResumido = chamarGroq(texto);
                break;
            default:
                // Inatingível: normalizarProvedor já rejeita qualquer outro valor.
                throw new ProvedorInvalidoException(
                        "Provedor inválido. Valores aceitos: gemini, groq.");
        }

        ResumoResponse response = new ResumoResponse();
        response.setResumo(textoResumido);
        return response;
    }

    private String normalizarProvedor(String provedorBruto) {
        if (provedorBruto == null || provedorBruto.isBlank()) {
            return PROVEDOR_PADRAO;
        }

        String provedor = provedorBruto.trim().toLowerCase();
        if (!provedor.equals("gemini") && !provedor.equals("groq")) {
            throw new ProvedorInvalidoException("Provedor inválido. Valores aceitos: gemini, groq.");
        }

        return provedor;
    }

    // ---- Gemini ----

    private String chamarGemini(String texto) {
        String url = geminiProperties.getUrl() + "/" + geminiProperties.getModel()
                + ":generateContent";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", geminiProperties.getKey());

        GeminiRequest corpo = new GeminiRequest(PROMPT_BASE + texto);
        HttpEntity<GeminiRequest> requisicao = new HttpEntity<>(corpo, headers);

        GeminiResponse resposta;
        try {
            resposta = geminiRestTemplate.postForObject(url, requisicao, GeminiResponse.class);
        } catch (HttpStatusCodeException ex) {
            throw mapearErroHttp(ex);
        } catch (ResourceAccessException ex) {
            throw new ProvedorIndisponivelException(
                    "O serviço de IA está indisponível no momento. Tente novamente em instantes.", ex);
        } catch (RestClientException ex) {
            throw new ProvedorIndisponivelException(
                    "O serviço de IA está indisponível no momento. Tente novamente em instantes.", ex);
        }

        return extrairTextoGeradoGemini(resposta);
    }

    private String extrairTextoGeradoGemini(GeminiResponse resposta) {
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

    // ---- Groq ----

    private String chamarGroq(String texto) {
        String url = groqProperties.getUrl();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqProperties.getKey());

        GroqRequest corpo = new GroqRequest(groqProperties.getModel(), PROMPT_BASE + texto);
        HttpEntity<GroqRequest> requisicao = new HttpEntity<>(corpo, headers);

        GroqResponse resposta;
        try {
            resposta = groqRestTemplate.postForObject(url, requisicao, GroqResponse.class);
        } catch (HttpStatusCodeException ex) {
            throw mapearErroHttp(ex);
        } catch (ResourceAccessException ex) {
            throw new ProvedorIndisponivelException(
                    "O serviço de IA está indisponível no momento. Tente novamente em instantes.", ex);
        } catch (RestClientException ex) {
            throw new ProvedorIndisponivelException(
                    "O serviço de IA está indisponível no momento. Tente novamente em instantes.", ex);
        }

        return extrairTextoGeradoGroq(resposta);
    }

    private String extrairTextoGeradoGroq(GroqResponse resposta) {
        List<GroqResponse.Choice> choices = resposta != null ? resposta.getChoices() : null;

        if (choices == null || choices.isEmpty()) {
            throw new ProvedorIndisponivelException("Não foi possível gerar o resumo. Tente novamente.");
        }

        GroqResponse.Message message = choices.get(0).getMessage();
        String conteudo = message != null ? message.getContent() : null;

        if (conteudo == null) {
            throw new ProvedorIndisponivelException("Não foi possível gerar o resumo. Tente novamente.");
        }

        return conteudo;
    }

    // ---- Compartilhado ----

    /**
     * Traduz uma resposta HTTP não-2xx de qualquer provedor (ex.:
     * {@link HttpClientErrorException} ou {@link HttpServerErrorException}) em
     * uma mensagem apropriada ao caso. Reaproveitado entre Gemini e Groq porque
     * opera apenas sobre tipos genéricos do Spring (exceção e status HTTP),
     * nunca sobre a estrutura de resposta específica de um provedor.
     */
    private ProvedorIndisponivelException mapearErroHttp(HttpStatusCodeException ex) {
        if (ex.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            return new ProvedorIndisponivelException(
                    "Limite de requisições ao provedor de IA excedido.", ex);
        }

        return new ProvedorIndisponivelException(
                "O serviço de IA está indisponível no momento. Tente novamente em instantes.", ex);
    }
}
