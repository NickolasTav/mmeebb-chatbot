package br.edu.unipam.tcc.service;

import br.edu.unipam.tcc.config.AdaptiveProperties;
import br.edu.unipam.tcc.config.TimeConfig;
import br.edu.unipam.tcc.dto.AdaptiveReviewOutcomeDto;
import br.edu.unipam.tcc.dto.TopicPerformanceDto;
import br.edu.unipam.tcc.entity.Course;
import br.edu.unipam.tcc.entity.Flashcard;
import br.edu.unipam.tcc.entity.RepetitionSchedule;
import br.edu.unipam.tcc.entity.ReviewAttempt;
import br.edu.unipam.tcc.entity.Student;
import br.edu.unipam.tcc.entity.Subject;
import br.edu.unipam.tcc.entity.enums.TopicMastery;
import br.edu.unipam.tcc.observability.MmeebbMetrics;
import br.edu.unipam.tcc.repository.FlashcardRepository;
import br.edu.unipam.tcc.repository.RepetitionScheduleRepository;
import br.edu.unipam.tcc.repository.ReviewAttemptRepository;
import br.edu.unipam.tcc.service.impl.AdaptiveReviewServiceImpl;
import br.edu.unipam.tcc.service.impl.MmeebbServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdaptiveReviewServiceImplTest {

    private static final LocalDateTime RESPONDIDO_EM = LocalDateTime.of(2026, 9, 17, 10, 0);
    private static final LocalDate HOJE = RESPONDIDO_EM.toLocalDate();
    private static final Long FARMACO = 4L;
    private static final String TOPICO = "Antibioticoterapia";

    private RepetitionScheduleRepository repetitionScheduleRepository;
    private ReviewAttemptRepository reviewAttemptRepository;
    private FlashcardRepository flashcardRepository;
    private PerformanceAnalysisService performanceAnalysisService;
    private MmeebbMetrics mmeebbMetrics;
    private AdaptiveReviewService service;

    private Student student;
    private Flashcard flashcard;

    @BeforeEach
    void setUp() {
        repetitionScheduleRepository = mock(RepetitionScheduleRepository.class);
        reviewAttemptRepository = mock(ReviewAttemptRepository.class);
        flashcardRepository = mock(FlashcardRepository.class);
        performanceAnalysisService = mock(PerformanceAnalysisService.class);
        mmeebbMetrics = mock(MmeebbMetrics.class);

        Clock clock = Clock.fixed(RESPONDIDO_EM.atZone(TimeConfig.APP_ZONE).toInstant(), TimeConfig.APP_ZONE);
        service = new AdaptiveReviewServiceImpl(
                repetitionScheduleRepository,
                reviewAttemptRepository,
                flashcardRepository,
                performanceAnalysisService,
                new MmeebbServiceImpl(mmeebbMetrics),
                new AdaptiveProperties(true, 90, 3, 0.50, 0.20, 2, 5),
                mmeebbMetrics,
                clock);

        Course medicina = Course.builder().code("MED").name("Medicina").build();
        medicina.setId(1L);
        Subject farmacologia = Subject.builder().course(medicina).code("FARMACO").name("Farmacologia Clinica").build();
        farmacologia.setId(FARMACO);

        student = Student.builder().phoneNumber("5534900001111").fullName("Maria Silva").build();
        student.setId(UUID.randomUUID());

        flashcard = Flashcard.builder().subject(farmacologia).topic(TOPICO)
                .question("Qual o espectro da amoxicilina?").answer("Gram-positivos").build();
        flashcard.setId(100L);

        when(flashcardRepository.findSubjectIdById(flashcard.getId())).thenReturn(Optional.of(FARMACO));
        when(repetitionScheduleRepository.save(any(RepetitionSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void givenMastery(TopicMastery mastery, double errorRate) {
        when(performanceAnalysisService.analyzeTopic(student.getId(), FARMACO, TOPICO))
                .thenReturn(new TopicPerformanceDto(FARMACO, "Farmacologia Clinica", TOPICO,
                        10L, Math.round(errorRate * 10), errorRate, mastery));
    }

    private void givenSchedule(int nIndex) {
        RepetitionSchedule schedule = RepetitionSchedule.builder()
                .student(student).flashcard(flashcard)
                .nIndex(nIndex).intervalDays(1 << nIndex)
                .nextReviewDate(HOJE).build();
        schedule.setId(500L);
        when(repetitionScheduleRepository.findByStudentIdAndFlashcardId(student.getId(), flashcard.getId()))
                .thenReturn(Optional.of(schedule));
    }

    private ReviewAttempt tentativaGravada() {
        ArgumentCaptor<ReviewAttempt> captor = ArgumentCaptor.forClass(ReviewAttempt.class);
        verify(reviewAttemptRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Smoke Test: deve salvar o agendamento e registrar a tentativa com o N antes e depois")
    void deveSalvarAgendamentoERegistrarTentativa() {
        givenMastery(TopicMastery.DOMINADO, 0.1);
        givenSchedule(2);

        AdaptiveReviewOutcomeDto resultado = service.processAnswer(student, flashcard, true, RESPONDIDO_EM);

        assertThat(resultado.schedule().getNIndex()).isEqualTo(3);
        assertThat(resultado.schedule().getIntervalDays()).isEqualTo(8);
        verify(repetitionScheduleRepository).save(resultado.schedule());

        ReviewAttempt tentativa = tentativaGravada();
        assertThat(tentativa.getNIndexBefore()).isEqualTo(2);
        assertThat(tentativa.getNIndexAfter()).isEqualTo(3);
        assertThat(tentativa.getIntervalDaysAfter()).isEqualTo(8);
        assertThat(tentativa.getCorrect()).isTrue();
        assertThat(tentativa.getTopicMastery()).isEqualTo(TopicMastery.DOMINADO);
        assertThat(tentativa.getAnsweredAt()).isEqualTo(RESPONDIDO_EM);
    }

    @Test
    @DisplayName("Deve criar o agendamento na hora quando o cartão ainda não tiver um")
    void deveCriarAgendamentoQuandoNaoExistir() {
        givenMastery(TopicMastery.SEM_DADOS, 0.0);
        when(repetitionScheduleRepository.findByStudentIdAndFlashcardId(student.getId(), flashcard.getId()))
                .thenReturn(Optional.empty());

        AdaptiveReviewOutcomeDto resultado = service.processAnswer(student, flashcard, true, RESPONDIDO_EM);

        assertThat(resultado.schedule().getNIndex()).isEqualTo(1);
        assertThat(tentativaGravada().getNIndexBefore()).isZero();
    }

    @Test
    @DisplayName("Deve sinalizar que o teto do tópico frágil segurou o intervalo")
    void deveSinalizarQueOTetoSegurouOIntervalo() {
        givenMastery(TopicMastery.FRAGIL, 0.7);
        givenSchedule(5);

        AdaptiveReviewOutcomeDto resultado = service.processAnswer(student, flashcard, true, RESPONDIDO_EM);

        assertThat(resultado.intervalCapped()).isTrue();
        assertThat(resultado.lapseSoftened()).isFalse();
        assertThat(resultado.schedule().getIntervalDays()).isEqualTo(8);
        verify(mmeebbMetrics).recordAdaptiveDecision("FRAGIL", "interval_capped");
    }

    @Test
    @DisplayName("A saturação clássica em N=13 não deve ser confundida com teto adaptativo")
    void saturacaoClassicaNaoEhTetoAdaptativo() {
        givenMastery(TopicMastery.DOMINADO, 0.05);
        givenSchedule(13);

        AdaptiveReviewOutcomeDto resultado = service.processAnswer(student, flashcard, true, RESPONDIDO_EM);

        assertThat(resultado.intervalCapped()).isFalse();
        assertThat(resultado.schedule().getNIndex()).isEqualTo(13);
        verify(mmeebbMetrics).recordAdaptiveDecision("DOMINADO", "classic");
    }

    @Test
    @DisplayName("Deve sinalizar o lapso amortecido no erro em tópico dominado")
    void deveSinalizarLapsoAmortecido() {
        givenMastery(TopicMastery.DOMINADO, 0.08);
        givenSchedule(6);

        AdaptiveReviewOutcomeDto resultado = service.processAnswer(student, flashcard, false, RESPONDIDO_EM);

        assertThat(resultado.lapseSoftened()).isTrue();
        assertThat(resultado.schedule().getNIndex()).isEqualTo(4);
        assertThat(resultado.schedule().getIntervalDays()).isEqualTo(16);
        verify(mmeebbMetrics).recordAdaptiveDecision("DOMINADO", "lapse_softened");
        verify(repetitionScheduleRepository, never()).findFutureSchedulesByTopic(any(), any(), anyString(), any());
    }

    @Test
    @DisplayName("Erro em tópico frágil deve antecipar cartões relacionados do mesmo tópico")
    void erroEmTopicoFragilDeveAnteciparCartoesRelacionados() {
        givenMastery(TopicMastery.FRAGIL, 0.8);
        givenSchedule(2);
        when(repetitionScheduleRepository.countDueSchedulesByTopic(student.getId(), FARMACO, TOPICO, HOJE))
                .thenReturn(0L);
        when(repetitionScheduleRepository.findFutureSchedulesByTopic(student.getId(), FARMACO, TOPICO, HOJE))
                .thenReturn(List.of(futuro(601L, HOJE.plusDays(4)), futuro(602L, HOJE.plusDays(9)),
                        futuro(603L, HOJE.plusDays(30))));

        AdaptiveReviewOutcomeDto resultado = service.processAnswer(student, flashcard, false, RESPONDIDO_EM);

        assertThat(resultado.reinforcedCards()).isEqualTo(2);

        ArgumentCaptor<List<RepetitionSchedule>> captor = ArgumentCaptor.forClass(List.class);
        verify(repetitionScheduleRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2)
                .allSatisfy(schedule -> assertThat(schedule.getNextReviewDate()).isEqualTo(HOJE))
                .extracting(RepetitionSchedule::getId).containsExactly(601L, 602L);
        verify(mmeebbMetrics).recordAdaptiveReinforcement(2);
    }

    @Test
    @DisplayName("Não deve antecipar reforço quando o tópico já tem cartões vencidos suficientes")
    void naoDeveAnteciparQuandoJaHaCartoesVencidos() {
        givenMastery(TopicMastery.FRAGIL, 0.8);
        givenSchedule(2);
        when(repetitionScheduleRepository.countDueSchedulesByTopic(student.getId(), FARMACO, TOPICO, HOJE))
                .thenReturn(2L);

        AdaptiveReviewOutcomeDto resultado = service.processAnswer(student, flashcard, false, RESPONDIDO_EM);

        assertThat(resultado.reinforcedCards()).isZero();
        verify(repetitionScheduleRepository, never()).findFutureSchedulesByTopic(any(), any(), anyString(), any());
        verify(repetitionScheduleRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Acerto em tópico frágil não deve disparar reforço dirigido")
    void acertoEmTopicoFragilNaoDeveDispararReforco() {
        givenMastery(TopicMastery.FRAGIL, 0.8);
        givenSchedule(2);

        AdaptiveReviewOutcomeDto resultado = service.processAnswer(student, flashcard, true, RESPONDIDO_EM);

        assertThat(resultado.reinforcedCards()).isZero();
        verify(repetitionScheduleRepository, never()).countDueSchedulesByTopic(any(), anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("Erro em tópico sem histórico não deve disparar reforço dirigido")
    void erroSemHistoricoNaoDeveDispararReforco() {
        givenMastery(TopicMastery.SEM_DADOS, 0.0);
        givenSchedule(2);

        AdaptiveReviewOutcomeDto resultado = service.processAnswer(student, flashcard, false, RESPONDIDO_EM);

        assertThat(resultado.reinforcedCards()).isZero();
        verify(repetitionScheduleRepository, never()).countDueSchedulesByTopic(any(), anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("Deve apresentar primeiro os cartões dos tópicos mais frágeis")
    void devePriorizarOsTopicosMaisFrageis() {
        when(performanceAnalysisService.analyzeTopics(student.getId())).thenReturn(List.of(
                new TopicPerformanceDto(FARMACO, "Farmacologia Clinica", "Fragil", 10L, 8L, 0.8, TopicMastery.FRAGIL),
                new TopicPerformanceDto(FARMACO, "Farmacologia Clinica", "Consolidando", 10L, 3L, 0.3, TopicMastery.EM_CONSOLIDACAO),
                new TopicPerformanceDto(FARMACO, "Farmacologia Clinica", "Dominado", 10L, 0L, 0.0, TopicMastery.DOMINADO)
        ));

        List<RepetitionSchedule> fila = List.of(
                agendamento(1L, "Dominado", HOJE.minusDays(5)),
                agendamento(2L, "Novo", HOJE),
                agendamento(3L, "Consolidando", HOJE),
                agendamento(4L, "Fragil", HOJE)
        );

        List<RepetitionSchedule> priorizada = service.prioritize(student.getId(), fila);

        assertThat(priorizada).extracting(s -> s.getFlashcard().getTopic())
                .containsExactly("Fragil", "Consolidando", "Novo", "Dominado");
    }

    @Test
    @DisplayName("Com o mesmo domínio, deve manter a revisão mais atrasada primeiro")
    void comMesmoDominioDeveManterAMaisAtrasadaPrimeiro() {
        when(performanceAnalysisService.analyzeTopics(student.getId())).thenReturn(List.of());

        List<RepetitionSchedule> fila = List.of(
                agendamento(1L, "Tema A", HOJE),
                agendamento(2L, "Tema B", HOJE.minusDays(7))
        );

        List<RepetitionSchedule> priorizada = service.prioritize(student.getId(), fila);

        assertThat(priorizada).extracting(RepetitionSchedule::getId).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("Deve devolver a fila intacta quando houver menos de dois cartões")
    void deveDevolverFilaIntactaComMenosDeDoisCartoes() {
        List<RepetitionSchedule> unico = List.of(agendamento(1L, "Tema A", HOJE));

        assertThat(service.prioritize(student.getId(), unico)).isEqualTo(unico);
        assertThat(service.prioritize(student.getId(), List.of())).isEmpty();
        verify(performanceAnalysisService, never()).analyzeTopics(any());
    }

    private RepetitionSchedule futuro(Long id, LocalDate nextReviewDate) {
        RepetitionSchedule schedule = RepetitionSchedule.builder()
                .student(student).flashcard(flashcard).nIndex(2).intervalDays(4)
                .nextReviewDate(nextReviewDate).build();
        schedule.setId(id);
        return schedule;
    }

    private RepetitionSchedule agendamento(Long id, String topic, LocalDate nextReviewDate) {
        Subject farmacologia = Subject.builder().code("FARMACO").name("Farmacologia Clinica").build();
        farmacologia.setId(FARMACO);
        Flashcard card = Flashcard.builder().subject(farmacologia).topic(topic)
                .question("Pergunta").answer("Resposta").build();
        card.setId(id + 1000);

        RepetitionSchedule schedule = RepetitionSchedule.builder()
                .student(student).flashcard(card).nIndex(1).intervalDays(2)
                .nextReviewDate(nextReviewDate).build();
        schedule.setId(id);
        return schedule;
    }
}
