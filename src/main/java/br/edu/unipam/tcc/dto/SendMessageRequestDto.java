package br.edu.unipam.tcc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload de requisição para envio de mensagem de texto via API REST da Uazapi.
 *
 * @param number   Número de destino (ex: "5534999998888").
 * @param text     Conteúdo do texto a ser enviado.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SendMessageRequestDto(
        @JsonProperty("number") String number,
        @JsonProperty("text") String text
) {
}
