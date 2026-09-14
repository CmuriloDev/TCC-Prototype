package com.adapter.prototype.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurações tipadas de acesso à API do Groq.
 *
 * <p>Os valores são lidos do prefixo {@code groq.api} em
 * {@code application.properties}. A chave de API vem da variável de ambiente
 * {@code GROQ_API_KEY} e pode ficar em branco enquanto a integração real
 * não é implementada — a aplicação continua subindo normalmente.
 */
@ConfigurationProperties(prefix = "groq.api")
public class GroqProperties {

    /** Chave de API do Groq (variável de ambiente {@code GROQ_API_KEY}). */
    private String key;

    /** URL completa do endpoint de chat completions da API do Groq. */
    private String url;

    /** Nome do modelo do Groq usado na geração (ex.: {@code llama-3.3-70b-versatile}). */
    private String model;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}
