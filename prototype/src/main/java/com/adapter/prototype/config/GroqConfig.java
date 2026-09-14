package com.adapter.prototype.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Configuração base para o acesso à API do Groq.
 *
 * <p>Expõe o cliente HTTP e as propriedades tipadas usadas pela chamada direta
 * feita dentro do {@code ResumoService}.
 */
@Configuration
@EnableConfigurationProperties(GroqProperties.class)
public class GroqConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

    /**
     * Cliente HTTP usado para falar diretamente com a API do Groq, com
     * timeouts explícitos para evitar que a aplicação fique travada
     * indefinidamente caso o Groq não responda.
     */
    @Bean
    public RestTemplate groqRestTemplate() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);

        return new RestTemplate(requestFactory);
    }
}
