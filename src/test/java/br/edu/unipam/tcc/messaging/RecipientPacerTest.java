package br.edu.unipam.tcc.messaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Horários do ritmo por destinatário. Tempos em ms; composing de 1,5 s como em produção.
 */
class RecipientPacerTest {

    private static final long COMPOSING = 1_500;
    private static final long AGORA = 10_000;

    @Test
    @DisplayName("1ª mensagem: composing depois do jitter e texto depois do composing")
    void primeiraMensagemEsperaJitterMaisComposing() {
        RecipientPacer pacer = new RecipientPacer(COMPOSING, 1_000);

        RecipientPacer.Slot slot = pacer.reserve("5534000000001", AGORA, 3_000);

        assertEquals(13_000, slot.composingAtMs());
        assertEquals(14_500, slot.textAtMs());
    }

    @Test
    @DisplayName("Mesmo telefone em rajada: o jitter conta a partir do texto anterior, como na fila única")
    void mesmoTelefoneContaJitterDoTextoAnterior() {
        RecipientPacer pacer = new RecipientPacer(COMPOSING, 1_000);

        RecipientPacer.Slot primeira = pacer.reserve("5534000000001", AGORA, 3_000);
        RecipientPacer.Slot segunda = pacer.reserve("5534000000001", AGORA, 4_000);

        assertEquals(14_500, primeira.textAtMs());
        assertEquals(14_500 + 4_000, segunda.composingAtMs());
        assertEquals(14_500 + 4_000 + COMPOSING, segunda.textAtMs());
    }

    @Test
    @DisplayName("Mesmo telefone depois de uma pausa longa: o jitter conta a partir da chegada")
    void mesmoTelefoneDepoisDePausaContaDaChegada() {
        RecipientPacer pacer = new RecipientPacer(COMPOSING, 1_000);

        pacer.reserve("5534000000001", AGORA, 3_000);
        RecipientPacer.Slot depois = pacer.reserve("5534000000001", 60_000, 3_000);

        assertEquals(64_500, depois.textAtMs());
    }

    @Test
    @DisplayName("Telefones diferentes não somam jitter: sem teto, todos saem no mesmo horário")
    void telefonesDiferentesNaoSomamJitter() {
        RecipientPacer pacer = new RecipientPacer(COMPOSING, 0);

        for (int i = 0; i < 10; i++) {
            RecipientPacer.Slot slot = pacer.reserve("553400000000" + i, AGORA, 3_000);
            assertEquals(14_500, slot.textAtMs(), "aluno " + i);
        }
    }

    @Test
    @DisplayName("Teto global sob rajada: 10 alunos ao mesmo tempo saem espaçados pelo teto, nem mais nem menos")
    void tetoGlobalEspacaRajada() {
        RecipientPacer pacer = new RecipientPacer(COMPOSING, 1_000);

        List<Long> textos = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            RecipientPacer.Slot slot = pacer.reserve("553400000000" + i, AGORA, 3_000);
            assertEquals(slot.textAtMs() - COMPOSING, slot.composingAtMs());
            textos.add(slot.textAtMs());
        }

        for (int i = 0; i < 10; i++) {
            assertEquals(14_500 + i * 1_000L, textos.get(i));
        }
    }

    @Test
    @DisplayName("Aluno com várias mensagens na fila não empurra os outros para trás dele")
    void alunoComFilaNaoAtrasaOsOutros() {
        RecipientPacer pacer = new RecipientPacer(COMPOSING, 1_000);
        pacer.reserve("A", AGORA, 3_000);   // texto em 14.500
        pacer.reserve("A", AGORA, 3_000);   // 19.000
        pacer.reserve("A", AGORA, 3_000);   // 23.500

        RecipientPacer.Slot b = pacer.reserve("B", AGORA, 3_000);

        // 14.500 está ocupado; o 1º horário a >= 1 s de todo texto reservado é 15.500 (e não 24.500)
        assertEquals(15_500, b.textAtMs());
    }

    @Test
    @DisplayName("Buraco entre textos reservados só é usado se couber a >= teto dos dois lados")
    void buracoRespeitaTetoDosDoisLados() {
        RecipientPacer pacer = new RecipientPacer(COMPOSING, 1_000);
        assertEquals(17_500, pacer.reserve("A", AGORA, 6_000).textAtMs());
        assertEquals(14_500, pacer.reserve("B", AGORA, 3_000).textAtMs()); // antes de A, com folga
        assertEquals(16_000, pacer.reserve("C", AGORA, 4_500).textAtMs()); // 1,5 s de B e de A

        // desejado 16.500: colide com C (16.000) → 17.000; colide com A (17.500) → 18.500
        assertEquals(18_500, pacer.reserve("D", AGORA, 5_000).textAtMs());
    }
}
