package br.edu.unipam.tcc.service;

/**
 * Contrato de serviço para integração com a API REST da Uazapi (WhatsApp).
 */
public interface UazapiClientService {

    /**
     * Envia uma mensagem de texto para o número especificado.
     *
     * @param phoneNumber Número de telefone com DDI e DDD (ex: "5534999998888").
     * @param message     Conteúdo do texto a ser enviado.
     */
    void sendTextMessage(String phoneNumber, String message);

    /**
     * Envia o estado de presença (ex: "composing", "paused", "recording") para o chat especificado.
     *
     * @param phoneNumber Número de telefone com DDI e DDD.
     * @param presence    Tipo de presença ("composing", "paused", "recording").
     */
    void sendPresence(String phoneNumber, String presence);
}
