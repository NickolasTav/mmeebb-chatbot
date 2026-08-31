package br.edu.unipam.tcc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Evento enfileirado na fila de saída (whatsapp.outgoing.queue) para despacho assíncrono com anti-ban.
 *
 * @param phoneNumber   Número de telefone de destino (normalizado).
 * @param messageText   Texto da mensagem a ser enviada.
 * @param typingDelayMs Tempo em milissegundos para simular presença 'composing'.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OutgoingMessageDto(
        @JsonProperty("phoneNumber") String phoneNumber,
        @JsonProperty("messageText") String messageText,
        @JsonProperty("typingDelayMs") Long typingDelayMs
) {
    public OutgoingMessageDto(String phoneNumber, String messageText) {
        this(phoneNumber, messageText, null);
    }
}
