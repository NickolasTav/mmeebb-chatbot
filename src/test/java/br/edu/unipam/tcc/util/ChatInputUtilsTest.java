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

    @Test
    @DisplayName("Deve numerar a lista em negrito no padrão do WhatsApp")
    void deveNumerarListaEmNegrito() {
        assertEquals("*1* - Medicina\n*2* - Sistemas\n",
                ChatInputUtils.numberedList(List.of("Medicina", "Sistemas")));
    }
}
