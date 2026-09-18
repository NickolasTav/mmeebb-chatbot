package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.enums.ScheduleStatus;
import br.edu.unipam.tcc.entity.enums.TopicMastery;
import br.edu.unipam.tcc.observability.MmeebbMetrics;
import br.edu.unipam.tcc.service.impl.MmeebbServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MmeebbServiceImplTest {

    private MmeebbService mmeebbService;
    private Student student;
    private Flashcard flashcard;

    @BeforeEach
    void setUp() {
        mmeebbService = new MmeebbServiceImpl(org.mockito.Mockito.mock(MmeebbMetrics.class));
        student = Student.builder()
                .id(UUID.randomUUID())
                .phoneNumber("5534999998888")
                .fullName("Estudante Teste")
                .build();
        flashcard = Flashcard.builder()
                .id(1L)
                .topic("Cardiologia")
                .question("O que é IAM?")
                .answer("Infarto Agudo do Miocárdio")
                .build();
    }

    @Test
    @DisplayName("Smoke Test: Deve calcular intervalo inicial de 1 dia quando N = 0 (2^0 = 1)")
    void deveCalcularIntervaloInicialDeUmDiaQuandoNZero() {
        int interval = mmeebbService.calculateIntervalDays(0);
        assertEquals(1, interval);
    }

    @ParameterizedTest(name = "N = {0} deve resultar em intervalo de {1} dias (2^{0})")
    @CsvSource({
            "0, 1",
            "1, 2",
            "2, 4",
            "3, 8",
            "4, 16",
            "5, 32",
            "6, 64",
            "7, 128",
            "8, 256",
            "9, 512",
            "10, 1024",
            "11, 2048",
            "12, 4096",
            "13, 8192"
    })
    @DisplayName("Deve calcular corretamente a progressão exponencial 2^N para N de 0 a 13")
    void deveCalcularIntervalosExponenciaisDeZeroATrezeCorretamente(int nIndex, int expectedInterval) {
        int actualInterval = mmeebbService.calculateIntervalDays(nIndex);
        assertEquals(expectedInterval, actualInterval);
    }

    @Test
    @DisplayName("Deve saturar no teto de 13 (8192 dias) quando N for maior que 13")
    void deveSaturarEmTrezeQuandoNFoxMaiorQueTreze() {
        assertEquals(8192, mmeebbService.calculateIntervalDays(14));
        assertEquals(8192, mmeebbService.calculateIntervalDays(20));
        assertEquals(8192, mmeebbService.calculateIntervalDays(100));
    }

    @Test
    @DisplayName("Deve lançar IllegalArgumentException quando N for negativo")
    void deveLancarExcecaoQuandoNFoxNegativo() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> mmeebbService.calculateIntervalDays(-1)
        );
        assertTrue(exception.getMessage().contains("negativo"));
    }

    @Test
    @DisplayName("Deve calcular a próxima data somando o intervalo 2^N à data base")
    void deveCalcularProximaDataDeRevisaoCorretamente() {
        LocalDate baseDate = LocalDate.of(2026, 8, 1);
        LocalDate nextDate = mmeebbService.calculateNextReviewDate(baseDate, 3); // 2^3 = 8 dias

        assertEquals(LocalDate.of(2026, 8, 9), nextDate);
    }

    @Test
    @DisplayName("Deve lançar exceção se a data base for nula")
    void deveLancarExcecaoQuandoDataDeReferenciaNula() {
        assertThrows(IllegalArgumentException.class, () -> mmeebbService.calculateNextReviewDate(null, 0));
    }

    @Test
    @DisplayName("Deve inicializar novo agendamento com N=0 e primeira revisão em 1 dia")
    void deveInicializarNovoAgendamentoComNZeroEIntervaloDeUmDia() {
        LocalDate today = LocalDate.of(2026, 8, 31);
        RepetitionSchedule schedule = mmeebbService.initializeSchedule(student, flashcard, today);

        assertNotNull(schedule);
        assertEquals(student, schedule.getStudent());
        assertEquals(flashcard, schedule.getFlashcard());
        assertEquals(0, schedule.getNIndex());
        assertEquals(1, schedule.getIntervalDays());
        assertEquals(0, schedule.getRepetitionCount());
        assertEquals(0, schedule.getConsecutiveCorrect());
        assertEquals(ScheduleStatus.PENDING, schedule.getStatus());
        assertEquals(LocalDate.of(2026, 9, 1), schedule.getNextReviewDate());
    }

    @Test
    @DisplayName("Deve avançar ciclo (N: 0 -> 1) e dobrar intervalo para 2 dias em caso de acerto")
    void deveAvancarCicloEDobrarIntervaloQuandoRespostaCorreta() {
        LocalDate reviewDate = LocalDate.of(2026, 8, 31);
        LocalDateTime answeredAt = reviewDate.atTime(10, 0);

        RepetitionSchedule schedule = RepetitionSchedule.builder()
                .student(student)
                .flashcard(flashcard)
                .nIndex(0)
                .intervalDays(1)
                .repetitionCount(0)
                .consecutiveCorrect(0)
                .nextReviewDate(reviewDate)
                .status(ScheduleStatus.PENDING)
                .build();

        RepetitionSchedule updated = mmeebbService.processAnswer(schedule, true, answeredAt);

        assertEquals(1, updated.getNIndex());
        assertEquals(2, updated.getIntervalDays()); // 2^1 = 2 dias
        assertEquals(1, updated.getConsecutiveCorrect());
        assertEquals(1, updated.getRepetitionCount());
        assertEquals(answeredAt, updated.getLastReviewedAt());
        assertEquals(LocalDate.of(2026, 9, 2), updated.getNextReviewDate()); // 31/08 + 2 dias = 02/09
        assertEquals(ScheduleStatus.PENDING, updated.getStatus()); // Ciclo segue aberto para a próxima revisão
    }

    @Test
    @DisplayName("Deve progredir sucessivamente em múltiplos acertos consecutivos")
    void deveProgredirSucessivamenteEmMultiplosAcertos() {
        LocalDateTime date1 = LocalDateTime.of(2026, 8, 1, 9, 0);
        RepetitionSchedule schedule = RepetitionSchedule.builder()
                .student(student)
                .flashcard(flashcard)
                .nIndex(2) // Intervalo era 4
                .intervalDays(4)
                .repetitionCount(2)
                .consecutiveCorrect(2)
                .nextReviewDate(date1.toLocalDate())
                .status(ScheduleStatus.PENDING)
                .build();

        RepetitionSchedule result = mmeebbService.processAnswer(schedule, true, date1);

        assertEquals(3, result.getNIndex());
        assertEquals(8, result.getIntervalDays()); // 2^3 = 8 dias
        assertEquals(3, result.getConsecutiveCorrect());
        assertEquals(3, result.getRepetitionCount());
        assertEquals(LocalDate.of(2026, 8, 9), result.getNextReviewDate());
    }

    @Test
    @DisplayName("Deve manter no teto N=13 e intervalo de 8192 dias se continuar acertando após N=13")
    void deveSaturarNoTetoMaximoDeTrezeEmAcertosConsecutivos() {
        LocalDateTime answeredAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        RepetitionSchedule schedule = RepetitionSchedule.builder()
                .student(student)
                .flashcard(flashcard)
                .nIndex(13)
                .intervalDays(8192)
                .repetitionCount(13)
                .consecutiveCorrect(13)
                .nextReviewDate(answeredAt.toLocalDate())
                .status(ScheduleStatus.PENDING)
                .build();

        RepetitionSchedule result = mmeebbService.processAnswer(schedule, true, answeredAt);

        assertEquals(13, result.getNIndex());
        assertEquals(8192, result.getIntervalDays());
        assertEquals(14, result.getConsecutiveCorrect());
        assertEquals(14, result.getRepetitionCount());
    }

    @Test
    @DisplayName("Deve resetar N para 0 e intervalo para 1 dia em caso de erro / esquecimento")
    void deveResetarParaZeroEIntervaloDeUmDiaQuandoRespostaIncorreta() {
        LocalDateTime answeredAt = LocalDateTime.of(2026, 8, 15, 14, 30);
        RepetitionSchedule schedule = RepetitionSchedule.builder()
                .student(student)
                .flashcard(flashcard)
                .nIndex(5) // Estava em 32 dias
                .intervalDays(32)
                .repetitionCount(5)
                .consecutiveCorrect(5)
                .nextReviewDate(answeredAt.toLocalDate())
                .status(ScheduleStatus.PENDING)
                .build();

        RepetitionSchedule updated = mmeebbService.processAnswer(schedule, false, answeredAt);

        assertEquals(0, updated.getNIndex());
        assertEquals(1, updated.getIntervalDays()); // 2^0 = 1 dia
        assertEquals(0, updated.getConsecutiveCorrect()); // Reset de acertos
        assertEquals(6, updated.getRepetitionCount()); // Incrementa total de tentativas
        assertEquals(answeredAt, updated.getLastReviewedAt());
        assertEquals(LocalDate.of(2026, 8, 16), updated.getNextReviewDate()); // Volta para o dia seguinte
        assertEquals(ScheduleStatus.PENDING, updated.getStatus()); // Ciclo segue aberto para a próxima revisão
    }

    @Test
    @DisplayName("Deve lançar exceção se o schedule ou answeredAt forem nulos no processamento")
    void deveLancarExcecaoQuandoParametrosInvalidosNoProcessAnswer() {
        assertThrows(IllegalArgumentException.class, () -> mmeebbService.processAnswer(null, true, LocalDateTime.now()));
        assertThrows(IllegalArgumentException.class, () -> mmeebbService.processAnswer(new RepetitionSchedule(), true, null));
    }

    // =========================================================================
    // MMEEBB adaptativo: teto de N por domínio do tópico e lapso graduado
    // =========================================================================

    private static final LocalDateTime RESPONDIDO_EM = LocalDateTime.of(2026, 9, 17, 10, 0);

    private RepetitionSchedule scheduleComN(int nIndex) {
        return RepetitionSchedule.builder()
                .student(student)
                .flashcard(flashcard)
                .nIndex(nIndex)
                .intervalDays(1 << nIndex)
                .repetitionCount(nIndex)
                .consecutiveCorrect(nIndex)
                .nextReviewDate(RESPONDIDO_EM.toLocalDate())
                .status(ScheduleStatus.PENDING)
                .build();
    }

    @ParameterizedTest(name = "N = {0}")
    @CsvSource({"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13"})
    @DisplayName("Sem histórico do tópico, o adaptativo deve ser idêntico ao MMEEBB clássico")
    void semHistoricoDeveSerIdenticoAoMmeebbClassico(int nIndex) {
        for (boolean acertou : new boolean[]{true, false}) {
            RepetitionSchedule classico = mmeebbService.processAnswer(scheduleComN(nIndex), acertou, RESPONDIDO_EM);
            RepetitionSchedule adaptativo = mmeebbService.processAnswer(
                    scheduleComN(nIndex), acertou, RESPONDIDO_EM, TopicMastery.SEM_DADOS);

            assertEquals(classico.getNIndex(), adaptativo.getNIndex(),
                    "N divergente para N=" + nIndex + ", acerto=" + acertou);
            assertEquals(classico.getIntervalDays(), adaptativo.getIntervalDays());
            assertEquals(classico.getNextReviewDate(), adaptativo.getNextReviewDate());
            assertEquals(classico.getConsecutiveCorrect(), adaptativo.getConsecutiveCorrect());
        }
    }

    @Test
    @DisplayName("Tópico frágil deve segurar o intervalo em 8 dias mesmo quando o aluno acerta")
    void topicoFragilDeveSegurarOIntervaloEmOitoDias() {
        RepetitionSchedule resultado = mmeebbService.processAnswer(
                scheduleComN(5), true, RESPONDIDO_EM, TopicMastery.FRAGIL);

        assertEquals(3, resultado.getNIndex());
        assertEquals(8, resultado.getIntervalDays());
        assertEquals(LocalDate.of(2026, 9, 25), resultado.getNextReviewDate());
        assertEquals(6, resultado.getConsecutiveCorrect());
    }

    @Test
    @DisplayName("Tópico em consolidação deve segurar o intervalo em 64 dias")
    void topicoEmConsolidacaoDeveSegurarOIntervaloEmSessentaEQuatroDias() {
        RepetitionSchedule resultado = mmeebbService.processAnswer(
                scheduleComN(9), true, RESPONDIDO_EM, TopicMastery.EM_CONSOLIDACAO);

        assertEquals(6, resultado.getNIndex());
        assertEquals(64, resultado.getIntervalDays());
    }

    @Test
    @DisplayName("Abaixo do teto, o tópico frágil deve avançar normalmente (N+1)")
    void abaixoDoTetoOTopicoFragilDeveAvancarNormalmente() {
        RepetitionSchedule resultado = mmeebbService.processAnswer(
                scheduleComN(1), true, RESPONDIDO_EM, TopicMastery.FRAGIL);

        assertEquals(2, resultado.getNIndex());
        assertEquals(4, resultado.getIntervalDays());
    }

    @Test
    @DisplayName("Erro em tópico dominado deve recuar duas casas em vez de zerar")
    void erroEmTopicoDominadoDeveRecuarDuasCasas() {
        RepetitionSchedule resultado = mmeebbService.processAnswer(
                scheduleComN(6), false, RESPONDIDO_EM, TopicMastery.DOMINADO);

        assertEquals(4, resultado.getNIndex());
        assertEquals(16, resultado.getIntervalDays());
        assertEquals(LocalDate.of(2026, 10, 3), resultado.getNextReviewDate());
        assertEquals(0, resultado.getConsecutiveCorrect());
        assertEquals(7, resultado.getRepetitionCount());
    }

    @Test
    @DisplayName("O recuo do lapso graduado não pode ultrapassar N=0")
    void oRecuoDoLapsoGraduadoNaoPodeUltrapassarZero() {
        RepetitionSchedule resultado = mmeebbService.processAnswer(
                scheduleComN(1), false, RESPONDIDO_EM, TopicMastery.DOMINADO);

        assertEquals(0, resultado.getNIndex());
        assertEquals(1, resultado.getIntervalDays());
    }

    @ParameterizedTest(name = "domínio = {0}")
    @CsvSource({"FRAGIL", "EM_CONSOLIDACAO", "SEM_DADOS"})
    @DisplayName("Fora de tópico dominado, o erro deve continuar zerando o ciclo")
    void foraDeTopicoDominadoOErroDeveZerarOCiclo(TopicMastery mastery) {
        RepetitionSchedule resultado = mmeebbService.processAnswer(
                scheduleComN(5), false, RESPONDIDO_EM, mastery);

        assertEquals(0, resultado.getNIndex());
        assertEquals(1, resultado.getIntervalDays());
        assertEquals(0, resultado.getConsecutiveCorrect());
    }

    @Test
    @DisplayName("Tópico dominado deve seguir o MMEEBB clássico no acerto, até o teto de 13")
    void topicoDominadoDeveSeguirOClassicoNoAcerto() {
        assertEquals(6, mmeebbService.processAnswer(
                scheduleComN(5), true, RESPONDIDO_EM, TopicMastery.DOMINADO).getNIndex());
        assertEquals(13, mmeebbService.processAnswer(
                scheduleComN(13), true, RESPONDIDO_EM, TopicMastery.DOMINADO).getNIndex());
    }

    @Test
    @DisplayName("Domínio nulo deve ser tratado como SEM_DADOS para nunca derrubar uma revisão")
    void dominioNuloDeveSerTratadoComoSemDados() {
        RepetitionSchedule resultado = mmeebbService.processAnswer(
                scheduleComN(5), true, RESPONDIDO_EM, null);

        assertEquals(6, resultado.getNIndex());
        assertEquals(64, resultado.getIntervalDays());
    }
}
