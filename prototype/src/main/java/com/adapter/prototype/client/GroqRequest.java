package com.adapter.prototype.client;

import java.util.List;

/**
 * Estrutura do corpo enviado ao endpoint {@code /chat/completions} da API do
 * Groq (compatível com o padrão OpenAI). Serve apenas para serializar o JSON
 * da requisição — não é uma camada de abstração.
 */
public class GroqRequest {

    private String model;
    private List<Message> messages;

    public GroqRequest() {
    }

    /**
     * Monta o corpo com o modelo informado e uma única mensagem de papel
     * "user" contendo o prompt.
     */
    public GroqRequest(String model, String texto) {
        Message message = new Message();
        message.setRole("user");
        message.setContent(texto);

        this.model = model;
        this.messages = List.of(message);
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public static class Message {

        private String role;
        private String content;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
