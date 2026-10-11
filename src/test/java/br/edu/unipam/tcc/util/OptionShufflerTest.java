package br.edu.unipam.tcc.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class OptionShufflerTest {

    private static final List<String> FOUR = List.of(
            "A) Estatina", "B) Fibrato", "C) Ômega-3", "D) Niacina");
    private static final List<String> FIVE = List.of(
            "A) Estatina", "B) Fibrato", "C) Ômega-3", "D) Niacina", "E) Ezetimiba");

    private static List<String> texts(List<String> labeled) {
        return labeled.stream().map(option -> option.substring(option.indexOf(')') + 1).trim()).toList();
    }

    @Test
    @DisplayName("Deve devolver uma permutação das alternativas, sem perder nem duplicar nenhuma")
    void shouldReturnPermutationWithoutLosingOptions() {
        OptionShuffler.ShuffledOptions shuffled = new OptionShuffler(new Random(7)).shuffle(FIVE, "C", null);

        assertThat(texts(shuffled.options())).containsExactlyInAnyOrderElementsOf(texts(FIVE));
        assertThat(shuffled.order()).containsExactlyInAnyOrder(0, 1, 2, 3, 4);
    }

    @Test
    @DisplayName("Deve re-rotular as alternativas exibidas de A em diante, na ordem")
    void shouldRelabelDisplayedOptionsInOrder() {
        OptionShuffler.ShuffledOptions shuffled = new OptionShuffler(new Random(3)).shuffle(FIVE, "A", null);

        List<String> labels = shuffled.options().stream().map(option -> option.substring(0, 3)).toList();
        assertThat(labels).containsExactly("A) ", "B) ", "C) ", "D) ", "E) ");
    }

    @Test
    @DisplayName("Deve mapear o gabarito para a letra em que a alternativa correta foi exibida")
    void shouldMapAnswerToDisplayedLetter() {
        for (int seed = 0; seed < 30; seed++) {
            OptionShuffler.ShuffledOptions shuffled = new OptionShuffler(new Random(seed)).shuffle(FOUR, "B", null);

            String expectedLetter = String.valueOf((char) ('A' + shuffled.correctPosition()));
            assertThat(shuffled.correctLetter()).isEqualTo(expectedLetter);
            assertThat(shuffled.correctOption()).isEqualTo(expectedLetter + ") Fibrato");
            assertThat(shuffled.order().get(shuffled.correctPosition())).isEqualTo(1);
        }
    }

    static Stream<Arguments> everyLastPosition() {
        return Stream.of(FOUR, FIVE).flatMap(options -> IntStream.range(0, options.size())
                .boxed()
                .flatMap(last -> Stream.of("A", "C", "D").map(answer -> Arguments.of(options, answer, last))));
    }

    @ParameterizedTest(name = "{1} correta, {0} alternativas, última posição {2}")
    @MethodSource("everyLastPosition")
    @DisplayName("A posição da correta deve mudar em relação à última apresentação, com 4 e 5 alternativas")
    void correctPositionMustDifferFromLastOne(List<String> options, String answer, int lastPosition) {
        for (int seed = 0; seed < 50; seed++) {
            OptionShuffler.ShuffledOptions shuffled =
                    new OptionShuffler(new Random(seed)).shuffle(options, answer, lastPosition);

            assertThat(shuffled.correctPosition()).isNotEqualTo(lastPosition);
            assertThat(texts(shuffled.options())).containsExactlyInAnyOrderElementsOf(texts(options));
        }
    }

    @Test
    @DisplayName("Sem última posição, a correta pode cair em qualquer letra")
    void anyPositionIsAllowedWithoutLastPosition() {
        // Uma instância semeada usada várias vezes, como o bean em produção. Sementes vizinhas em
        // instâncias novas geram o primeiro sorteio quase igual e escondem posições.
        OptionShuffler shuffler = new OptionShuffler(new Random(42));
        Set<Integer> positions = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            positions.add(shuffler.shuffle(FOUR, "A", null).correctPosition());
        }

        assertThat(positions).containsExactlyInAnyOrder(0, 1, 2, 3);
    }

    @Test
    @DisplayName("Deve reconstruir exatamente a mesma visão a partir da ordem salva")
    void shouldRebuildSameViewFromSavedOrder() {
        OptionShuffler shuffler = new OptionShuffler(new Random(11));
        OptionShuffler.ShuffledOptions shown = shuffler.shuffle(FIVE, "D", 2);

        OptionShuffler.ShuffledOptions rebuilt = shuffler.apply(FIVE, "D", new ArrayList<>(shown.order()));

        assertThat(rebuilt).isEqualTo(shown);
    }

    @Test
    @DisplayName("Ordem nula ou inválida deve cair na ordem original")
    void invalidSavedOrderFallsBackToOriginal() {
        OptionShuffler shuffler = new OptionShuffler(new Random(1));

        List<List<Integer>> invalidOrders = java.util.Arrays.asList(null, List.of(), List.of(0, 1, 2),
                List.of(0, 0, 1, 2), List.of(0, 1, 2, 9));
        for (List<Integer> order : invalidOrders) {
            OptionShuffler.ShuffledOptions view = shuffler.apply(FOUR, "C", order);

            assertThat(view.options()).containsExactlyElementsOf(FOUR);
            assertThat(view.correctLetter()).isEqualTo("C");
            assertThat(view.correctPosition()).isEqualTo(2);
            assertThat(view.order()).containsExactly(0, 1, 2, 3);
        }
    }

    @Test
    @DisplayName("Gabarito fora das alternativas ou menos de duas alternativas: mantém a ordem original")
    void keepsOriginalOrderWhenItCannotShuffleSafely() {
        OptionShuffler shuffler = new OptionShuffler(new Random(5));

        OptionShuffler.ShuffledOptions unknownAnswer = shuffler.shuffle(FOUR, "Z", 0);
        assertThat(unknownAnswer.options()).containsExactlyElementsOf(FOUR);
        assertThat(unknownAnswer.correctLetter()).isEqualTo("Z");
        assertThat(unknownAnswer.correctPosition()).isEqualTo(-1);

        OptionShuffler.ShuffledOptions single = shuffler.shuffle(List.of("A) Única"), "A", 0);
        assertThat(single.options()).containsExactly("A) Única");
        assertThat(single.correctLetter()).isEqualTo("A");
    }

    @Test
    @DisplayName("Deve ler as alternativas do JSON da coluna options_json")
    void shouldParseOptionsJson() {
        assertThat(OptionShuffler.parseOptions("[\"A) Estatina\", \"B) Fibrato\"]"))
                .containsExactly("A) Estatina", "B) Fibrato");
        assertThat(OptionShuffler.parseOptions(null)).isEmpty();
        assertThat(OptionShuffler.parseOptions("  ")).isEmpty();
        assertThat(OptionShuffler.parseOptions("não é json")).isEmpty();
        assertThat(OptionShuffler.parseOptions("{\"a\": 1}")).isEmpty();
    }

    @Test
    @DisplayName("A visão exibida deve virar o JSON de alternativas na nova ordem")
    void displayedViewSerializesToJsonArray() {
        OptionShuffler.ShuffledOptions shuffled = new OptionShuffler(new Random(9)).shuffle(FOUR, "A", 0);

        assertThat(OptionShuffler.parseOptions(shuffled.optionsJson())).containsExactlyElementsOf(shuffled.options());
    }
}
