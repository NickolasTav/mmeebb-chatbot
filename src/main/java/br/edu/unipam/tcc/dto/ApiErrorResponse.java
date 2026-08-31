package br.edu.unipam.tcc.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO padronizado para representação de erros e exceções retornados pela API REST.
 *
 * @param timestamp Data e hora da ocorrência do erro.
 * @param status    Código HTTP de status (ex: 400, 404, 422, 500).
 * @param error     Descrição curta do tipo de erro HTTP.
 * @param message   Mensagem amigável descrevendo a causa da falha.
 * @param path      URI do recurso / endpoint que originou o erro.
 * @param details   Lista opcional de detalhes ou erros de validação por campo.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details
) {
    public ApiErrorResponse(int status, String error, String message, String path) {
        this(LocalDateTime.now(), status, error, message, path, null);
    }

    public ApiErrorResponse(int status, String error, String message, String path, List<String> details) {
        this(LocalDateTime.now(), status, error, message, path, details);
    }
}
