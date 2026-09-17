package com.adapter.prototype.service;

import com.adapter.prototype.client.GeminiResponse;
import com.adapter.prototype.config.GeminiProperties;
import com.adapter.prototype.exception.ProvedorIndisponivelException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Valida a extração do texto gerado (ResumoService#extrairTextoGerado) contra
 * o novo formato de resposta simulado no Cenário 3 (response.candidates[]
 * .content.parts[], com possível fragmentação em múltiplas partes).
 *
 * <p>Desserializa JSON de exemplo diretamente em {@link GeminiResponse} e
 * chama o método de extração isoladamente, sem RestTemplate/chamada HTTP real.
 */
class ResumoServiceExtracaoGeminiTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ResumoService resumoService = new ResumoService(new RestTemplate(), new GeminiProperties());

    @Test
    void deveConcatenarTextoQuandoResponseTemMultiplasParts() {
        String json = """
                {
                  "response": {
                    "candidates": [
                      {
                        "content": {
                          "parts": [
                            { "text": "primeiro fragmento do texto " },
                            { "text": "segundo fragmento do texto" }
                          ]
                        }
                      }
                    ]
                  }
                }
                """;

        GeminiResponse resposta = objectMapper.readValue(json, GeminiResponse.class);

        String textoGerado = resumoService.extrairTextoGerado(resposta);

        assertEquals("primeiro fragmento do texto segundo fragmento do texto", textoGerado);
    }

    @Test
    void deveRetornarTextoQuandoResponseTemUmaUnicaPart() {
        String json = """
                {
                  "response": {
                    "candidates": [
                      {
                        "content": {
                          "parts": [
                            { "text": "texto unico gerado" }
                          ]
                        }
                      }
                    ]
                  }
                }
                """;

        GeminiResponse resposta = objectMapper.readValue(json, GeminiResponse.class);

        String textoGerado = resumoService.extrairTextoGerado(resposta);

        assertEquals("texto unico gerado", textoGerado);
    }

    @Test
    void deveLancarErroQuandoResponseEstaAusente() {
        // Simula o shape real da API do Gemini, sem o wrapper "response" —
        // "candidates" fica na raiz e é ignorado (ignoreUnknown), então
        // resposta.getResponse() vem null.
        String json = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "text": "texto que nunca deveria ser lido" }
                        ]
                      }
                    }
                  ]
                }
                """;

        GeminiResponse resposta = objectMapper.readValue(json, GeminiResponse.class);

        assertThrows(ProvedorIndisponivelException.class,
                () -> resumoService.extrairTextoGerado(resposta));
    }

    @Test
    void deveLancarErroQuandoPartsEstaVazia() {
        String json = """
                {
                  "response": {
                    "candidates": [
                      {
                        "content": {
                          "parts": []
                        }
                      }
                    ]
                  }
                }
                """;

        GeminiResponse resposta = objectMapper.readValue(json, GeminiResponse.class);

        assertThrows(ProvedorIndisponivelException.class,
                () -> resumoService.extrairTextoGerado(resposta));
    }

    @Test
    void deveLancarErroQuandoTextoConcatenadoEstaEmBranco() {
        String json = """
                {
                  "response": {
                    "candidates": [
                      {
                        "content": {
                          "parts": [
                            { "text": "   " }
                          ]
                        }
                      }
                    ]
                  }
                }
                """;

        GeminiResponse resposta = objectMapper.readValue(json, GeminiResponse.class);

        assertThrows(ProvedorIndisponivelException.class,
                () -> resumoService.extrairTextoGerado(resposta));
    }
}
