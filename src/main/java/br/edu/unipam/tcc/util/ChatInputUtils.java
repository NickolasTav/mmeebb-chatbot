package br.edu.unipam.tcc.util;

import java.text.Normalizer;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Leitura das respostas do WhatsApp (números e letra de alternativa) e formatação de listas de opções,
 * compartilhadas pelo cadastro, pelo menu principal e pelas Configurações.
 */
public final class ChatInputUtils {

    public static final int MAX_ACADEMIC_PERIOD = 20;

    /** A mensagem inteira é a letra, com ou sem "letra"/"opção"/"alternativa" e pontuação ("B", "b)", "letra c"). */
    private static final Pattern ONLY_LETTER = Pattern.compile("^(?:(?:letra|opcao|alternativa)\\s*)?([a-z])$");

    /** "letra B porque..." / "alternativa C, pois...": o prefixo deixa claro que é a escolha. */
    private static final Pattern PREFIXED_LETTER = Pattern.compile("^(?:letra|opcao|alternativa)\\s+([a-z])(?:\\s|$)");

    /** Alternativa colada como aparece na tela: "A) Estatina", "(D) Furosemida". */
    private static final Pattern PASTED_OPTION = Pattern.compile("^\\s*\\(?([A-Za-z])\\)\\s*\\S");

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

    /**
     * Letra que o estudante escolheu numa questão de múltipla escolha: letra solta, "letra B",
     * "alternativa C" ou a alternativa colada inteira ("A) Estatina"). Texto livre não tem letra.
     * É a mesma leitura usada na correção, então o "Você marcou X" do feedback diz exatamente a
     * letra que foi corrigida.
     *
     * <p>Frase que só começa com uma palavra de uma letra ("é a C?", "a resposta é B", "o que é FEVE?")
     * não é escolha: segue para a avaliação semântica, que distingue dúvida de resposta (bug 2.7).
     */
    public static Optional<String> choiceLetter(String answer) {
        if (answer == null) {
            return Optional.empty();
        }
        String normalized = normalize(answer);
        for (Matcher matcher : List.of(ONLY_LETTER.matcher(normalized), PREFIXED_LETTER.matcher(normalized),
                PASTED_OPTION.matcher(answer))) {
            if (matcher.find()) {
                return Optional.of(matcher.group(1).toUpperCase());
            }
        }
        return Optional.empty();
    }

    /** Minúsculas, sem acento e só com letras, dígitos e espaço. */
    public static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", "")
                .trim();
    }

    public static String numberedList(List<String> labels) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < labels.size(); i++) {
            sb.append("*").append(i + 1).append("* - ").append(labels.get(i)).append("\n");
        }
        return sb.toString();
    }
}
