package br.edu.unipam.tcc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload de requisição para envio de estado de presença (digitação, gravação, pausa) via Uazapi.
 *
 * @param number    Número de destino.
 * @param presence  Estado de presença (ex: "composing", "paused", "recording").
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SendPresenceRequestDto(
        @JsonProperty("number") String number,
        @JsonProperty("presence") String presence
) {
}
