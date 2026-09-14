package com.adapter.prototype.exception;

/**
 * Representa falhas na comunicação com o provedor de IA (Groq): timeout,
 * indisponibilidade, erro retornado pela API do provedor ou resposta em
 * formato inesperado.
 */
public class ProvedorIndisponivelException extends RuntimeException {

    public ProvedorIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public ProvedorIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
