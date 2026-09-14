package com.adapter.prototype.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Estrutura da resposta do endpoint {@code /chat/completions} da API do Groq
 * (compatível com o padrão OpenAI). Serve apenas para desserializar o JSON —
 * campos não mapeados são ignorados.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GroqResponse {

    private List<Choice> choices;

    public List<Choice> getChoices() {
        return choices;
    }

    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {

        private Message message;

        public Message getMessage() {
            return message;
        }

        public void setMessage(Message message) {
            this.message = message;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {

        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
