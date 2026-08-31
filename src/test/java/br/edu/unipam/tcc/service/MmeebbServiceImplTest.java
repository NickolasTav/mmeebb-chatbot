package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.enums.ScheduleStatus;
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
        mmeebbService = new MmeebbServiceImpl();
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
        assertEquals(ScheduleStatus.COMPLETED, updated.getStatus());
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
        assertEquals(ScheduleStatus.COMPLETED, updated.getStatus());
    }

    @Test
    @DisplayName("Deve lançar exceção se o schedule ou answeredAt forem nulos no processamento")
    void deveLancarExcecaoQuandoParametrosInvalidosNoProcessAnswer() {
        assertThrows(IllegalArgumentException.class, () -> mmeebbService.processAnswer(null, true, LocalDateTime.now()));
        assertThrows(IllegalArgumentException.class, () -> mmeebbService.processAnswer(new RepetitionSchedule(), true, null));
    }
}
