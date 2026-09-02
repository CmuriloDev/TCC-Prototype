package com.adapter.prototype.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuração base para o acesso à API do Gemini (Google).
 *
 * <p>Expõe o cliente HTTP e as propriedades tipadas usadas pela chamada direta
 * feita dentro do {@code ResumoService}.
 */
@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfig {

    /**
     * Cliente HTTP usado para falar diretamente com a API do Gemini.
     */
    @Bean
    public RestTemplate geminiRestTemplate() {
        return new RestTemplate();
    }
}
