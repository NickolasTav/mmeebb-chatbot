package br.edu.unipam.tcc.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChatInputUtilsTest {

    @Test
    @DisplayName("Deve interpretar apenas inteiros positivos")
    void deveInterpretarApenasInteirosPositivos() {
        assertEquals(3, ChatInputUtils.parsePositiveInt(" 3 "));
        assertNull(ChatInputUtils.parsePositiveInt("0"));
        assertNull(ChatInputUtils.parsePositiveInt("-1"));
        assertNull(ChatInputUtils.parsePositiveInt("abc"));
        assertNull(ChatInputUtils.parsePositiveInt(null));
    }

    @Test
    @DisplayName("Deve selecionar opção pela posição e recusar número fora da lista")
    void deveSelecionarOpcaoPelaPosicao() {
        List<String> options = List.of("Medicina", "Sistemas");

        assertEquals(Optional.of("Sistemas"), ChatInputUtils.parseSelection("2", options));
        assertEquals(Optional.empty(), ChatInputUtils.parseSelection("3", options));
    }

    @org.junit.jupiter.params.ParameterizedTest(name = "\"{0}\" -> {1}")
    @DisplayName("Deve extrair a letra escolhida: letra solta, 'letra B', alternativa colada")
    @org.junit.jupiter.params.provider.CsvSource(delimiter = '|', value = {
            "B | B",
            "b | B",
            "B) | B",
            "letra c | C",
            "Alternativa D | D",
            "opção a | A",
            "A) Estatina | A",
            "  e  | E"
    })
    void deveExtrairLetraEscolhida(String answer, String letter) {
        assertEquals(Optional.of(letter), ChatInputUtils.choiceLetter(answer));
    }

    @org.junit.jupiter.params.ParameterizedTest(name = "\"{0}\"")
    @DisplayName("Não deve ver letra em texto livre")
    @org.junit.jupiter.params.provider.ValueSource(strings = {
            "não sei", "estatina", "acho que é fibrato", "", "   "
    })
    void naoDeveVerLetraEmTextoLivre(String answer) {
        assertEquals(Optional.empty(), ChatInputUtils.choiceLetter(answer));
    }

    @Test
    @DisplayName("Resposta nula não tem letra")
    void respostaNulaNaoTemLetra() {
        assertEquals(Optional.empty(), ChatInputUtils.choiceLetter(null));
    }

    @Test
    @DisplayName("Deve numerar a lista em negrito no padrão do WhatsApp")
    void deveNumerarListaEmNegrito() {
        assertEquals("*1* - Medicina\n*2* - Sistemas\n",
                ChatInputUtils.numberedList(List.of("Medicina", "Sistemas")));
    }
}
