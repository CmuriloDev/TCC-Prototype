package com.adapter.prototype.provider;

/**
 * Falha de comunicação com um provedor de IA (Adaptee): timeout,
 * indisponibilidade, erro retornado pela API ou resposta em formato
 * inesperado. Neutra em relação a qual provedor específico a lançou.
 */
public class ProvedorIndisponivelException extends RuntimeException {

    public ProvedorIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public ProvedorIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
