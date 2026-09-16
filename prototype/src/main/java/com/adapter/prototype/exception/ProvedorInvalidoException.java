package com.adapter.prototype.exception;

/**
 * Representa o envio de um valor não reconhecido no campo "provedor" do
 * {@link com.adapter.prototype.dto.ResumoRequest}.
 */
public class ProvedorInvalidoException extends RuntimeException {

    public ProvedorInvalidoException(String mensagem) {
        super(mensagem);
    }
}
