package com.adapter.prototype.provider;

/**
 * Target do padrão Adapter: contrato único que o Client (ResumoService) usa
 * para pedir um resumo, sem conhecer os detalhes de comunicação do provedor
 * (Adaptee) por trás de cada implementação concreta.
 */
public interface AiSummarizerClient {

    String gerarResumo(String texto) throws ProvedorIndisponivelException;
}
