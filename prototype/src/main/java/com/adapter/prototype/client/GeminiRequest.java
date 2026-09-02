package com.adapter.prototype.client;

import java.util.List;

/**
 * Estrutura do corpo enviado ao endpoint {@code :generateContent} da API do
 * Gemini. Serve apenas para serializar o JSON da requisição — não é uma camada
 * de abstração.
 */
public class GeminiRequest {

    private List<Content> contents;

    public GeminiRequest() {
    }

    /**
     * Monta o corpo mínimo com um único bloco de texto.
     */
    public GeminiRequest(String texto) {
        Part part = new Part();
        part.setText(texto);

        Content content = new Content();
        content.setParts(List.of(part));

        this.contents = List.of(content);
    }

    public List<Content> getContents() {
        return contents;
    }

    public void setContents(List<Content> contents) {
        this.contents = contents;
    }

    public static class Content {

        private List<Part> parts;

        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }

    public static class Part {

        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
