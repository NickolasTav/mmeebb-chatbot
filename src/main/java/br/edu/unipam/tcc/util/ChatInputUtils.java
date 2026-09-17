package br.edu.unipam.tcc.util;

import java.util.List;
import java.util.Optional;

/**
 * Leitura das respostas numéricas do WhatsApp e formatação de listas de opções,
 * compartilhadas pelo cadastro, pelo menu principal e pelas Configurações.
 */
public final class ChatInputUtils {

    public static final int MAX_ACADEMIC_PERIOD = 20;

    private ChatInputUtils() {
    }

    public static Integer parsePositiveInt(String input) {
        if (input == null) {
            return null;
        }
        try {
            int value = Integer.parseInt(input.trim());
            return value >= 1 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static <T> Optional<T> parseSelection(String input, List<T> options) {
        Integer index = parsePositiveInt(input);
        if (index == null || index > options.size()) {
            return Optional.empty();
        }
        return Optional.of(options.get(index - 1));
    }

    public static String numberedList(List<String> labels) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < labels.size(); i++) {
            sb.append("*").append(i + 1).append("* - ").append(labels.get(i)).append("\n");
        }
        return sb.toString();
    }
}
