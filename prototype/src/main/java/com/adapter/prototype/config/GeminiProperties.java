package com.adapter.prototype.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurações tipadas de acesso à API do Gemini (Google).
 *
 * <p>Os valores são lidos do prefixo {@code gemini.api} em
 * {@code application.properties}. A chave de API vem da variável de ambiente
 * {@code GEMINI_API_KEY} e pode ficar em branco enquanto a integração real
 * não é implementada — a aplicação continua subindo normalmente.
 */
@ConfigurationProperties(prefix = "gemini.api")
public class GeminiProperties {

    /** Chave de API do Gemini (variável de ambiente {@code GEMINI_API_KEY}). */
    private String key;

    /** URL base dos endpoints de modelos da API do Gemini. */
    private String url;

    /** Nome do modelo do Gemini usado na geração (ex.: {@code gemini-2.5-flash}). */
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
