package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.config.DailyLimitProperties;
import br.edu.unipam.tcc.dto.DailyReviewPlanDto;
import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.enums.ScheduleStatus;
import br.edu.unipam.tcc.repository.RepetitionScheduleRepository;
import br.edu.unipam.tcc.repository.ReviewAttemptRepository;
import br.edu.unipam.tcc.service.impl.DailyReviewQueueServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DailyReviewQueueServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    // 02:30 UTC do dia 18 = 23:30 do dia 17 em São Paulo: o "hoje" do limite é o dia 17.
    private static final Instant NOW = Instant.parse("2026-09-18T02:30:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 17);

    @Mock private RepetitionScheduleRepository repetitionScheduleRepository;
    @Mock private ReviewAttemptRepository reviewAttemptRepository;
    @Mock private AdaptiveReviewService adaptiveReviewService;

    private Student student;
    private long nextId;

    @BeforeEach
    void setUp() {
        student = Student.builder().id(UUID.randomUUID()).phoneNumber("5534999998888").fullName("Maria Silva").build();
        nextId = 1;
        // Ordem adaptativa neutra: a fila chega como veio do banco, salvo quando o teste reordena.
        when(adaptiveReviewService.prioritize(any(), anyList())).thenAnswer(invocation -> invocation.getArgument(1));
    }

    private DailyReviewQueueServiceImpl serviceAt(Instant instant) {
        return new DailyReviewQueueServiceImpl(repetitionScheduleRepository, reviewAttemptRepository,
                adaptiveReviewService, new DailyLimitProperties(20, 5, 100, 0.25), Clock.fixed(instant, ZONE));
    }

    /** Sem reserva de novas: as revisões vencidas podem ocupar o limite inteiro. */
    private DailyReviewQueueServiceImpl serviceWithoutNewReservation() {
        return new DailyReviewQueueServiceImpl(repetitionScheduleRepository, reviewAttemptRepository,
                adaptiveReviewService, new DailyLimitProperties(20, 5, 100, 0), Clock.fixed(NOW, ZONE));
    }

    private DailyReviewQueueServiceImpl service() {
        return serviceAt(NOW);
    }

    private RepetitionSchedule review() {
        RepetitionSchedule schedule = schedule();
        schedule.setLastReviewedAt(LocalDateTime.of(2026, 9, 10, 8, 0));
        return schedule;
    }

    private RepetitionSchedule newQuestion() {
        return schedule();
    }

    private RepetitionSchedule schedule() {
        Flashcard card = Flashcard.builder().topic("Arritmias").question("Q").answer("A").active(true).build();
        card.setId(nextId);
        RepetitionSchedule schedule = RepetitionSchedule.builder()
                .student(student).flashcard(card).status(ScheduleStatus.PENDING).nextReviewDate(TODAY).build();
        schedule.setId(nextId++);
        return schedule;
    }

    private List<RepetitionSchedule> many(int count, boolean reviewed) {
        List<RepetitionSchedule> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            list.add(reviewed ? review() : newQuestion());
        }
        return list;
    }

    private void stubDue(List<RepetitionSchedule> due) {
        when(repetitionScheduleRepository.findPendingReviewsByStudent(student.getId(), TODAY, ScheduleStatus.PENDING))
                .thenReturn(due);
    }

    private void stubAnsweredToday(long answered) {
        stubAnsweredToday(answered, 0);
    }

    /** @param newAnswered quantas das respondidas hoje eram a primeira tentativa da questão */
    private void stubAnsweredToday(long answered, long newAnswered) {
        when(reviewAttemptRepository.countAnsweredBetween(
                student.getId(), TODAY.atStartOfDay(), TODAY.plusDays(1).atStartOfDay()))
                .thenReturn(answered);
        when(reviewAttemptRepository.countFirstAnswersBetween(
                student.getId(), TODAY.atStartOfDay(), TODAY.plusDays(1).atStartOfDay()))
                .thenReturn(newAnswered);
    }

    private static <T> List<T> concat(List<T> first, List<T> second) {
        List<T> all = new ArrayList<>(first);
        all.addAll(second);
        return all;
    }

    // ============================================================ fila do dia

    @Test
    @DisplayName("Sem limite próprio, deve cortar a fila no padrão de 20 e deixar o resto para os próximos dias")
    void deveUsarLimitePadrao() {
        stubDue(many(381, false));

        DailyReviewPlanDto plan = service().planToday(student);

        assertEquals(20, plan.dailyLimit());
        assertEquals(20, plan.queue().size());
        assertEquals(0, plan.reviews());
        assertEquals(20, plan.newQuestions());
        assertEquals(361, plan.beyondLimit());
        assertEquals(381, plan.dueTotal());
    }

    @Test
    @DisplayName("Limite escolhido pelo estudante deve substituir o padrão")
    void deveUsarLimitePersonalizado() {
        student.setDailyQuestionLimit(7);
        stubDue(many(30, false));

        DailyReviewPlanDto plan = service().planToday(student);

        assertEquals(7, plan.dailyLimit());
        assertEquals(7, plan.queue().size());
        assertEquals(23, plan.beyondLimit());
    }

    @Test
    @DisplayName("Revisões vencidas devem vir antes das novas, mesmo quando o banco as traz misturadas")
    void revisoesDevemVirAntesDasNovas() {
        RepetitionSchedule nova1 = newQuestion();
        RepetitionSchedule revisao1 = review();
        RepetitionSchedule nova2 = newQuestion();
        RepetitionSchedule revisao2 = review();
        stubDue(List.of(nova1, revisao1, nova2, revisao2));

        DailyReviewPlanDto plan = service().planToday(student);

        assertEquals(List.of(revisao1, revisao2, nova1, nova2), plan.queue());
        assertEquals(2, plan.reviews());
        assertEquals(2, plan.newQuestions());
    }

    @Test
    @DisplayName("Novas só devem completar o que sobrar do limite depois das revisões")
    void novasSoCompletamOQueSobra() {
        List<RepetitionSchedule> reviews = many(12, true);
        List<RepetitionSchedule> news = many(50, false);
        stubDue(concat(news, reviews));

        DailyReviewPlanDto plan = service().planToday(student);

        assertEquals(12, plan.reviews());
        assertEquals(8, plan.newQuestions());
        assertEquals(concat(reviews, news.subList(0, 8)), plan.queue());
        assertEquals("12 revisões + 8 novas", plan.breakdown());
        assertEquals(42, plan.beyondLimit());
    }

    // ============================================================ reserva de questões novas

    @Test
    @DisplayName("Com mais revisões vencidas que o limite, um quarto do dia ainda deve ficar para novas")
    void reservaGaranteNovasMesmoComRevisoesSobrando() {
        List<RepetitionSchedule> reviews = many(25, true);
        List<RepetitionSchedule> news = many(10, false);
        stubDue(concat(news, reviews));

        DailyReviewPlanDto plan = service().planToday(student);

        assertEquals(15, plan.reviews());
        assertEquals(5, plan.newQuestions());
        assertEquals(concat(reviews.subList(0, 15), news.subList(0, 5)), plan.queue());
        assertEquals(15, plan.beyondLimit());
    }

    @Test
    @DisplayName("Sem reserva configurada, as revisões vencidas devem poder ocupar o limite inteiro")
    void semReservaRevisoesOcupamOLimiteInteiro() {
        stubDue(concat(many(25, true), many(10, false)));

        DailyReviewPlanDto plan = serviceWithoutNewReservation().planToday(student);

        assertEquals(20, plan.reviews());
        assertEquals(0, plan.newQuestions());
        assertTrue(plan.queue().stream().allMatch(schedule -> schedule.getLastReviewedAt() != null));
    }

    @Test
    @DisplayName("Vaga reservada sem questão nova disponível deve voltar para as revisões")
    void vagaReservadaSemNovaVoltaParaRevisoes() {
        stubDue(concat(many(25, true), many(2, false)));

        DailyReviewPlanDto plan = service().planToday(student);

        assertEquals(18, plan.reviews());
        assertEquals(2, plan.newQuestions());
    }

    @Test
    @DisplayName("A reserva não deve se desfazer ao longo do dia: depois de 15 revisões, as 5 novas continuam garantidas")
    void reservaNaoSeDesfazAoLongoDoDia() {
        stubAnsweredToday(15, 0);
        stubDue(concat(many(10, true), many(40, false)));

        DailyReviewPlanDto plan = service().planToday(student);

        assertEquals(0, plan.reviews());
        assertEquals(5, plan.newQuestions());
    }

    @Test
    @DisplayName("Novas já respondidas hoje devem consumir a reserva, liberando o resto para revisões")
    void novasRespondidasConsomemAReserva() {
        stubAnsweredToday(10, 5);
        stubDue(concat(many(30, true), many(30, false)));

        DailyReviewPlanDto plan = service().planToday(student);

        assertEquals(10, plan.reviews());
        assertEquals(0, plan.newQuestions());
    }

    @Test
    @DisplayName("Limite pequeno deve garantir ao menos uma questão nova por dia")
    void limitePequenoGaranteUmaNova() {
        student.setDailyQuestionLimit(5);
        stubDue(concat(many(30, true), many(30, false)));

        DailyReviewPlanDto plan = service().planToday(student);

        assertEquals(4, plan.reviews());
        assertEquals(1, plan.newQuestions());
    }

    @Test
    @DisplayName("Deve manter, dentro de cada grupo, a ordem decidida pela camada adaptativa")
    void deveManterOrdemAdaptativaDentroDeCadaGrupo() {
        RepetitionSchedule revisaoA = review();
        RepetitionSchedule revisaoB = review();
        RepetitionSchedule novaA = newQuestion();
        RepetitionSchedule novaB = newQuestion();
        stubDue(List.of(revisaoA, novaA, revisaoB, novaB));
        // A camada adaptativa põe o tópico frágil (B) na frente.
        when(adaptiveReviewService.prioritize(eq(student.getId()), anyList()))
                .thenReturn(List.of(novaB, revisaoB, novaA, revisaoA));

        DailyReviewPlanDto plan = service().planToday(student);

        assertEquals(List.of(revisaoB, revisaoA, novaB, novaA), plan.queue());
    }

    @Test
    @DisplayName("Questões respondidas hoje devem descontar do limite")
    void respondidasHojeDescontamDoLimite() {
        // 15 respondidas, 10 delas novas: a reserva do dia já foi cumprida.
        stubAnsweredToday(15, 10);
        stubDue(concat(many(3, true), many(40, false)));

        DailyReviewPlanDto plan = service().planToday(student);

        assertEquals(15, plan.answeredToday());
        assertEquals(3, plan.reviews());
        assertEquals(2, plan.newQuestions());
        assertEquals(38, plan.beyondLimit());
    }

    @Test
    @DisplayName("Limite atingido deve encerrar a meta do dia: fila vazia, com o excedente informado")
    void limiteAtingidoEncerraAMetaDoDia() {
        stubAnsweredToday(20);
        stubDue(many(361, false));

        DailyReviewPlanDto plan = service().planToday(student);

        assertTrue(plan.queue().isEmpty());
        assertTrue(plan.goalReached());
        assertEquals(361, plan.beyondLimit());
    }

    @Test
    @DisplayName("Limite reduzido abaixo do já respondido não deve gerar restante negativo")
    void limiteMenorQueORespondidoNaoFicaNegativo() {
        student.setDailyQuestionLimit(5);
        stubAnsweredToday(12);
        stubDue(many(10, true));

        DailyReviewPlanDto plan = service().planToday(student);

        assertTrue(plan.queue().isEmpty());
        assertEquals(0, plan.total());
        assertEquals(10, plan.beyondLimit());
    }

    @Test
    @DisplayName("Em dia com o banco, a fila vazia não deve ser tratada como meta concluída")
    void semPendenciasNaoEhMetaConcluida() {
        stubAnsweredToday(3);
        stubDue(List.of());

        DailyReviewPlanDto plan = service().planToday(student);

        assertTrue(plan.queue().isEmpty());
        assertFalse(plan.goalReached());
    }

    @Test
    @DisplayName("Deve contar as respondidas do dia de São Paulo, da meia-noite à meia-noite")
    void deveContarRespondidasNoDiaDeSaoPaulo() {
        stubDue(List.of());

        service().planToday(student);

        verify(reviewAttemptRepository).countAnsweredBetween(student.getId(),
                LocalDateTime.of(2026, 9, 17, 0, 0), LocalDateTime.of(2026, 9, 18, 0, 0));
        verify(reviewAttemptRepository).countFirstAnswersBetween(student.getId(),
                LocalDateTime.of(2026, 9, 17, 0, 0), LocalDateTime.of(2026, 9, 18, 0, 0));
        verify(repetitionScheduleRepository).findPendingReviewsByStudent(student.getId(), TODAY, ScheduleStatus.PENDING);
    }

    @Test
    @DisplayName("A virada do dia deve zerar a contagem: o dia seguinte consulta um novo intervalo")
    void viradaDoDiaZeraAContagem() {
        LocalDate tomorrow = TODAY.plusDays(1);
        stubAnsweredToday(20);
        when(reviewAttemptRepository.countAnsweredBetween(
                student.getId(), tomorrow.atStartOfDay(), tomorrow.plusDays(1).atStartOfDay()))
                .thenReturn(0L);
        stubDue(many(30, false));
        when(repetitionScheduleRepository.findPendingReviewsByStudent(student.getId(), tomorrow, ScheduleStatus.PENDING))
                .thenReturn(many(30, false));

        DailyReviewPlanDto hoje = service().planToday(student);
        // 00:05 do dia 18 em São Paulo.
        DailyReviewPlanDto amanha = serviceAt(Instant.parse("2026-09-18T03:05:00Z")).planToday(student);

        assertTrue(hoje.goalReached());
        assertEquals(20, amanha.queue().size());
        assertEquals(0, amanha.answeredToday());
    }

    // ============================================================ resumo do lembrete

    @Test
    @DisplayName("Resumo do lembrete deve aplicar o mesmo corte sem carregar nem priorizar a fila")
    void resumoDeveAplicarOMesmoCorteSemCarregarAFila() {
        stubAnsweredToday(2);
        when(repetitionScheduleRepository.countDueReviewsByStudent(student.getId(), TODAY, ScheduleStatus.PENDING))
                .thenReturn(12L);
        when(repetitionScheduleRepository.countDueNewQuestionsByStudent(student.getId(), TODAY, ScheduleStatus.PENDING))
                .thenReturn(369L);

        DailyReviewPlanDto plan = service().summarizeToday(student);

        assertEquals(12, plan.reviews());
        assertEquals(6, plan.newQuestions());
        assertEquals(18, plan.total());
        assertEquals(363, plan.beyondLimit());
        assertTrue(plan.queue().isEmpty());
        verify(repetitionScheduleRepository, never()).findPendingReviewsByStudent(any(), any(), any());
        verify(adaptiveReviewService, never()).prioritize(any(), anyList());
    }

    @Test
    @DisplayName("Resumo e fila completa devem chegar às mesmas contagens")
    void resumoEFilaDevemBater() {
        List<RepetitionSchedule> reviews = many(4, true);
        List<RepetitionSchedule> news = many(30, false);
        stubAnsweredToday(1);
        stubDue(concat(news, reviews));
        when(repetitionScheduleRepository.countDueReviewsByStudent(student.getId(), TODAY, ScheduleStatus.PENDING))
                .thenReturn(4L);
        when(repetitionScheduleRepository.countDueNewQuestionsByStudent(student.getId(), TODAY, ScheduleStatus.PENDING))
                .thenReturn(30L);

        DailyReviewPlanDto plan = service().planToday(student);
        DailyReviewPlanDto summary = service().summarizeToday(student);

        assertEquals(plan.reviews(), summary.reviews());
        assertEquals(plan.newQuestions(), summary.newQuestions());
        assertEquals(plan.beyondLimit(), summary.beyondLimit());
    }
}
