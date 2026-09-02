package com.adapter.prototype.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Configuração base para o acesso à API do Gemini (Google).
 *
 * <p>Por enquanto apenas expõe o cliente HTTP e as propriedades tipadas. A
 * chamada real à API será implementada na próxima etapa, reaproveitando o
 * {@link RestClient} declarado aqui.
 */
@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfig {

    /**
     * Cliente HTTP usado para falar com a API do Gemini, já apontado para a
     * URL base configurada em {@code gemini.api.url}.
     */
    @Bean
    public RestClient geminiRestClient(GeminiProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.getUrl())
                .build();
    }
}
