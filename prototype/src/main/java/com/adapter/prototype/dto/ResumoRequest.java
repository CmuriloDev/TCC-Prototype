package com.adapter.prototype.dto;

public class ResumoRequest {

    private String texto;

    /** Provedor de IA a ser usado ("gemini" ou "groq"). Padrão: "gemini". */
    private String provedor;

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getProvedor() {
        return provedor;
    }

    public void setProvedor(String provedor) {
        this.provedor = provedor;
    }
}
