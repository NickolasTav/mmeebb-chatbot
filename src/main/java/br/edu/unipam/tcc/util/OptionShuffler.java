package br.edu.unipam.tcc.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Embaralha as alternativas de uma questão de múltipla escolha a cada apresentação (D11).
 *
 * <p>A regra é que a alternativa correta nunca apareça na mesma posição em que o aluno a viu da
 * última vez. Sem isso, quem decora "a resposta desta questão é a B" acerta sem saber o conteúdo,
 * e o MMEEBB passa a medir memória de letra em vez de memória do conceito.
 *
 * <p>Não tem estado: a ordem sorteada volta para quem chamou, que a guarda (sessão Redis) para
 * corrigir e reenviar a questão exatamente como foi mostrada.
 */
@Component
public class OptionShuffler {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern LABEL = Pattern.compile("^\\s*([A-Za-z])\\)\\s*(.*)$", Pattern.DOTALL);

    private final Random random;

    public OptionShuffler() {
        this(new Random());
    }

    public OptionShuffler(Random random) {
        this.random = random;
    }

    /**
     * Visão exibida de uma questão.
     *
     * @param options         Alternativas re-rotuladas ("A) texto") na ordem exibida.
     * @param correctLetter   Letra em que a alternativa correta foi exibida.
     * @param correctPosition Índice (0 = A) da correta na ordem exibida; -1 se o gabarito não está entre as alternativas.
     * @param order           Para cada posição exibida, o índice da alternativa original.
     */
    public record ShuffledOptions(List<String> options, String correctLetter, int correctPosition, List<Integer> order) {

        public String correctOption() {
            return correctPosition >= 0 ? options.get(correctPosition) : null;
        }

        public String optionsJson() {
            try {
                return JSON.writeValueAsString(options);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Falha ao serializar as alternativas exibidas.", e);
            }
        }
    }

    /**
     * Sorteia uma ordem nova em que a correta não ocupa {@code lastCorrectPosition}.
     *
     * @param options             Alternativas como estão no banco ("A) texto").
     * @param correctLetter       Gabarito no banco (letra).
     * @param lastCorrectPosition Posição (0 = A) em que a correta foi mostrada da última vez; nula se nunca foi.
     */
    public ShuffledOptions shuffle(List<String> options, String correctLetter, Integer lastCorrectPosition) {
        int correctIndex = indexOfLetter(options, correctLetter);
        if (options == null || options.size() < 2 || correctIndex < 0) {
            return identity(options, correctLetter);
        }

        List<Integer> order = new ArrayList<>(IntStream.range(0, options.size()).boxed().toList());
        Collections.shuffle(order, random);

        if (lastCorrectPosition != null && order.indexOf(correctIndex) == lastCorrectPosition) {
            // Troca a correta com outra posição sorteada entre as que não são a última.
            int from = lastCorrectPosition;
            int to = random.nextInt(options.size() - 1);
            if (to >= from) {
                to++;
            }
            Collections.swap(order, from, to);
        }

        return build(options, correctIndex, order);
    }

    /**
     * Reconstrói a visão a partir de uma ordem já mostrada (reenvio depois de dúvida, correção).
     * Ordem ausente ou que não é permutação das alternativas cai na ordem original.
     */
    public ShuffledOptions apply(List<String> options, String correctLetter, List<Integer> order) {
        int correctIndex = indexOfLetter(options, correctLetter);
        if (options == null || correctIndex < 0 || !isPermutation(order, options.size())) {
            return identity(options, correctLetter);
        }
        return build(options, correctIndex, order);
    }

    /**
     * Lê o {@code options_json} da questão. Nulo, vazio ou fora do formato de array vira lista vazia,
     * e a questão é tratada como dissertativa na exibição.
     */
    public static List<String> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = JSON.readTree(optionsJson);
            if (node == null || !node.isArray()) {
                return List.of();
            }
            List<String> options = new ArrayList<>();
            node.forEach(item -> options.add(item.asText()));
            return List.copyOf(options);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private ShuffledOptions build(List<String> options, int correctIndex, List<Integer> order) {
        List<String> displayed = new ArrayList<>(options.size());
        for (int position = 0; position < order.size(); position++) {
            displayed.add(letterAt(position) + ") " + textOf(options.get(order.get(position))));
        }
        int correctPosition = order.indexOf(correctIndex);
        return new ShuffledOptions(List.copyOf(displayed), letterAt(correctPosition), correctPosition, List.copyOf(order));
    }

    private ShuffledOptions identity(List<String> options, String correctLetter) {
        List<String> original = options == null ? List.of() : List.copyOf(options);
        int correctIndex = indexOfLetter(original, correctLetter);
        List<Integer> order = IntStream.range(0, original.size()).boxed().toList();
        String letter = correctIndex >= 0 ? letterAt(correctIndex) : correctLetter;
        return new ShuffledOptions(original, letter, correctIndex, order);
    }

    private static boolean isPermutation(List<Integer> order, int size) {
        if (order == null || order.size() != size) {
            return false;
        }
        boolean[] seen = new boolean[size];
        for (Integer index : order) {
            if (index == null || index < 0 || index >= size || seen[index]) {
                return false;
            }
            seen[index] = true;
        }
        return true;
    }

    private static int indexOfLetter(List<String> options, String letter) {
        if (options == null || letter == null || letter.isBlank()) {
            return -1;
        }
        String wanted = letter.trim().toUpperCase();
        for (int i = 0; i < options.size(); i++) {
            Matcher matcher = LABEL.matcher(options.get(i));
            if (matcher.matches() && matcher.group(1).toUpperCase().equals(wanted)) {
                return i;
            }
        }
        return -1;
    }

    private static String textOf(String option) {
        Matcher matcher = LABEL.matcher(option);
        return matcher.matches() ? matcher.group(2).trim() : option.trim();
    }

    private static String letterAt(int position) {
        return String.valueOf((char) ('A' + position));
    }
}
