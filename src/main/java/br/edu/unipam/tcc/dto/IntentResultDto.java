package br.edu.unipam.tcc.dto;

import br.edu.unipam.tcc.entity.enums.ChatIntent;

/**
 * Resultado da classificação de intenção de uma mensagem em texto livre.
 *
 * @param intent      Intenção detectada.
 * @param subjectHint Nome da disciplina citada pelo estudante, ou {@code null} se não houver.
 */
public record IntentResultDto(ChatIntent intent, String subjectHint) {

    public static IntentResultDto of(ChatIntent intent) {
        return new IntentResultDto(intent, null);
    }
}
